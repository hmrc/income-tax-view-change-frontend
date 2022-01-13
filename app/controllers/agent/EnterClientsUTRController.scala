/*
 * Copyright 2022 HM Revenue & Customs
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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.BaseAgentController
import controllers.agent.utils.SessionKeys
import controllers.predicates.agent.AgentAuthenticationPredicate.defaultAgentPredicates
import forms.agent.ClientsUTRForm
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.agent.ClientDetailsService
import services.agent.ClientDetailsService.{BusinessDetailsNotFound, CitizenDetailsNotFound, ClientDetails}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.EnterClientsUTR

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterClientsUTRController @Inject()(enterClientsUTR: EnterClientsUTR,
                                          clientDetailsService: ClientDetailsService,
                                          val authorisedFunctions: AuthorisedFunctions)
                                         (implicit mcc: MessagesControllerComponents,
                                          val appConfig: FrontendAppConfig,
                                          val itvcErrorHandler: AgentItvcErrorHandler,
                                          val ec: ExecutionContext)
  extends BaseAgentController with I18nSupport with FeatureSwitching {

  lazy val notAnAgentPredicate = {
    val redirectNotAnAgent = Future.successful(Redirect(controllers.agent.errors.routes.AgentErrorController.show()))
    defaultAgentPredicates(onMissingARN = redirectNotAnAgent)
  }

  def show: Action[AnyContent] = Authenticated.asyncWithoutClientAuth(notAnAgentPredicate) { implicit request =>
		implicit user =>
			Future.successful(Ok(enterClientsUTR(
				clientUTRForm = ClientsUTRForm.form,
				postAction = routes.EnterClientsUTRController.submit()
			)))
  }

  def submit: Action[AnyContent] = Authenticated.asyncWithoutClientAuth() { implicit request =>
		implicit user =>
			ClientsUTRForm.form.bindFromRequest.fold(
				hasErrors => Future.successful(BadRequest(enterClientsUTR(
					clientUTRForm = hasErrors,
					postAction = routes.EnterClientsUTRController.submit()
				))),
				validUTR => {
					clientDetailsService.checkClientDetails(
						utr = validUTR
					) map {
						case Right(ClientDetails(firstName, lastName, nino, mtdItId)) =>
							val sessionValues: Seq[(String, String)] = Seq(
								SessionKeys.clientMTDID -> mtdItId,
								SessionKeys.clientNino -> nino,
								SessionKeys.clientUTR -> validUTR
							) ++ firstName.map(SessionKeys.clientFirstName -> _) ++ lastName.map(SessionKeys.clientLastName -> _)
							Redirect(routes.ConfirmClientUTRController.show()).addingToSession(sessionValues: _*)
						case Left(CitizenDetailsNotFound | BusinessDetailsNotFound) =>
							val sessionValue: Seq[(String, String)] = Seq(SessionKeys.clientUTR -> validUTR)
							Redirect(routes.UTRErrorController.show()).addingToSession(sessionValue: _*)
						case Left(_) =>
							throw new InternalServerException("[EnterClientsUTRController][submit] - Unexpected response received")
					}
				}
			)
  }
}
