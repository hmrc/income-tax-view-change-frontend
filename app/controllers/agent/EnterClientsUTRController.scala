/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent

import config.featureswitch.{AgentViewer, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.SelectClientController
import controllers.agent.utils.SessionKeys
import forms.agent.ClientsUTRForm
import javax.inject.{Inject, Singleton}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import views.html.agent.EnterClientsUTR

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterClientsUTRController @Inject()(enterClientsUTR: EnterClientsUTR,
                                          val authorisedFunctions: AuthorisedFunctions)
                                         (implicit mcc: MessagesControllerComponents,
                                          val appConfig: FrontendAppConfig,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val ec: ExecutionContext)
  extends SelectClientController with I18nSupport with FeatureSwitching {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (!isEnabled(AgentViewer)) {
        Future.failed(new NotFoundException("[EnterClientsUTRController][show] - Agent viewer is disabled"))
      } else {
        Future.successful(Ok(enterClientsUTR(
          clientUTRForm = ClientsUTRForm.form,
          postAction = routes.EnterClientsUTRController.submit()
        )))
      }
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      if (!isEnabled(AgentViewer)) {
        Future.failed(new NotFoundException("[EnterClientsUTRController][submit] - Agent viewer is disabled"))
      } else {
        ClientsUTRForm.form.bindFromRequest.fold(
          hasErrors => Future.successful(BadRequest(enterClientsUTR(
            clientUTRForm = hasErrors,
            postAction = routes.EnterClientsUTRController.submit()
          ))),
          validUTR => {
            Future.successful(
              Redirect(routes.EnterClientsUTRController.show()).addingToSession(
                SessionKeys.clientFirstName -> "first name",
                SessionKeys.clientLastName -> "last name",
                SessionKeys.clientMTDID -> "mtditid",
                SessionKeys.clientNino -> "nino",
                SessionKeys.clientUTR -> validUTR
              )
            )
          }
        )
      }
  }

}
