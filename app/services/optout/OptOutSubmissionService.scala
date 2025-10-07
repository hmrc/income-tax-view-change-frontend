/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.optout

import audit.AuditingService
import audit.models.OptOutAuditModel
import audit.models.OptOutAuditModel.createOutcome
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel._
import enums.JourneyType.{Opt, OptOutJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, UnknownStatus}
import models.optout.{OptOutSessionData, OptOutYearToUpdate}
import play.api.Logging
import repositories.{OptOutContextData, UIJourneySessionDataRepository}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutSubmissionService @Inject()(
                                         auditingService: AuditingService,
                                         itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                                         uiJourneySessionDataRepo: UIJourneySessionDataRepository
                                       ) extends Logging {


  def getOptOutSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptOutSessionData]] = {
    for {
      uiSessionData: Option[UIJourneySessionData] <- uiJourneySessionDataRepo.get(hc.sessionId.get.value, Opt(OptOutJourney))
      optOutSessionData: Option[OptOutSessionData] = uiSessionData.flatMap(_.optOutSessionData)
    } yield optOutSessionData
  }

  def getOptOutYearsToUpdateWithStatuses(maybeOptOutContextData: Option[OptOutContextData]): List[OptOutYearToUpdate] = {
    maybeOptOutContextData match {
      case Some(contextData) =>

        val currentYearTaxYear: Option[TaxYear] = TaxYear.`fromStringYYYY-YYYY`(contextData.currentYear)
        val allTaxYears: List[TaxYear] = List(currentYearTaxYear.map(_.previousYear), currentYearTaxYear, currentYearTaxYear.map(_.nextYear)).flatten
        val taxYearItsaStatuses: List[ITSAStatus] =
          List(contextData.previousYearITSAStatus, contextData.currentYearITSAStatus, contextData.nextYearITSAStatus).map(ITSAStatus.fromString)
        val optOutYearsToUpdate: List[OptOutYearToUpdate] =
          allTaxYears
            .zip(taxYearItsaStatuses)
            .map { case (taxYear, itsaStatus) =>
              OptOutYearToUpdate(taxYear, itsaStatus)
            }

        logger.debug(
          s"[ConfirmOptOutUpdateService][getOptOutYearsToUpdateWithStatuses] All TaxYears $allTaxYears\n" +
            s"[ConfirmOptOutUpdateService][getOptOutYearsToUpdateWithStatuses] All TaxYears Statuses $taxYearItsaStatuses\n" +
            s"[ConfirmOptOutUpdateService][getOptOutYearsToUpdateWithStatuses] Voluntary OptOutYearsToUpdate: $optOutYearsToUpdate"
        )
        optOutYearsToUpdate
      case None =>
        List()
    }
  }

  def correctYearsToUpdateBasedOnUserSelection(maybeSessionData: Option[OptOutSessionData], allYearsToUpdate: List[OptOutYearToUpdate]): List[OptOutYearToUpdate] = {

    val maybeOptOutContextData: Option[OptOutContextData] = maybeSessionData.flatMap(_.optOutContextData)
    val maybeCurrentTaxYear: Option[TaxYear] = maybeOptOutContextData.map(_.currentYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
    val selectedOptOutTaxYear: Option[TaxYear] = maybeSessionData.flatMap(sessionData => sessionData.selectedOptOutYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
    val crystallisationStatus = maybeOptOutContextData.exists(_.crystallisationStatus)

    val gatherTaxYearsToUpdateBasedOnUserAnswerChoice =
      (maybeCurrentTaxYear, selectedOptOutTaxYear) match {
        case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear.previousYear && crystallisationStatus =>
          allYearsToUpdate.filterNot(_.taxYear == currentTaxYear.previousYear)
        case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear.previousYear && !crystallisationStatus =>
          allYearsToUpdate
        case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear =>
          allYearsToUpdate.filter(optOutYearsToUpdate => optOutYearsToUpdate.taxYear != currentTaxYear.previousYear)
        case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear.nextYear =>
          allYearsToUpdate.filter(optOutYearsToUpdate => optOutYearsToUpdate.taxYear == currentTaxYear.nextYear)
        case _ =>
          allYearsToUpdate
      }

    logger.debug(s"[ConfirmOptOutUpdateService][correctYearsToUpdateBasedOnUserSelection] User account itsa data: $maybeOptOutContextData")
    logger.debug(s"[ConfirmOptOutUpdateService][correctYearsToUpdateBasedOnUserSelection] All years to updated based on user tax year selection: $gatherTaxYearsToUpdateBasedOnUserAnswerChoice, useranswers chosen tax year: $selectedOptOutTaxYear")
    gatherTaxYearsToUpdateBasedOnUserAnswerChoice
  }

  def createAuditEvent(
                        mayBeSelectedTaxYear: Option[String],
                        mayBeOptOutContextData: Option[OptOutContextData],
                        filteredTaxYearsForDesiredItsaStatus: List[OptOutYearToUpdate],
                        updateRequestsForEachYearResponse: ITSAStatusUpdateResponse
                      )(implicit user: MtdItUser[_]): OptOutAuditModel = {

    val currentTaxYear: Option[TaxYear] = mayBeOptOutContextData.flatMap(data => TaxYear.`fromStringYYYY-YYYY`(data.currentYear))

    val updatedPreviousTaxYearItsaStatus: ITSAStatus =
      if (currentTaxYear.flatMap(taxYear => filteredTaxYearsForDesiredItsaStatus.find(_.taxYear == taxYear.previousYear)).isDefined) Annual
      else mayBeOptOutContextData.fold(UnknownStatus)(data => ITSAStatus.fromString(data.previousYearITSAStatus))

    val updatedCurrentTaxYearItsaStatus: ITSAStatus =
      if (currentTaxYear.flatMap(taxYear => filteredTaxYearsForDesiredItsaStatus.find(_.taxYear == taxYear)).isDefined) Annual
      else mayBeOptOutContextData.fold(UnknownStatus)(data => ITSAStatus.fromString(data.currentYearITSAStatus))

    val updatedNextTaxYearItsaStatus: ITSAStatus =
      if (currentTaxYear.flatMap(taxYear => filteredTaxYearsForDesiredItsaStatus.find(_.taxYear == taxYear.nextYear)).isDefined) Annual
      else mayBeOptOutContextData.fold(UnknownStatus)(data => ITSAStatus.fromString(data.nextYearITSAStatus))

    OptOutAuditModel(
      saUtr = user.saUtr,
      credId = user.credId,
      userType = user.userType,
      agentReferenceNumber = user.arn,
      mtditid = user.mtditid,
      nino = user.nino,
      optOutRequestedFromTaxYear = mayBeSelectedTaxYear.getOrElse("No tax year chosen"),
      currentYear = currentTaxYear.map(_.formatAsShortYearRange).getOrElse("Unable to get current tax year"),
      `beforeITSAStatusCurrentYear-1` = mayBeOptOutContextData.fold(UnknownStatus)(data => ITSAStatus.fromString(data.previousYearITSAStatus)),
      beforeITSAStatusCurrentYear = mayBeOptOutContextData.fold(UnknownStatus)(data => ITSAStatus.fromString(data.currentYearITSAStatus)),
      `beforeITSAStatusCurrentYear+1` = mayBeOptOutContextData.fold(UnknownStatus)(data => ITSAStatus.fromString(data.nextYearITSAStatus)),
      outcome = createOutcome(updateRequestsForEachYearResponse),
      `afterAssumedITSAStatusCurrentYear-1` = updatedPreviousTaxYearItsaStatus,
      afterAssumedITSAStatusCurrentYear = updatedCurrentTaxYearItsaStatus,
      `afterAssumedITSAStatusCurrentYear+1` = updatedNextTaxYearItsaStatus,
      `currentYear-1Crystallised` = mayBeOptOutContextData.fold(false)(_.crystallisationStatus)
    )
  }


  // This possibly makes multiple api calls to update each valid "MTD Voluntary" tax year
  def updateTaxYearsITSAStatusRequest(itsaStatusToSendUpdatesFor: ITSAStatus)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[ITSAStatusUpdateResponse]] = {
    for {
      maybeSessionData: Option[OptOutSessionData] <- getOptOutSessionData()
      maybeSelectedTaxYear: Option[String] = maybeSessionData.flatMap(_.selectedOptOutYear)
      maybeOptOutContextData: Option[OptOutContextData] = maybeSessionData.flatMap(_.optOutContextData)
      allItsaStatusYears: List[OptOutYearToUpdate] = getOptOutYearsToUpdateWithStatuses(maybeOptOutContextData)
      yearsToUpdateBasedOnUserSelection: List[OptOutYearToUpdate] = correctYearsToUpdateBasedOnUserSelection(maybeSessionData, allItsaStatusYears)
      filteredTaxYearsForDesiredItsaStatus: List[OptOutYearToUpdate] = yearsToUpdateBasedOnUserSelection.filter { yearsToUpdate => yearsToUpdate.itsaStatus == itsaStatusToSendUpdatesFor }
      taxYearsToUpdate = filteredTaxYearsForDesiredItsaStatus.map(_.taxYear)
      _ = logger.debug(s"[ITSAStatusUpdateConnector][updateTaxYearsITSAStatusRequest] Making update requests for tax years: $taxYearsToUpdate")
      makeUpdateRequestsForEachYear: List[ITSAStatusUpdateResponse] <- Future.sequence(taxYearsToUpdate.map(taxYear => itsaStatusUpdateConnector.makeITSAStatusUpdate(taxYear, user.nino, optOutUpdateReason)))
      auditEvents: List[OptOutAuditModel] = makeUpdateRequestsForEachYear.map(response => createAuditEvent(maybeSelectedTaxYear, maybeOptOutContextData, filteredTaxYearsForDesiredItsaStatus, response))
      _ <- Future.sequence(auditEvents.map((event: OptOutAuditModel) => auditingService.extendedAudit(event)))
    } yield {
      makeUpdateRequestsForEachYear
    }
  }

}