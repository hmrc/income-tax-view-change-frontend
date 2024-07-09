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
import enums.IncomeSourceJourney.AfterSubmissionPage
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoAAmendmentData}
import play.api.Logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.claimToAdjust.{ClaimToAdjustUtils, WithSessionAndPoa}
import utils.AuthenticatorPredicate
import views.html.claimToAdjustPoa.PaymentsOnAccountAdjustedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PaymentsOnAccountAdjustedController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                                    val view: PaymentsOnAccountAdjustedView,
                                                    val poaSessionService: PaymentOnAccountSessionService,
                                                    val claimToAdjustService: ClaimToAdjustService,
                                                    auth: AuthenticatorPredicate,
                                                    implicit val itvcErrorHandler: ItvcErrorHandler,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext)
  extends ClientConfirmedController with ClaimToAdjustUtils with WithSessionAndPoa {

  def show(isAgent: Boolean): Action[AnyContent] = auth.authenticatedAction(isAgent) {
    implicit user =>
      withSessionDataAndPoa(journeyState = AfterSubmissionPage) { (session, poa) =>
        checkAndLogAPIDataSet(session, poa)
        EitherT.liftF(setJourneyCompletedFlag(isAgent, poa))
      } recover {
        case ex: Exception =>
          Logger("application").error(if (isAgent) "[Agent]" else "" + s"Unexpected error: ${ex.getMessage} - ${ex.getCause}")
          showInternalServerError(isAgent)
      }
  }

  private def checkAndLogAPIDataSet(session: PoAAmendmentData, poa: PaymentOnAccountViewModel): Unit = {
    if (session.newPoAAmount.contains(poa.totalAmountOne)) {
      Logger("application").info(s"Amount returned from API equals amount in mongo: ${poa.totalAmountOne}")
    }
    else {
      Logger("application").error(s"Amount returned from API: ${poa.totalAmountOne} does not equal amount in mongo: ${session.newPoAAmount}")
    }
  }

  private def setJourneyCompletedFlag(isAgent: Boolean, poa: PaymentOnAccountViewModel)(implicit user: MtdItUser[_]): Future[Result] = {
    poaSessionService.setCompletedJourney(hc, ec).flatMap {
      case Right(_) => Future.successful(Ok(view(isAgent, poa.taxYear, poa.totalAmountOne)))
      case Left(ex) =>
        Logger("application").error(s"Error setting journey completed flag in mongo${ex.getMessage} - ${ex.getCause}")
        Future.successful(showInternalServerError(isAgent))
    }
  }
}