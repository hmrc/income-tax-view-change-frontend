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

package controllers.agent

import audit.AuditingService
import audit.models.ConfirmClientDetailsAuditModel
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ConfirmClientController
import sessionUtils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.AuthenticatorPredicate
import views.html.agent.confirmClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientUTRController @Inject()(confirmClient: confirmClient,
                                           val authorisedFunctions: AuthorisedFunctions,
                                           val auditingService: AuditingService,
                                           val auth: AuthenticatorPredicate)
                                          (implicit mcc: MessagesControllerComponents,
                                           val appConfig: FrontendAppConfig,
                                           val itvcErrorHandler: AgentItvcErrorHandler,
                                           val ec: ExecutionContext)
  extends ConfirmClientController with FeatureSwitching with I18nSupport {

  def show: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      Future.successful(Ok(confirmClient(
        clientName = fetchClientName,
        clientUtr = fetchClientUTR,
        postAction = routes.ConfirmClientUTRController.submit,
        backUrl = backUrl
      )))
  }

  def submit: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        clientName <- fetchClientName
        nino <- request.session.get(SessionKeys.clientNino)
        clientMTDID <- request.session.get(SessionKeys.clientMTDID)
        arn <- user.agentReferenceNumber
        saUtr <- request.session.get(SessionKeys.clientUTR)
      } yield
        auditingService.extendedAudit(ConfirmClientDetailsAuditModel(
          clientName = clientName,
          nino = nino,
          mtditid = clientMTDID,
          arn = arn,
          saUtr = saUtr,
          credId = user.credId
        ))
      Future.successful(
        Redirect(controllers.routes.HomeController.showAgent().url).addingToSession(
          SessionKeys.confirmedClient -> "true"
        )
      )
  }

  lazy val backUrl: String = controllers.agent.routes.EnterClientsUTRController.show.url


}
