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

package utils.ClaimToAdjust

import auth.MtdItUser
import config.{AgentItvcErrorHandler, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney._
import models.claimToAdjustPoa.PoAAmendmentData
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler

import scala.concurrent.{ExecutionContext, Future}

trait JourneyCheckerClaimToAdjust extends ClaimToAdjustUtils {
  self =>

  val poaSessionService: PaymentOnAccountSessionService
  val itvcErrorHandler: ItvcErrorHandler
  val itvcErrorHandlerAgent: AgentItvcErrorHandler
  implicit val ec: ExecutionContext

  def withSessionData(journeyState: JourneyState = BeforeSubmissionPage)(codeBlock: PoAAmendmentData => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    ifAdjustPoaIsEnabled(isAgent(user)) {
      if (journeyState == InitialPage) {
        handleSession(codeBlock)
      } else {
        poaSessionService.getMongo.flatMap {
          case Right(Some(sessionData)) if !showCannotGoBackErrorPage(sessionData.journeyCompleted, journeyState) =>
            codeBlock(sessionData)
          case Right(Some(_)) =>
            Future.successful(redirectToYouCannotGoBackPage(user))
          case Right(None) =>
            Logger("application").error(if (isAgent(user)) "[Agent]" else "" + s"Necessary session data was empty in mongo")
            Future.successful(errorHandler.showInternalServerError())
          case Left(ex: Throwable) =>
            Logger("application").error(if (isAgent(user)) "[Agent]" else "" +
              s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >")
            Future.successful(errorHandler.showInternalServerError())
        }
      }
    }
  }

  def errorHandler(implicit user: MtdItUser[_]): FrontendErrorHandler with ShowInternalServerError =
    if (isAgent(user)) itvcErrorHandlerAgent else itvcErrorHandler

  lazy val isAgent: MtdItUser[_] => Boolean = (user: MtdItUser[_]) => user.userType.contains(Agent)

  def redirectToYouCannotGoBackPage(user: MtdItUser[_]): Result = {
    // TODO: Update with new URL
    Redirect(controllers.routes.HomeController.show().url)
  }

  def showCannotGoBackErrorPage(journeyCompleted: Boolean, journeyState: JourneyState): Boolean = {
    val isOnCannotGoBackOrSuccessPage = journeyState == CannotGoBackPage || journeyState == AfterSubmissionPage
    (journeyCompleted, isOnCannotGoBackOrSuccessPage) match {
      case (_, true) => false
      case (true, _) => true
      case _ => false
    }
  }

  private def handleSession(codeBlock: PoAAmendmentData => Future[Result])(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Result] = {
    poaSessionService.getMongo flatMap {
      case Right(Some(poaData: PoAAmendmentData)) => {
        if (poaData.journeyCompleted) {
          Logger("application").info(s"The current active mongo Claim to Adjust POA session has been completed by the user, so a new session will be created")
          poaSessionService.createSession.flatMap {
            case Right(_) => codeBlock(poaData)
            case Left(ex: Throwable) =>
              Logger("application").error(if (isAgent(user)) "[Agent]" else "" +
                s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >")
              Future.successful(errorHandler.showInternalServerError())
          }
        } else {
          Logger("application").info(s"The current active mongo Claim to Adjust POA session has not been completed by the user")
          codeBlock(poaData)
        }
      }
      case Right(None) =>
        Logger("application").info(s"There is no active mongo Claim to Adjust POA session, so a new one will be created")
        poaSessionService.createSession
        // TODO: look at this again
        codeBlock(PoAAmendmentData())
      case Left(ex) =>
        Logger("application").error(if (isAgent(user)) "[Agent]" else "" +
          s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >")
        Future.successful(errorHandler.showInternalServerError())
    }
  }

}
