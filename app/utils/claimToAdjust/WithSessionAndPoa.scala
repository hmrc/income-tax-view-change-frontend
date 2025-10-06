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
import enums.{BeforeSubmissionPage, InitialPage, JourneyState}
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoaAmendmentData}
import models.core.Nino
import play.api.Logger
import play.api.mvc.Result
import services.ClaimToAdjustService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait WithSessionAndPoa extends JourneyCheckerClaimToAdjust {
  self =>

  val claimToAdjustService: ClaimToAdjustService

  def withSessionDataAndPoa(journeyState: JourneyState = BeforeSubmissionPage)
                           (codeBlock: (PoaAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                           (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
      {
        if (journeyState == InitialPage) {
          handleSessionAndPoaStartPage(codeBlock)
        } else {
          handleSessionAndPoa(journeyState)(codeBlock)
        }
      } fold (
        logAndRedirect,
        view => view
      )
  }

  private def handleSessionAndPoaStartPage(codeBlock: (PoaAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
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
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(logAndRedirect(s"Error: POA missing"))
          x
      }
    } yield result
  }

  private def createSessionCodeBlock(p: PaymentOnAccountViewModel)
                                    (codeBlock: (PoaAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
                                    (implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): EitherT[Future, Throwable, Result] = {
    EitherT(poaSessionService.createSession.map {
      case Right(_) =>
        codeBlock(PoaAmendmentData(), p).value
      case Left(ex: Throwable) =>
        val x: EitherT[Future, Throwable, Result] = EitherT.rightT(
          logAndRedirect(s"There was an error while retrieving the mongo data. < Exception message: ${ex.getMessage}, Cause: ${ex.getCause} >"))
        x.value
    }.flatten)
  }

  private def handleSessionAndPoa(journeyState: JourneyState)(codeBlock: (PoaAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result])
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
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(logAndRedirect(s"Error, session missing"))
          x
        case (_, None) =>
          val x: EitherT[Future, Throwable, Result] = EitherT.rightT(logAndRedirect(s"Error, relevant POA not found"))
          x
      }
    } yield result
  }

}
