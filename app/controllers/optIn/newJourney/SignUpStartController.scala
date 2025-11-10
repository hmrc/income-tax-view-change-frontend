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

package controllers.optIn.newJourney

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.DateServiceInterface
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.JourneyCheckerSignUp
import views.html.optIn.newJourney.SignUpStart

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SignUpStartController @Inject()(authActions: AuthActions,
                                      signUpStart: SignUpStart,
                                      val optInService: OptInService,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      implicit val dateService: DateServiceInterface)
                                     (implicit val ec: ExecutionContext,
                                      val appConfig: FrontendAppConfig,
                                      mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with JourneyCheckerSignUp {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def buttonUrl(isAgent: Boolean, taxYear: String) = routes.SignUpTaxYearQuestionController.show(isAgent, Some(taxYear)).url

  def show(isAgent: Boolean, taxYear: Option[String]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    withSignUpRFChecks {
      withRecover(isAgent) {
        optInService.isSignUpTaxYearValid(taxYear).flatMap {
          case Some(viewModel) =>
            retrieveIsJourneyComplete.flatMap { journeyIsComplete =>
              if (!journeyIsComplete) {
                withSessionData(isStart = true, viewModel.signUpTaxYear.taxYear, None) {
                  Future.successful(
                    Ok(signUpStart(
                      isAgent,
                      dateService.getCurrentTaxYearEnd.equals(viewModel.signUpTaxYear.taxYear.endYear),
                      buttonUrl(isAgent, viewModel.signUpTaxYear.taxYear.startYear.toString)
                    ))
                  )
                }
              } else {
                Future.successful(Redirect(controllers.routes.SignUpOptOutCannotGoBackController.show(isAgent, isSignUpJourney = Some(true))))
              }
            }
          case None =>
            Logger("application").warn("[SignUpStartController.show] chosen tax year intent not found. Redirecting to reporting obligations page.")
            Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(isAgent)))
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
}