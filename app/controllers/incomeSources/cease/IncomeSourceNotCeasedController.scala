/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.cease

import auth.MtdItUser
import auth.authV2.AuthActions
import config.FrontendAppConfig
import enums.IncomeSourceJourney.IncomeSourceType
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.errorPages.templates.ErrorTemplateWithLink

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceNotCeasedController @Inject() (
    val authActions:   AuthActions,
    val errorTemplate: ErrorTemplateWithLink
  )(
    implicit val appConfig: FrontendAppConfig,
    mcc:                    MessagesControllerComponents,
    implicit val ec:        ExecutionContext)
    extends FrontendController(mcc)
    with I18nSupport {

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
      handleRequest(isAgent, incomeSourceType)
    }

  private def handleRequest(
      isAgent:          Boolean,
      incomeSourceType: IncomeSourceType
    )(
      implicit user: MtdItUser[_]
    ): Future[Result] = {
    val pageTitle = messagesApi.preferred(user)("standardError.heading")
    val heading   = messagesApi.preferred(user)("standardError.heading")
    val message   = messagesApi.preferred(user)(s"incomeSources.cease.error.${incomeSourceType.key}.notCeased.text")
    val linkText  = messagesApi.preferred(user)("incomeSources.cease.error.notCeased.link.text")
    val linkUrl =
      if (isAgent) routes.CeaseIncomeSourceController.showAgent().url else routes.CeaseIncomeSourceController.show().url
    val linkPrefix = Some(messagesApi.preferred(user)("incomeSources.cease.error.notCeased.link.prefix"))

    Future.successful {
      Ok(errorTemplate(pageTitle, heading, message, linkPrefix, linkText, linkUrl, isAgent = isAgent))
    }
  }
}
