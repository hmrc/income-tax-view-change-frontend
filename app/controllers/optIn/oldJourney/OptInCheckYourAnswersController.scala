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

package controllers.optIn.oldJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponseSuccess
import controllers.optIn.oldJourney.routes.OptInErrorController
import models.optin.MultiYearCheckYourAnswersViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.DateService
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.optIn.oldJourney.CheckYourAnswersView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

class OptInCheckYourAnswersController @Inject()(val view: CheckYourAnswersView,
                                                val optInService: OptInService,
                                                val authActions: AuthActions,
                                                val itvcErrorHandler: ItvcErrorHandler,
                                                val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                               (
                                                 implicit val dateService: DateService,
                                                 val appConfig: FrontendAppConfig,
                                                 mcc: MessagesControllerComponents,
                                                 val ec: ExecutionContext
                                               ) extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ReportingObligationsUtils {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def redirectToCheckpointPage(isAgent: Boolean): Result = {
    val nextPage = controllers.optIn.oldJourney.routes.OptInCompletedController.show(isAgent)
    Logger("application").info(s"redirecting to : $nextPage")
    Redirect(nextPage)
  }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  def show(isAgent: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptInRFChecks {
        withRecover(isAgent) {

          optInService.getMultiYearCheckYourAnswersViewModel(isAgent) map {
            case Some(model) =>
              Ok(view(
                MultiYearCheckYourAnswersViewModel(
                  model.intentTaxYear,
                  model.isAgent,
                  model.cancelURL,
                  model.intentIsNextYear
                )
              ))
            case None => errorHandler(isAgent).showInternalServerError()
          }
        }
      }
  }

  def submit(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withOptInRFChecks {
        optInService.makeOptInCall() map {
          case ITSAStatusUpdateResponseSuccess(_) => redirectToCheckpointPage(isAgent)
          case _ => Redirect(OptInErrorController.show(isAgent))
        }
      }
  }
}
