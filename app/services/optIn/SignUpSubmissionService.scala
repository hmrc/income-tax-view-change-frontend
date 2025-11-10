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
import audit.models.{SignUpAuditModel, SignUpMultipleYears, SignUpSingleYear}
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import enums.JourneyType.{Opt, OptInJourney}
import models.incomeSourceDetails.TaxYear
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
                         selectedSignUpYear: Option[TaxYear]
                       )(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[ITSAStatusUpdateResponse] = {
    selectedSignUpYear match {
      case Some(year) => itsaStatusUpdateConnector.optIn(taxYear = year, taxableEntityId = user.nino)
      case _ => Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())
    }
  }

  private[services] def makeSignUpAuditEventRequest(
                                                     selectedSignUpYear: Option[TaxYear],
                                                     currentYearItsaStatus: ITSAStatus,
                                                     nextYearItsaStatus: ITSAStatus
                                                   )(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val currentTaxYear = dateService.getCurrentTaxYear
    val nextTaxYear = currentTaxYear.nextYear

    (selectedSignUpYear, currentYearItsaStatus, nextYearItsaStatus) match {
      case (Some(selectedTaxYear), Annual, nyStatus) if selectedTaxYear == currentTaxYear =>
        val signUpYearType = if (nyStatus == Annual) SignUpMultipleYears else SignUpSingleYear
        auditingService.extendedAudit(SignUpAuditModel(selectedTaxYear, signUpYearType, currentYearItsaStatus, nextYearItsaStatus))
      case (Some(selectedTaxYear), _, Annual) if selectedTaxYear == nextTaxYear =>
        auditingService.extendedAudit(SignUpAuditModel(selectedTaxYear, SignUpMultipleYears, currentYearItsaStatus, nextYearItsaStatus))
      case _ => Future(())
    }
  }

  def triggerSignUpRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    for {
      optInSessionData: Option[OptInSessionData] <- getOptInSessionData()
      selectedSignUpYear: Option[TaxYear] = optInSessionData.flatMap(_.selectedOptInYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
      optInContextData: Option[OptInContextData] = optInSessionData.flatMap(_.optInContextData)
      currentTaxYear: TaxYear = optInContextData.map(data => data.currentTaxYear).flatMap(TaxYear.`fromStringYYYY-YYYY`).getOrElse(dateService.getCurrentTaxYear)
      optInProposition: OptInProposition <- optInService.fetchOptInProposition()
      currentYearItsaStatus = optInProposition.currentTaxYear.status
      nextYearItsaStatus = optInProposition.nextTaxYear.status
      _ = logger.info(
        s"\n[SignUpSubmissionService][triggerSignUpRequest] currentTaxYear: $currentTaxYear \n" +
          s"[SignUpSubmissionService][triggerSignUpRequest] selectedSignUpYear: $selectedSignUpYear \n" +
          s"[SignUpSubmissionService][triggerSignUpRequest] optInProposition: $optInProposition"
      )
      updateResponse <- makeUpdateRequest(selectedSignUpYear)
      _ = logger.info(
        s"\n[SignUpSubmissionService][triggerSignUpRequest] Sign Up update response: $updateResponse"
      )
      _ <- makeSignUpAuditEventRequest(selectedSignUpYear, currentYearItsaStatus, nextYearItsaStatus)
    } yield {
      updateResponse
    }
  }
}