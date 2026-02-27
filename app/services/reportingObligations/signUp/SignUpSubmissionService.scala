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

package services.reportingObligations.signUp

import audit.AuditingService
import audit.models.SignUpAuditModel
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponse, ITSAStatusUpdateResponseFailure}
import enums.JourneyType.{Opt, SignUpJourney}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.reportingObligations.signUp.{SignUpContextData, SignUpSessionData}
import play.api.Logging
import repositories.UIJourneySessionDataRepository
import services.DateServiceInterface
import services.reportingObligations.signUp.core.SignUpProposition
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignUpSubmissionService @Inject()(
                                         auditingService: AuditingService,
                                         dateService: DateServiceInterface,
                                         itsaStatusUpdateConnector: ITSAStatusUpdateConnector,
                                         optInService: SignUpService,
                                         uiJourneySessionDataRepository: UIJourneySessionDataRepository
                                       ) extends Logging {

  private[services] def getOptInSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[SignUpSessionData]] = {
    for {
      uiSessionData <- uiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney))
      optInSessionData: Option[SignUpSessionData] = uiSessionData.flatMap(_.signUpSessionData)
    } yield {
      optInSessionData
    }
  }

  private[services] def getAllSignUpTaxYears(
                                                    currentYearItsaStatus: ITSAStatus,
                                                    nextYearItsaStatus: ITSAStatus,
                                                    intentTaxYear: Option[TaxYear]
                                                  ): Seq[TaxYear] = {
    val currentTaxYear = dateService.getCurrentTaxYear

    (intentTaxYear, currentYearItsaStatus, nextYearItsaStatus) match {
      case (Some(year), Annual, Annual) if year == currentTaxYear     => Seq(currentTaxYear, currentTaxYear.nextYear)
      case (Some(year), Annual, _) if year == currentTaxYear          => Seq(currentTaxYear)
      case (Some(year), _, Annual) if year == currentTaxYear.nextYear => Seq(currentTaxYear.nextYear)
      case _ => Seq.empty
    }
  }

  private[services] def sendSignUpRequest(signUpTaxYears: Seq[TaxYear], nino: String)
                                        (implicit hc: HeaderCarrier): Future[ITSAStatusUpdateResponse] = {
    signUpTaxYears.headOption match {
      case Some(taxYear) =>
        itsaStatusUpdateConnector.signUp(taxYear, nino)
      case None =>
        logger.error(s"[SignUpSubmissionService][sendSignUpRequest] No tax years to sign up for.")
        Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure())
    }
  }

  def triggerSignUpRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[ITSAStatusUpdateResponse] = {
    for {
      optInSessionData: Option[SignUpSessionData] <- getOptInSessionData()
      selectedSignUpYear: Option[TaxYear] = optInSessionData.flatMap(_.selectedSignUpYear).flatMap(TaxYear.`fromStringYYYY-YYYY`)
      optInContextData: Option[SignUpContextData] = optInSessionData.flatMap(_.signUpContextData)
      currentTaxYear: TaxYear = optInContextData.map(data => data.currentTaxYear).flatMap(TaxYear.`fromStringYYYY-YYYY`).getOrElse(dateService.getCurrentTaxYear)
      optInProposition: SignUpProposition <- optInService.fetchSignUpProposition()
      currentYearItsaStatus = optInProposition.currentTaxYear.status
      nextYearItsaStatus = optInProposition.nextTaxYear.status
      _ = logger.info(
        s"\n[SignUpSubmissionService][triggerSignUpRequest] currentTaxYear: $currentTaxYear \n" +
          s"[SignUpSubmissionService][triggerSignUpRequest] selectedSignUpYear: $selectedSignUpYear \n" +
          s"[SignUpSubmissionService][triggerSignUpRequest] optInProposition: $optInProposition"
      )
      taxYearsToSignUp = getAllSignUpTaxYears(currentYearItsaStatus, nextYearItsaStatus, selectedSignUpYear)
      _ = logger.info(s"\n[SignUpSubmissionService][triggerSignUpRequest] taxYearsToSignUp: $taxYearsToSignUp")
      updateResponse <- sendSignUpRequest(taxYearsToSignUp, user.nino)
      _ = logger.info(s"\n[SignUpSubmissionService][triggerSignUpRequest] Sign Up update response: $updateResponse")
      _ <- auditingService.extendedAudit(SignUpAuditModel(taxYearsToSignUp.map(_.toString)))
    } yield {
      updateResponse
    }
  }
}