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

import audit.AuditingService
import audit.models.ConfirmClientDetailsAuditModel
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.sessionUtils.SessionKeys
import models.sessionData.SessionCookieData
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SessionDataService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{AuthenticatorPredicate, SessionCookieUtil}
import views.html.agent.confirmClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientUTRController @Inject()(confirmClient: confirmClient,
                                           val authActions: AuthActions,
                                           val auditingService: AuditingService,
                                           val auth: AuthenticatorPredicate,
                                           val sessionDataService: SessionDataService)
                                          (implicit mcc: MessagesControllerComponents,
                                           val appConfig: FrontendAppConfig,
                                           val itvcErrorHandler: AgentItvcErrorHandler,
                                           val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with SessionCookieUtil{

  def show: Action[AnyContent] = authActions.asMTDAgentWithUnconfirmedClient { implicit user =>
    Ok(confirmClient(
      clientName = user.optClientNameAsString,
      clientUtr = user.saUtr,
      postAction = routes.ConfirmClientUTRController.submit,
      backUrl = backUrl
    ))
  }

  def submit: Action[AnyContent] = authActions.asMTDAgentWithUnconfirmedClient.async { implicit user =>
    val clientName = user.optClientNameAsString.getOrElse("")
    val names = clientName.split(" ")
    handleSessionCookies(SessionCookieData(user.mtditid, user.nino, user.saUtr.getOrElse(""),
      names.headOption, names.lastOption, user.isSupportingAgent)){ _ =>
      auditingService.extendedAudit(ConfirmClientDetailsAuditModel(
        clientName = clientName,
        nino = user.nino,
        mtditid = user.mtditid,
        arn = user.arn.getOrElse(""),
        saUtr = user.saUtr.getOrElse(""),
        credId = user.credId
      ))
      Future.successful(Redirect(controllers.routes.HomeController.showAgent.url).addingToSession(
        SessionKeys.confirmedClient -> "true"
      ))
    }
  }

  lazy val backUrl: String = controllers.agent.routes.EnterClientsUTRController.show.url

}
