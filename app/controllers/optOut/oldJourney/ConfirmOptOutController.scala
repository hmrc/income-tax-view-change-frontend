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

package controllers.optOut.oldJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import cats.data.OptionT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import controllers.optOut.routes
import models.optout.{MultiYearOptOutCheckpointViewModel, OneYearOptOutCheckpointViewModel, OptOutCheckpointViewModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.optOut.oldJourney.{CheckOptOutAnswers, ConfirmOptOut}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmOptOutController @Inject()(view: ConfirmOptOut,
                                        checkOptOutAnswers: CheckOptOutAnswers,
                                        optOutService: OptOutService,
                                        authActions: AuthActions,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val ec: ExecutionContext,
                                        val mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with ReportingObligationsUtils {

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutFS {
        withRecover(isAgent) {
          val cancelURL =
            if (isAgent) {
              controllers.optOut.oldJourney.routes.OptOutCancelledController.showAgent().url
            } else {
              controllers.optOut.oldJourney.routes.OptOutCancelledController.show().url
            }

          val resultToReturn = for {
            viewModel <- OptionT(optOutService.optOutCheckPointPageViewModel())
            result <- OptionT(Future(Option(toPropositionView(isAgent, viewModel, cancelURL))))
          } yield result

          resultToReturn.getOrElse(handleError("No qualified tax year available for opt out", isAgent))

        }
      }
  }

  private def toPropositionView(isAgent: Boolean, viewModel: OptOutCheckpointViewModel, cancelURL: String)(implicit mtdItUser: MtdItUser[_]) =
    viewModel match {
    case oneYear: OneYearOptOutCheckpointViewModel => Ok(view(oneYear, isAgent = isAgent, cancelURL))
    case multiYear: MultiYearOptOutCheckpointViewModel => Ok(checkOptOutAnswers(multiYear, isAgent, cancelURL))
  }

  def submit(isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        withOptOutFS {
          optOutService.makeOptOutUpdateRequest().map {
            case ITSAStatusUpdateResponseSuccess(_) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
            case _ => Redirect(controllers.optOut.oldJourney.routes.OptOutErrorController.show(isAgent))
          }
        }
    }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception => handleError(s"request failed :: $ex", isAgent)
    }
  }

  private def handleError(message: String, isAgent: Boolean)(implicit request: Request[_]): Result = {
    val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    Logger("application").error(message)
    errorHandler(isAgent).showInternalServerError()
  }

}