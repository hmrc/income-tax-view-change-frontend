/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.optIn

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.admin.{OptInOptOutContentUpdateR17, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.DateService
import services.optIn.OptInService
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.optIn.SignUpStart

import scala.concurrent.{ExecutionContext, Future}

class SignUpStartController @Inject()(authActions: AuthActions,
                                      signUpStart: SignUpStart,
                                      optInService: OptInService,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      dateService: DateService)
                                     (implicit val ec: ExecutionContext,
                                      val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def buttonUrl(isAgent: Boolean) = routes.SignUpStartController.show(isAgent).url // TODO: Replace with actual URL once journey is built

  def show(isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    withRFAndOptInOptOutR17FS {
      withRecover(isAgent) {
        for {
          _ <- optInService.saveIntent(TaxYear(2025, 2026)) // TODO: Only for testing the page. Remove once the journey is built.
          chosenTaxYear <- optInService.fetchSavedChosenTaxYear()
        } yield {
          chosenTaxYear match {
            case Some(taxYear) => Ok(signUpStart(isAgent, dateService.getCurrentTaxYearEnd.equals(taxYear.endYear), buttonUrl(isAgent)))
            case None =>
              Logger("application").warn("[SignUpStartController.show] chosen tax year intent not found. Redirecting to reporting obligations page.")
              Redirect(controllers.routes.ReportingFrequencyPageController.show(isAgent))
          }
        }
      }
    }
  }

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  private def withRFAndOptInOptOutR17FS(codeBlock: => Future[Result])(implicit user: MtdItUser[_]): Future[Result] = {
    (isEnabled(ReportingFrequencyPage), isEnabled(OptInOptOutContentUpdateR17)) match {
      case (true, true) => codeBlock
      case (true, false) =>
        user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(true)))
          case _ => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(false)))
        }
      case (false, _) =>
        user.userType match {
          case Some(Agent) => Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
          case _ => Future.successful(Redirect(controllers.routes.HomeController.show()))
        }
    }
  }

}
