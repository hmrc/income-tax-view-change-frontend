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

package controllers.agent

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.BaseAgentController
import controllers.agent.utils.SessionKeys
import controllers.predicates.AuthPredicate.{AuthPredicate, AuthPredicateSuccess}
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate
import controllers.predicates.agent.AgentAuthenticationPredicate.{clientDetailsPredicates, confirmedClientPredicates, defaultAgentPredicates, defaultPredicates}
import forms.agent.ClientsUTRForm
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.agent.ClientDetailsService
import services.agent.ClientDetailsService.{BusinessDetailsNotFound, CitizenDetailsNotFound, ClientDetails, ClientDetailsFailure}
import uk.gov.hmrc.auth.core.authorise.{EmptyPredicate, Predicate}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel, credentials}
import uk.gov.hmrc.auth.core.retrieve.{Credentials, ~}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisedFunctions, ConfidenceLevel, Enrolment, Enrolments, User}
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

  lazy val notAnAgentPredicate: AuthPredicate[IncomeTaxAgentUser] = {
    val redirectNotAnAgent = Future.successful(Redirect(controllers.agent.errors.routes.AgentErrorController.show))
    defaultAgentPredicates(onMissingARN = redirectNotAnAgent)
  }

  def show: Action[AnyContent] = Authenticated.asyncWithoutClientAuth(notAnAgentPredicate) { implicit request =>
    implicit user =>
      Future.successful(Ok(enterClientsUTR(
        clientUTRForm = ClientsUTRForm.form,
        postAction = routes.EnterClientsUTRController.submit
      )))
  }

  def showWithUtr(utr: String): Action[AnyContent] = Authenticated.asyncWithoutClientAuth(notAnAgentPredicate) { implicit request =>
    implicit user =>
      val utrSafe = utr.filter(_.isDigit).take(10)
      Future.successful(Ok(enterClientsUTR(
        clientUTRForm = ClientsUTRForm.form.fill(utrSafe),
        postAction = routes.EnterClientsUTRController.submit
      )))
  }

  def redirect: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit agent =>
      Future.successful(Redirect(routes.ConfirmClientUTRController.show))
  }

  def submit: Action[AnyContent] = Authenticated.asyncWithoutClientAuth() { implicit request =>
    implicit user =>
      ClientsUTRForm.form.bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(enterClientsUTR(
          clientUTRForm = hasErrors,
          postAction = routes.EnterClientsUTRController.submit
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
              Redirect(routes.EnterClientsUTRController.redirect).addingToSession(sessionValues: _*)
            case Left(CitizenDetailsNotFound | BusinessDetailsNotFound) =>
              val sessionValue: Seq[(String, String)] = Seq(SessionKeys.clientUTR -> validUTR)
              Redirect(routes.UTRErrorController.show).addingToSession(sessionValue: _*)
            case Left(error) =>
              throw new InternalServerException("[EnterClientsUTRController][submit] - Unexpected response received")
          }
        }
      )
  }
}
