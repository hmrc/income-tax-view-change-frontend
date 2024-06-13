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

package utils

import auth.MtdItUser
import cats.data.EitherT
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.IncomeSourceJourney.{AfterSubmissionPage, CannotGoBackPage, InitialPage, JourneyState}
import models.claimToAdjustPoa.PoAAmendmentData
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.PaymentOnAccountSessionService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait JourneyCheckerClaimToAdjust extends ClaimToAdjustUtils {
  self =>

  val poaSessionService: PaymentOnAccountSessionService
  val itvcErrorHandler: ItvcErrorHandler
  val itvcErrorHandlerAgent: AgentItvcErrorHandler

  implicit val ec: ExecutionContext

  def errorHandler(implicit user: MtdItUser[_]) = if (isAgent(user)) itvcErrorHandlerAgent else itvcErrorHandler

  def withSessionData(journeyState: JourneyState)(codeBlock: Option[PoAAmendmentData] => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    ifAdjustPoaIsEnabled(isAgent(user)) {
      poaSessionService.getMongo.flatMap {
        case Right(Some(sessionData)) if !showCannotGoBackErrorPage(sessionData.journeyCompleted, journeyState) =>
          codeBlock(Some(sessionData))
        case Right(Some(_)) =>
          redirectToYouCannotGoBackPage(user)
        case Right(None) =>
          if (journeyState == InitialPage) {
            codeBlock(None)
          } else {
            Future.successful(errorHandler.showInternalServerError())
          }
        case Left(ex: Exception) =>
          Logger("application").error( if (isAgent(user)) "[Agent]" else "" +
            s"There was an error while retrieving the mongo data")
          Future.successful(errorHandler.showInternalServerError())
      }

    }
  }

  private lazy val isAgent: MtdItUser[_] => Boolean = (user: MtdItUser[_]) => user.userType.contains(Agent)

  def redirectToYouCannotGoBackPage(user: MtdItUser[_]): Future[Result] = {
    // TODO: Update with new URL
    Future.successful(Redirect(controllers.routes.HomeController.showAgent.url))
  }

  private def showCannotGoBackErrorPage(journeyCompleted: Boolean, journeyState: JourneyState): Boolean = {
    val isOnCannotGoBackOrSuccessPage = journeyState == CannotGoBackPage || journeyState == AfterSubmissionPage
    (journeyCompleted, isOnCannotGoBackOrSuccessPage) match {
      case (_, true) => false
      case (true, _) => true
      case _ => false
    }
  }

}
