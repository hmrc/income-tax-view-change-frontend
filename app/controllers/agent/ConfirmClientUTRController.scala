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
import models.sessionData.SessionDataPostResponse.{SessionDataPostFailure, SessionDataPostSuccess}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ITSAStatusService, SessionDataService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.agent.confirmClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConfirmClientUTRController @Inject()(confirmClient: confirmClient,
                                           val authActions: AuthActions,
                                           val auditingService: AuditingService,
                                           val sessionDataService: SessionDataService,
                                           val ITSAStatusService: ITSAStatusService)
                                          (implicit mcc: MessagesControllerComponents,
                                           val appConfig: FrontendAppConfig,
                                           val itvcErrorHandler: AgentItvcErrorHandler,
                                           val ec: ExecutionContext)
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  def show: Action[AnyContent] = authActions.asMTDAgentWithUnconfirmedClient { implicit user =>
    Ok(confirmClient(
      clientName = user.optClientNameAsString,
      clientUtr = user.saUtr,
      postAction = routes.ConfirmClientUTRController.submit(),
      backUrl = backUrl
    ))
  }

  def submit: Action[AnyContent] = authActions.asMTDAgentWithUnconfirmedClient.async { implicit user =>
    val clientName = user.optClientNameAsString.getOrElse("")
    val names = clientName.split(" ")

   ITSAStatusService.hasMandatedOrVoluntaryStatusCurrentYear(user.nino, _.isMandated).flatMap( mandation => {
      val mandationStatus = if (mandation) "on" else "off"
      handleSessionCookies(SessionCookieData(user.mtditid, user.nino, user.saUtr.getOrElse(""),
        names.headOption, names.lastOption, user.isSupportingAgent, mandationStatus)) { _ =>
        auditingService.extendedAudit(ConfirmClientDetailsAuditModel(
          clientName = clientName,
          nino = user.nino,
          mtditid = user.mtditid,
          arn = user.arn.getOrElse(""),
          saUtr = user.saUtr.getOrElse(""),
          isSupportingAgent = user.isSupportingAgent,
          credId = user.credId
        ))
        Future.successful(Redirect(controllers.routes.HomeController.showAgent().url).addingToSession(SessionKeys.confirmedClient -> "true"))
      }
    })

  }

  lazy val backUrl: String = controllers.agent.routes.EnterClientsUTRController.show().url

  def getSessionDataStorageFS: Boolean = appConfig.isSessionDataStorageEnabled

  def handleSessionCookies(sessionCookieData: SessionCookieData)(codeBlock: Seq[(String, String)] => Future[Result])
                          (implicit hc: HeaderCarrier, request: Request[_], ec: ExecutionContext): Future[Result] = {
    if (getSessionDataStorageFS) {
      sessionDataService.postSessionData(sessionCookieData.toSessionDataModel).flatMap {
        case Left(value: SessionDataPostFailure) =>
          Logger("application").error(s"[Agent] Posting user session data was unsuccessful. Status: ${value.status}, error message: ${value.errorMessage}")
          Future.successful(itvcErrorHandler.showInternalServerError())
        case Right(value: SessionDataPostSuccess) =>
          Logger("application").info(s"[Agent] Posting user session data was successful. Status: ${value.status}")
          codeBlock(sessionCookieData.toSessionCookieSeq)
      }
    } else {
      Logger("application").info(s"[Agent] GetUserSessionApi feature switch was off so session data has not been posted to the session-data service")
      codeBlock(sessionCookieData.toSessionCookieSeq)
    }
  }

}
