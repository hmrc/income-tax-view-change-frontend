/*
 * Copyright 2025 HM Revenue & Customs
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

package obligations.utils.reportingObligations

import common.auth.MtdItUser
import common.models.incomeSourceDetails.TaxYear
import obligations.controllers.reportingObligations.routes as reportingObligationsRoutes
import obligations.services.reportingObligations.signUp.SignUpService
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import shared.enums.{JourneyCompleted, JourneyState}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyCheckerSignUp extends ReportingObligationsUtils {
  self =>

  val signUpService: SignUpService

  def withSessionData(isStart: Boolean, taxYear: TaxYear)
                     (codeBlock: => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    if(isStart) {
      signUpService.initialiseOptInContextData().flatMap {
        case true =>
          signUpService.saveIntent(taxYear).flatMap {
            case true => codeBlock
            case false =>
              Logger("application").error(s"[JourneyCheckerSignUp][withSessionData] - Could not save sign up tax year to session for intent year: ${taxYear.toString} and user with sessionId: ${hc.sessionId.getOrElse("NO SESSION ID")} from referrer: ${hc.otherHeaders.find(h => h._1 == "Referer").getOrElse(("Referer", "No Referer"))._2}")
              Future(Redirect(reportingObligationsRoutes.ReportingFrequencyPageController.show(user.isAgent)))
          }
        case false =>
          Logger("application").error(s"[JourneyCheckerSignUp][withSessionData] - Could not initialise opt-in context data in session for intent year: ${taxYear.toString} and user with sessionId: ${hc.sessionId.getOrElse("NO SESSION ID")} from referrer: ${hc.otherHeaders.find(h => h._1 == "Referer").getOrElse(("Referer", "No Referer"))._2}")
          Future(Redirect(reportingObligationsRoutes.ReportingFrequencyPageController.show(user.isAgent)))
      }
    } else {
      signUpService.fetchSavedChosenTaxYear().flatMap {
        case Some(savedTaxYear) if savedTaxYear == taxYear => codeBlock
        case _ => redirectReportingFrequency(user.userType)
      }
    }
  }

  def retrieveIsJourneyComplete(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    signUpService.fetchSavedSignUpSessionData().map {
      case Some(data) => data.journeyIsComplete.contains(true)
      case _ => false
    }
  }

  def setJourneyComplete(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    signUpService.updateJourneyStatusInSessionData(journeyComplete = true)
  }
}
