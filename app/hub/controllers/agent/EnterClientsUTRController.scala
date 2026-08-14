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

package hub.controllers.agent

import hub.audit.models.EnterClientUTRAuditModel
import common.auth.{AuthActions, AuthorisedUserRequest, FrontendAuthorisedFunctions}
import common.config.{AgentItvcErrorHandler, FrontendAppConfig}
import common.config.featureswitch.FeatureSwitching
import common.services.AuditingService
import common.services.agent.ClientDetailsService
import common.utils.AuthUtils.*
import common.utils.sessionUtils.SessionKeys
import hub.forms.agent.ClientsUTRForm
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.*
import ClientDetailsService.{BusinessDetailsNotFound, CitizenDetailsNotFound}
import common.enums.{MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import common.models.sessionData.SessionCookieData
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import hub.views.html.agent.EnterClientsUTRView

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
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with Logging {

  lazy val postAction =
    if(appConfig.isNewHubUrl)
      routes.EnterClientsUTRController.submitNewUrl()
    else
      routes.EnterClientsUTRController.submit()

  def show: Action[AnyContent] = authActions.asAgent().async { implicit user =>

    Future.successful(Ok(enterClientsUTR(
      clientUTRForm = ClientsUTRForm.form,
      postAction = postAction
    )))
  }

  def showWithUtr(utr: String): Action[AnyContent] = authActions.asAgent().async { implicit user =>
    val utrSafe = utr.filter(_.isDigit).take(10)
    Future.successful(Ok(enterClientsUTR(
      clientUTRForm = ClientsUTRForm.form.fill(utrSafe),
      postAction = postAction
    )))
  }

  def submit: Action[AnyContent] = handleSubmit

  def submitNewUrl: Action[AnyContent] = handleSubmit


  def handleSubmit: Action[AnyContent] = authActions.asAgent().async { implicit user =>
    ClientsUTRForm.form.bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(enterClientsUTR(
        clientUTRForm = hasErrors,
        postAction = postAction
      ))),
      validUTR => {
        clientDetailsService.checkClientDetails(utr = validUTR)
          .flatMap {
            case Right(clientDetails) =>
              checkAgentAuthorisedAndGetRole(clientDetails.mtdItId).flatMap { userRole =>
                val sessionCookies: Seq[(String, String)] = SessionCookieData(clientDetails, validUTR, userRole == MTDSupportingAgent).toSessionCookieSeq
                sendAudit(true, user, validUTR, clientDetails.nino, clientDetails.mtdItId, Some(userRole == MTDSupportingAgent))
                Future.successful(Redirect(appConfig.confirmClientUTRUrl).addingToSession(sessionCookies: _*))
              }.recover {
                case ex =>
                  logger.error(s"[submit] - ${ex.getMessage} - ${ex.getCause}")
                  sendAudit(false, user, validUTR, clientDetails.nino, clientDetails.mtdItId, None)
                  Redirect(appConfig.utrErrorUrl)
              }

            case Left(CitizenDetailsNotFound | BusinessDetailsNotFound) =>
              val sessionValue: Seq[(String, String)] = Seq(SessionKeys.clientUTR -> validUTR)
              Future.successful(Redirect(appConfig.utrErrorUrl).addingToSession(sessionValue: _*))
            case Left(_) =>
              logger.error(s"[submit] - Error response received from API")
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
