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

package services.optIn

import audit.AuditingService
import audit.models.OptInAuditModel
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import enums.JourneyType.{Opt, OptInJourney}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus._
import models.optin.{OptInContextData, OptInSessionData}
import play.api.Logging
import repositories.UIJourneySessionDataRepository
import services.DateServiceInterface
import services.optIn.core.OptInProposition
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignUpSubmissionService @Inject()(
                                         auditingService: AuditingService,
                                         dateService: DateServiceInterface,
                                         itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                                         optInService: OptInService,
                                         uiJourneySessionDataRepository: UIJourneySessionDataRepository
                                       ) extends Logging {

  def getOptInSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptInSessionData]] = {
    for {
      uiSessionData <- uiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney))
      optInSessionData: Option[OptInSessionData] = uiSessionData.flatMap(_.optInSessionData)
    } yield {
      optInSessionData
    }
  }

  def makeUpdateRequest(
                         selectedSignUpYear: Option[TaxYear],
                         currentYearItsaStatus: Option[ITSAStatus],
                         nextYearItsaStatus: Option[ITSAStatus]
                       )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {

    val currentTaxYear = dateService.getCurrentTaxYear
    val nextTaxYear = currentTaxYear.nextYear

    (selectedSignUpYear, currentYearItsaStatus, nextYearItsaStatus) match {
      case (Some(selectedTaxYear), Some(Annual), Some(Annual)) =>
        itsaStatusUpdateConnector.optIn(taxYear = selectedTaxYear, user.nino)
      case (None, Some(Annual), Some(nextYearStatus)) if nextYearStatus != Annual =>
        itsaStatusUpdateConnector.optIn(taxYear = currentTaxYear, user.nino)
      case (None, Some(currentYearStatus), Some(Annual)) if currentYearStatus != Annual =>
        itsaStatusUpdateConnector.optIn(taxYear = nextTaxYear, user.nino)
      case (Some(selectedTaxYear), None, None) =>
        itsaStatusUpdateConnector.optIn(taxYear = selectedTaxYear, user.nino)
      case _ =>
        Future(ITSAStatusUpdateResponseFailure.defaultFailure()) // we return a failure if it does not satisfy scenarios
    }
  }

  def makeSignUpAuditEventRequest(
                                   selectedSignUpYear: Option[TaxYear],
                                   currentYearItsaStatus: Option[ITSAStatus],
                                   nextYearItsaStatus: Option[ITSAStatus],
                                   optInProposition: OptInProposition,
                                   itsaStatusUpdateResponse: ITSAStatusUpdateResponse
                                 )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {

    val currentTaxYear = dateService.getCurrentTaxYear
    val nextTaxYear = currentTaxYear.nextYear

    (selectedSignUpYear, currentYearItsaStatus, nextYearItsaStatus) match {
      case (Some(selectedTaxYear), Some(Annual), Some(Annual)) =>
        val optInAuditModel = OptInAuditModel(optInProposition, selectedTaxYear, itsaStatusUpdateResponse)
        auditingService.extendedAudit(optInAuditModel)
      case (None, Some(Annual), Some(nextYearStatus)) if nextYearStatus != Annual =>
        val optInAuditModel = OptInAuditModel(optInProposition, currentTaxYear, itsaStatusUpdateResponse)
        auditingService.extendedAudit(optInAuditModel)
      case (None, Some(currentYearStatus), Some(Annual)) if currentYearStatus != Annual =>
        val optInAuditModel = OptInAuditModel(optInProposition, nextTaxYear, itsaStatusUpdateResponse)
        auditingService.extendedAudit(optInAuditModel)
      case _ =>
        Future(()) // we don't send an audit if it does not satisfy scenarios
    }
  }

  def triggerSignUpRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    for {
      optInSessionData: Option[OptInSessionData] <- getOptInSessionData()
      selectedSignUpYear: Option[TaxYear] = optInSessionData.flatMap(_.selectedOptInYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
      optInContextData: Option[OptInContextData] = optInSessionData.flatMap(_.optInContextData)
      currentTaxYear: TaxYear = optInContextData.map(data => data.currentTaxYear).flatMap(TaxYear.`fromStringYYYY-YYYY`).getOrElse(dateService.getCurrentTaxYear)
      currentYearItsaStatus: Option[ITSAStatus] = optInContextData.map(data => ITSAStatus.fromString(data.currentYearITSAStatus))
      nextYearItsaStatus: Option[ITSAStatus] = optInContextData.map(data => ITSAStatus.fromString(data.nextYearITSAStatus))
      optInProposition: OptInProposition <- optInService.fetchOptInProposition()
      _ = logger.info(
        s"\n[SignUpSubmissionService][triggerSignUpRequest] currentTaxYear: $currentTaxYear \n" +
          s"[SignUpSubmissionService][triggerSignUpRequest] selectedSignUpYear: $selectedSignUpYear \n" +
          s"[SignUpSubmissionService][triggerSignUpRequest] optInProposition: $optInProposition"
      )
      updateResponse <- makeUpdateRequest(selectedSignUpYear, currentYearItsaStatus, nextYearItsaStatus)
      _ = logger.info(
        s"\n[SignUpSubmissionService][triggerSignUpRequest] Sign Up update response: $updateResponse"
      )
      _ <- makeSignUpAuditEventRequest(selectedSignUpYear, currentYearItsaStatus, nextYearItsaStatus, optInProposition, updateResponse)
    } yield {
      updateResponse
    }
  }


}