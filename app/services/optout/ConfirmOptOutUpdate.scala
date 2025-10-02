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

import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, optOutUpdateReason}
import enums.JourneyType.{Opt, OptOutJourney}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.optout.{OptOutSessionData, OptOutYearsToUpdate}
import play.api.Logging
import repositories.{OptOutContextData, UIJourneySessionDataRepository}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutUpdateService @Inject()(
                                            itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                                            uiJourneySessionDataRepo: UIJourneySessionDataRepository
                                          )(implicit hc: HeaderCarrier, ec: ExecutionContext) extends Logging {

  def getOptOutSessionData(): Future[Option[OptOutSessionData]] = {
    for {
      uiSessionData: Option[UIJourneySessionData] <- uiJourneySessionDataRepo.get(hc.sessionId.get.value, Opt(OptOutJourney))
      optOutSessionData: Option[OptOutSessionData] = uiSessionData.flatMap(_.optOutSessionData)
    } yield optOutSessionData
  }

  def getOptOutYearsToUpdateWithStatuses(): Future[List[OptOutYearsToUpdate]] = {
    for {
      getOptOutSessionData: Option[OptOutSessionData] <- getOptOutSessionData()
      maybeOptOutContextData: Option[OptOutContextData] = getOptOutSessionData.flatMap(_.optOutContextData)
    } yield {
      maybeOptOutContextData match {
        case Some(contextData) =>

          val currentYearTaxYear: Option[TaxYear] = TaxYear.`fromStringYYYY-YYYY`(contextData.currentYear)
          val allTaxYears: List[TaxYear] = List(currentYearTaxYear.map(_.previousYear), currentYearTaxYear, currentYearTaxYear.map(_.nextYear)).flatten
          val taxYearItsaStatuses: List[ITSAStatus] =
            List(contextData.previousYearITSAStatus, contextData.currentYearITSAStatus, contextData.nextYearITSAStatus).map(ITSAStatus.fromString)

          val optOutYearsToUpdate: List[OptOutYearsToUpdate] = {
            for {
              taxYear: TaxYear <- allTaxYears
              itsaStatuses: ITSAStatus <- taxYearItsaStatuses
            } yield {
              OptOutYearsToUpdate(taxYear, itsaStatuses)
            }
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
  }

  def correctYearsToUpdateBasedOnUserSelection(): Future[List[OptOutYearsToUpdate]] = {
    for {
      maybeSessionData: Option[OptOutSessionData] <- getOptOutSessionData()
      allYearsToUpdate: List[OptOutYearsToUpdate] <- getOptOutYearsToUpdateWithStatuses()
      maybeOptOutContextData: Option[OptOutContextData] = maybeSessionData.flatMap(_.optOutContextData)
      maybeCurrentTaxYear: Option[TaxYear] = maybeOptOutContextData.map(_.currentYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
      selectedOptOutTaxYear: Option[TaxYear] = maybeSessionData.flatMap(sessionData => sessionData.selectedOptOutYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
      gatherTaxYearsToUpdateBasedOnUserAnswerChoice =
        (maybeCurrentTaxYear, selectedOptOutTaxYear) match {
          case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear.previousYear =>
            allYearsToUpdate
          case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear =>
            allYearsToUpdate.filter(optOutYearsToUpdate => optOutYearsToUpdate.taxYear != currentTaxYear.previousYear)
          case (Some(currentTaxYear), Some(selectedTaxYear)) if selectedTaxYear == currentTaxYear.nextYear =>
            allYearsToUpdate.filter(optOutYearsToUpdate => optOutYearsToUpdate.taxYear == currentTaxYear.nextYear)
          case _ =>
            List()
        }
    } yield {
      gatherTaxYearsToUpdateBasedOnUserAnswerChoice
    }
  }


  def updateTaxYearsITSAStatusRequest(itsaStatus: ITSAStatus)(implicit user: MtdItUser[_]): Future[List[ITSAStatusUpdateResponse]] = {
    for {
      correctYearsToUpdateBasedOnUserSelection: List[OptOutYearsToUpdate] <- correctYearsToUpdateBasedOnUserSelection()
      returnOnlyYearWithDesiredItsaStatus = correctYearsToUpdateBasedOnUserSelection.filter { yearsToUpdate => yearsToUpdate.itsaStatus == itsaStatus }
      makeRequests: List[ITSAStatusUpdateResponse] <- Future.sequence(returnOnlyYearWithDesiredItsaStatus.map(optOutYearsToUpdate => itsaStatusUpdateConnector.makeITSAStatusUpdate(optOutYearsToUpdate.taxYear, user.nino, optOutUpdateReason)))
    } yield {
      makeRequests
    }
  }

}