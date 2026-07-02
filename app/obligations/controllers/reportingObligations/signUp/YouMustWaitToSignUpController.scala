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

package obligations.controllers.reportingObligations.signUp

import com.google.inject.Inject
import common.auth.AuthActions
import common.services.DateService
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import common.config.FrontendAppConfig
import obligations.views.html.reportingObligations.signUp.YouMustWaitToSignUpView

import scala.concurrent.Future

class YouMustWaitToSignUpController @Inject()(
                                               authActions: AuthActions,
                                               dateService: DateService,
                                               view: YouMustWaitToSignUpView
                                             )(implicit mcc: MessagesControllerComponents,
                                                appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport {

  def show(isAgent: Boolean): Action[AnyContent] = {
    lazy val auth = {
      if (isAgent) {
        authActions.checkSessionTimeout andThen
          authActions.authoriseAndRetrieveAgent.authorise() andThen
          authActions.retrieveClientData.authorise() andThen
          authActions.authoriseAndRetrieveMtdAgent andThen
          authActions.agentHasConfirmedClientAction andThen
          authActions.incomeSourceRetrievalAction andThen
          authActions.retrieveFeatureSwitches
      } else {
        authActions.checkSessionTimeout andThen
          authActions.authoriseAndRetrieveIndividual andThen
          authActions.incomeSourceRetrievalAction andThen
          authActions.retrieveFeatureSwitches andThen
          authActions.retrieveNavBar
      }

    }
    auth.async { implicit user =>
      val taxYear = dateService.getCurrentTaxYear
      val nextTaxYear = taxYear.endYear.toString
      val backUrl = appConfig.homePageUrl(isAgent)
      Future.successful(Ok(view(taxYear, nextTaxYear, backUrl)))
    }
  }
}
