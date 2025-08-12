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

package controllers.optOutNew

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import controllers.optOut.routes
import models.incomeSourceDetails.TaxYear
import models.optoutnew.CheckOptOutUpdateAnswersViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optout.OptOutService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.ReportingObligationsUtils
import views.html.optOutNew.CheckOptOutUpdateAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmOptOutUpdateController @Inject()(view: CheckOptOutUpdateAnswers,
                                              optOutService: OptOutService,
                                              authActions: AuthActions,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                             (implicit val appConfig: FrontendAppConfig,
                                        val ec: ExecutionContext,
                                        val mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with ReportingObligationsUtils {


  def show(isAgent: Boolean = false, taxYear: String): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptOutFS {
        withRecover(isAgent) {
          val selectedTaxYear: TaxYear = TaxYear(taxYear.toInt, taxYear.toInt + 1)
          val cancelURL = controllers.optOut.routes.OptOutCancelledController.show().url
          Future.successful(Ok(view(CheckOptOutUpdateAnswersViewModel(selectedTaxYear), isAgent, cancelURL)))
        }
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        withOptOutFS {
          optOutService.makeOptOutUpdateRequest().map {
            case ITSAStatusUpdateResponseSuccess(_) => Redirect(routes.ConfirmedOptOutController.show(isAgent))
            case _ => Redirect(routes.OptOutErrorController.show(isAgent))
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