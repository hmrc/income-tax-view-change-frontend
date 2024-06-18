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

package controllers.claimToAdjustPoa

import auth.MtdItUser
import cats.data.EitherT
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.Nino
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, ClaimToAdjustUtils}
import views.html.claimToAdjustPoa.PaymentsOnAccountAdjustedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsOnAccountAdjustedController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                    val view: PaymentsOnAccountAdjustedView,
                                                    val sessionService: PaymentOnAccountSessionService,
                                                    val claimToAdjustService: ClaimToAdjustService,
                                                    auth: AuthenticatorPredicate,
                                                    implicit val itvcErrorHandler: ItvcErrorHandler,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext)
  extends ClientConfirmedController with ClaimToAdjustUtils {

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      ifAdjustPoaIsEnabled(isAgent) {
        {
          for {
            poaMaybe <- EitherT(claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino)))
          } yield poaMaybe
        }.value.flatMap {
          case Right(Some(poa)) =>
            checkAPIDataSet(poa)
            setJourneyCompletedFlag(isAgent, poa)
          case Right(None) =>
            Logger("application").error(s"No payment on account data found")
            Future.successful(showInternalServerError(isAgent))
          case Left(ex) =>
            Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
            Future.successful(showInternalServerError(isAgent))
        }
      } recover {
        case ex: Exception =>
          Logger("application").error(s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
          showInternalServerError(isAgent)
      }
  }

  private def checkAPIDataSet(poa: PaymentOnAccountViewModel)(implicit hc: HeaderCarrier): Future[Unit] = {
    sessionService.getMongo.map {
      case Right(Some(sessionData)) =>
        if (sessionData.newPoAAmount.contains(poa.paymentOnAccountOne)) {
          Logger("application").info(s"Amount returned from API equals amount in mongo: ${poa.paymentOnAccountOne}")
        }
        else {
          Logger("application").error(s"Amount returned from API: ${poa.paymentOnAccountOne} does not equal amount in mongo: ${sessionData.newPoAAmount}")
        }
      case _ => Logger("application").error(s"Error connecting to mongo to verify API data set")
    }
  }

  private def setJourneyCompletedFlag(isAgent: Boolean, poa: PaymentOnAccountViewModel)(implicit user: MtdItUser[_]): Future[Result] = {
    sessionService.setCompletedJourney(hc, ec).flatMap {
      case Right(_) => Future.successful(Ok(view(isAgent, poa.taxYear, poa.paymentOnAccountOne)))
      case Left(ex) =>
        Logger("application").error(s"Error setting journey completed flag in mongo${ex.getMessage} - ${ex.getCause}")
        Future.successful(showInternalServerError(isAgent))
    }
  }
}