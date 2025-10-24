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

package controllers

import auth.MtdItUser
import auth.authV2.AuthActions
import com.google.inject.Inject
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.reportingObligations.ReportingObligationsUtils
import views.html.SignUpOptOutCannotGoBackView

import scala.concurrent.{ExecutionContext, Future}

class SignUpOptOutCannotGoBackController @Inject()(
                                                    authActions: AuthActions,
                                                    val view: SignUpOptOutCannotGoBackView
                                                  )
                                                  (
                                                    implicit val appConfig: FrontendAppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext
                                                  )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with ReportingObligationsUtils {

  def show(isAgent: Boolean, isSignUpJourney: Option[Boolean]): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    isSignUpJourney match {
      case Some(true) => Future.successful(Ok(view(isAgent, isSignUp = true)))
      case Some(false) => Future.successful(Ok(view(isAgent, isSignUp = false)))
      case _ => Future.successful(Redirect(controllers.routes.ReportingFrequencyPageController.show(isAgent)))
    }
  }

}