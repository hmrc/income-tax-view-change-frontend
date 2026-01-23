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
import audit.models.EnterClientUTRAuditModel
import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import auth.authV2.models.AuthorisedUserRequest
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.AuthUtils.*
import controllers.agent.sessionUtils.SessionKeys
import enums.*
import enums.MTDUserRole.{MTDPrimaryAgent, MTDSupportingAgent}
import forms.agent.ClientsUTRForm
import models.sessionData.SessionCookieData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.agent.ClientDetailsService
import services.agent.ClientDetailsService.{BusinessDetailsNotFound, CitizenDetailsNotFound}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.agent.EnterClientsUTRView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterClientsUTRController @Inject()(enterClientsUTR: EnterClientsUTRView,
                                          clientDetailsService: ClientDetailsService,
                                          val authorisedFunctions: FrontendAuthorisedFunctions,
                                          val authActions: AuthActions,
                                          val auditingService: AuditingService)
                                         (implicit mcc: MessagesControllerComponents,
                                          val appConfig: FrontendAppConfig,
                                          val itvcErrorHandler: AgentItvcErrorHandler,
                                          val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show: Action[AnyContent] = authActions.asAgent().async { implicit user =>
    Future.successful(Ok(enterClientsUTR(
      clientUTRForm = ClientsUTRForm.form,
      postAction = routes.EnterClientsUTRController.submit()
    )))
  }

  def showWithUtr(utr: String): Action[AnyContent] = authActions.asAgent().async { implicit user =>
    val utrSafe = utr.filter(_.isDigit).take(10)
    Future.successful(Ok(enterClientsUTR(
      clientUTRForm = ClientsUTRForm.form.fill(utrSafe),
      postAction = routes.EnterClientsUTRController.submit()
    )))
  }


  def submit: Action[AnyContent] = authActions.asAgent().async { implicit user =>
    ClientsUTRForm.form.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(enterClientsUTR(
        clientUTRForm = hasErrors,
        postAction = routes.EnterClientsUTRController.submit()
      ))),
      validUTR => {
        clientDetailsService.checkClientDetails(utr = validUTR)
          .flatMap {
            case Right(clientDetails) =>
              checkAgentAuthorisedAndGetRole(clientDetails.mtdItId).flatMap { userRole =>
                val sessionCookies: Seq[(String, String)] = SessionCookieData(clientDetails, validUTR, userRole == MTDSupportingAgent).toSessionCookieSeq
                sendAudit(true, user, validUTR, clientDetails.nino, clientDetails.mtdItId, Some(userRole == MTDSupportingAgent))
                Future.successful(Redirect(routes.ConfirmClientUTRController.show()).addingToSession(sessionCookies: _*))
              }.recover {
                case ex =>
                  Logger("application").error(s"[EnterClientsUTRController] - ${ex.getMessage} - ${ex.getCause}")
                  sendAudit(false, user, validUTR, clientDetails.nino, clientDetails.mtdItId, None)
                  Redirect(controllers.agent.routes.UTRErrorController.show())
              }

            case Left(CitizenDetailsNotFound | BusinessDetailsNotFound) =>
              val sessionValue: Seq[(String, String)] = Seq(SessionKeys.clientUTR -> validUTR)
              Future.successful(Redirect(routes.UTRErrorController.show()).addingToSession(sessionValue: _*))
            case Left(_) =>
              Logger("application").error(s"[EnterClientsUTRController] - Error response received from API")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
      }
    )
  }


  private def checkAgentAuthorisedAndGetRole(mtdItId: String)(implicit request: Request[_]): Future[MTDUserRole] = {
    authorisedFunctions
      .authorised(Enrolment(mtdEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
        .withDelegatedAuthRule(primaryAgentAuthRule)) {
        Future.successful(MTDPrimaryAgent)
      }.recoverWith { case e =>
        authorisedFunctions
          .authorised(Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
            .withDelegatedAuthRule(secondaryAgentAuthRule)) {
            Future.successful(MTDSupportingAgent)
          }
      }
  }

  private def sendAudit[A](isSuccessful: Boolean, user: AuthorisedUserRequest[A],
                           validUTR: String, nino: String, mtdItId: String, isSupportingAgent: Option[Boolean])(implicit request: Request[_]): Unit = {
    auditingService.extendedAudit(EnterClientUTRAuditModel(
      isSuccessful = isSuccessful,
      nino = nino,
      mtditid = mtdItId,
      arn = user.authUserDetails.agentReferenceNumber,
      saUtr = validUTR,
      credId = user.authUserDetails.credId,
      isSupportingAgent = isSupportingAgent
    )
    )
  }
}
