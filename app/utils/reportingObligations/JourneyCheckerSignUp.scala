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

package utils.reportingObligations

import auth.MtdItUser
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.{JourneyCompleted, JourneyState}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.mvc.{Request, Result}
import services.optIn.OptInService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyCheckerSignUp extends ReportingObligationsUtils {
  self =>

  val itvcErrorHandler: ItvcErrorHandler
  val itvcErrorHandlerAgent: AgentItvcErrorHandler

  val optInService: OptInService

  def withSessionData(isStart: Boolean, taxYear: TaxYear, journeyState: Option[JourneyState])
                     (codeBlock: => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    (isStart, journeyState) match {
      case (true, None) =>
        optInService.saveIntent(taxYear).flatMap {
          case true => codeBlock
          case false =>
            Logger("application").error(s"[JourneyCheckerSignUp][withSessionData] - Could not save sign up tax year to session")
            showInternalServerError(user.isAgent())
        }
      case (false, None) =>
        optInService.fetchSavedChosenTaxYear().flatMap {
          case Some(savedTaxYear) if savedTaxYear == taxYear => codeBlock
          case _ => redirectReportingFrequency(user.userType)
        }

      case (false, Some(JourneyCompleted)) => codeBlock
    }
  }

  def retrieveIsJourneyComplete(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    optInService.fetchSavedOptInSessionData().map {
      case Some(data) => data.journeyIsComplete.contains(true)
      case _ => false
    }
  }

  def setJourneyComplete(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Boolean] = {
    optInService.updateJourneyStatusInSessionData(journeyComplete = true)
  }

  private def showInternalServerError(isAgent: Boolean)(implicit request: Request[_]): Future[Result] = {
    val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    Future.successful(errorHandler(isAgent).showInternalServerError())
  }
}
