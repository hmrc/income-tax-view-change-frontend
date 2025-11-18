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
import audit.models.SignUpAuditModel
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

  private[services] def getOptInSessionData()(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[OptInSessionData]] = {
    for {
      uiSessionData <- uiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney))
      optInSessionData: Option[OptInSessionData] = uiSessionData.flatMap(_.optInSessionData)
    } yield {
      optInSessionData
    }
  }


  private[services] def filterTaxYearsForSignUp(
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

  private def resolveUpdateItsaStatuses(
                                         taxYears: Seq[TaxYear],
                                         nino: String
                                       )(implicit headerCarrier: HeaderCarrier, executionContext: ExecutionContext): Future[Seq[ITSAStatusUpdateResponse]] = {
    taxYears.foldLeft(Future.successful(Seq.empty[ITSAStatusUpdateResponse])) { (accumulatorFuture, year) =>
      accumulatorFuture.flatMap { acc =>
        if (acc.exists(_.isInstanceOf[ITSAStatusUpdateResponseFailure])) {
          logger.warn(s"Failed to (sign up) update ITSA status for tax year ${year.toString}")
          Future.successful(acc)
        } else {
          itsaStatusUpdateConnector.optIn(taxYear = year, taxableEntityId = nino).map { response =>
            logger.info(s"Successfully (signed up) updated ITSA status for tax year ${year.toString}")
            acc :+ response
          }
        }
      }
    }
  }

  def triggerSignUpRequest()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Seq[ITSAStatusUpdateResponse]] = {
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
      taxYearsToSignUp = filterTaxYearsForSignUp(currentYearItsaStatus, nextYearItsaStatus, selectedSignUpYear)
      _ = logger.info(s"\n[SignUpSubmissionService][triggerSignUpRequest] taxYearsToSignUp: $taxYearsToSignUp")
      updateResponse <- resolveUpdateItsaStatuses(taxYearsToSignUp, nino = user.nino)
      _ = logger.info(s"\n[SignUpSubmissionService][triggerSignUpRequest] Sign Up update response: $updateResponse")
      _ <- auditingService.extendedAudit(SignUpAuditModel(taxYearsToSignUp.map(_.toString)))
    } yield {
      updateResponse
    }
  }
}