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
import cats.data.EitherT
import enums.IncomeSourceJourney.{BeforeSubmissionPage, InitialPage, JourneyState}
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoAAmendmentData}
import models.core.Nino
import play.api.Logger
import play.api.mvc.Result
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait WithSessionAndPoa extends JourneyCheckerClaimToAdjust {
  self =>

  val claimToAdjustService: ClaimToAdjustService

  def withSessionDataAndPoa(journeyState: JourneyState = BeforeSubmissionPage)
                           (codeBlock: (PoAAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                           (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    ifAdjustPoaIsEnabled(isAgent(user)) {
      {
        if (journeyState == InitialPage) {
          handleSessionAndPoaStartPage(codeBlock)
        } else {
          handleSessionAndPoa(journeyState)(codeBlock)
        }
      } fold (
        logAndShowErrorPage(isAgent(user)),
        view => view
      )
    }
  }

  private def handleSessionAndPoaStartPage(codeBlock: (PoAAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                              (implicit hc: HeaderCarrier, user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {
    for {
      session <- EitherT(poaSessionService.getMongo)
      poa <- EitherT(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)))
      result <- (session, poa) match {
        case (Some(s), Some(p)) =>
          if (s.journeyCompleted) {
            Logger("application").info(s"The current active mongo Claim to Adjust POA session has been completed by the user, so a new session will be created")
            createSessionCodeBlock(p)(codeBlock)
          } else {
            Logger("application").info(s"The current active mongo Claim to Adjust POA session has not been completed by the user")
            codeBlock(s, p)
          }
        case (None, Some(p)) =>
          createSessionCodeBlock(p)(codeBlock)
        case (_, None) =>
          Logger("application").error(s"Error: POA missing")
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(errorHandler.showInternalServerError())
          x
      }
    } yield result
  }

  private def createSessionCodeBlock(p: PaymentOnAccountViewModel)
                                    (codeBlock: (PoAAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                                    (implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): EitherT[Future, Throwable, Result] = {
    EitherT(poaSessionService.createSession.map {
      case Right(_) =>
        codeBlock(PoAAmendmentData(), p).value
      case Left(ex: Throwable) =>
        Logger("application").error(if (isAgent(user)) "[Agent]" else "" +
          s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >")
        val x: EitherT[Future, Throwable, Result] = EitherT.rightT(errorHandler.showInternalServerError())
        x.value
    }.flatten)
  }

  private def handleSessionAndPoa(journeyState: JourneyState)(codeBlock: (PoAAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                            (implicit hc: HeaderCarrier, user: MtdItUser[_]): EitherT[Future, Throwable, Result] = {
    for {
      session <- EitherT(poaSessionService.getMongo)
      poa <- EitherT(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)))
      result <- (session, poa) match {
        case (Some(s), Some(_)) if showCannotGoBackErrorPage(s.journeyCompleted, journeyState) =>
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(redirectToYouCannotGoBackPage(user))
          x
        case (Some(s), Some(p)) =>
          codeBlock(s, p)
        case (None, _) =>
          Logger("application").error(s"Error, session missing")
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(errorHandler.showInternalServerError())
          x
        case (_, None) =>
          Logger("application").error(s"Error, relevant POA not found")
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(errorHandler.showInternalServerError())
          x
      }
    } yield result
  }

  private def logAndShowErrorPage(isAgent: Boolean)(ex: Throwable)(implicit user: MtdItUser[_]): Result = {
    Logger("application").error(if (isAgent) "[Agent]" else "" + s"${ex.getMessage} - ${ex.getCause}")
    errorHandler.showInternalServerError()
  }

}
