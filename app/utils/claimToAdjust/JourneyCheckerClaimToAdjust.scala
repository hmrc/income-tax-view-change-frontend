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

package utils.claimToAdjust

import auth.MtdItUser
import enums.IncomeSourceJourney._
import enums.{AfterSubmissionPage, BeforeSubmissionPage, CannotGoBackPage, InitialPage, JourneyState}
import models.claimToAdjustPoa.PoaAmendmentData
import play.api.Logger
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.PaymentOnAccountSessionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorRecovery

import scala.concurrent.{ExecutionContext, Future}

trait JourneyCheckerClaimToAdjust extends ErrorRecovery {
  self =>

  val poaSessionService: PaymentOnAccountSessionService
  implicit val ec: ExecutionContext

  def withSessionData(journeyState: JourneyState = BeforeSubmissionPage)(codeBlock: PoaAmendmentData => Future[Result])
                     (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
      if (journeyState == InitialPage) {
        handleSession(codeBlock)
      } else {
        poaSessionService.getMongo.flatMap {
          case Right(Some(sessionData)) if !showCannotGoBackErrorPage(sessionData.journeyCompleted, journeyState) =>
            codeBlock(sessionData)
          case Right(Some(_)) =>
            Future.successful(redirectToYouCannotGoBackPage(user))
          case Right(None) =>
            Future.successful(logAndRedirect(s"Necessary session data was empty in mongo"))
          case Left(ex: Throwable) =>
            Future.successful(
              logAndRedirect(s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >"))
        }
      }
  }

  def redirectToYouCannotGoBackPage(user: MtdItUser[_]): Result = {
    Redirect(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(user.isAgent()).url)
  }

  def showCannotGoBackErrorPage(journeyCompleted: Boolean, journeyState: JourneyState): Boolean = {
    val isOnCannotGoBackOrSuccessPage = journeyState == CannotGoBackPage || journeyState == AfterSubmissionPage
    (journeyCompleted, isOnCannotGoBackOrSuccessPage) match {
      case (_, true) => false
      case (true, _) => true
      case _ => false
    }
  }

  private def handleSession(codeBlock: PoaAmendmentData => Future[Result])(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Result] = {
    poaSessionService.getMongo flatMap {
      case Right(Some(poaData: PoaAmendmentData)) =>
        if (poaData.journeyCompleted) {
          Logger("application").info(s"The current active mongo Claim to Adjust POA session has been completed by the user, so a new session will be created")
          poaSessionService.createSession.flatMap {
            case Right(_) => codeBlock(poaData)
            case Left(ex: Throwable) =>
              Future.successful(logAndRedirect(
                s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >"))
          }
        } else {
          Logger("application").info(s"The current active mongo Claim to Adjust POA session has not been completed by the user")
          codeBlock(poaData)
        }
      case Right(None) =>
        Logger("application").info(s"There is no active mongo Claim to Adjust POA session, so a new one will be created")
        poaSessionService.createSession.flatMap(
          _ => codeBlock(PoaAmendmentData())
        )
      case Left(ex) =>
        Future.successful(logAndRedirect(
          s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >"))
    }
  }

}
