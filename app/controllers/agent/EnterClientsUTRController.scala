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

import AuthUtils._
import audit.AuditingService
import audit.models.EnterClientUTRAuditModel
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.BaseAgentController
import controllers.agent.sessionUtils.SessionKeys
import controllers.predicates.AuthPredicate.AuthPredicate
import controllers.predicates.IncomeTaxAgentUser
import controllers.predicates.agent.AgentAuthenticationPredicate.defaultAgentPredicates
import enums.{PrimaryAgent, SecondaryAgent, UserRole}
import forms.agent.ClientsUTRForm
import models.sessionData.SessionCookieData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import services.SessionDataService
import services.agent.ClientDetailsService
import services.agent.ClientDetailsService.{BusinessDetailsNotFound, CitizenDetailsNotFound, ClientDetails}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, authorisedEnrolments, confidenceLevel, credentials}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment}
import utils.SessionCookieUtil
import views.html.agent.EnterClientsUTR

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnterClientsUTRController @Inject()(enterClientsUTR: EnterClientsUTR,
                                          clientDetailsService: ClientDetailsService,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          val auditingService: AuditingService,
                                          val sessionDataService: SessionDataService)
                                         (implicit mcc: MessagesControllerComponents,
                                          val appConfig: FrontendAppConfig,
                                          val itvcErrorHandler: AgentItvcErrorHandler,
                                          val ec: ExecutionContext)
  extends BaseAgentController with I18nSupport with FeatureSwitching with SessionCookieUtil {

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
          ) flatMap {
            case Right(clientDetails) =>
              checkAgentAuthorisedAndGetRole(clientDetails.mtdItId).flatMap{userRole =>
                val sessionCookieData: SessionCookieData = SessionCookieData(clientDetails, validUTR, userRole == SecondaryAgent)
                handleSessionCookies(sessionCookieData) { sessionCookies =>
                  sendAudit(true, user, sessionCookieData.utr, sessionCookieData.nino, sessionCookieData.mtditid)
                  Future.successful(Redirect(routes.ConfirmClientUTRController.show).addingToSession(sessionCookies: _*))
                }
              }.recover{
                case ex =>
                  Logger("application")
                    .error(s"[EnterClientsUTRController] - ${ex.getMessage} - ${ex.getCause}")
                  sendAudit(false, user, validUTR, clientDetails.nino, clientDetails.mtdItId)
                  Redirect(controllers.agent.routes.UTRErrorController.show)
              }

            case Left(CitizenDetailsNotFound | BusinessDetailsNotFound)
            =>
              val sessionValue: Seq[(String, String)] = Seq(SessionKeys.clientUTR -> validUTR)
              Future.successful(Redirect(routes.UTRErrorController.show).addingToSession(sessionValue: _*))
            case Left(_)
            =>
              Logger("application").error(s"Error response received from API")
              Future.successful(itvcErrorHandler.showInternalServerError())
          }
        }
      )
  }


  private def checkAgentAuthorisedAndGetRole(mtdItId: String)(implicit request: Request[_]): Future[UserRole] = {
    authorisedFunctions
      .authorised(Enrolment(primaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
        .withDelegatedAuthRule(primaryAgentAuthRule)).retrieve(allEnrolments and affinityGroup and confidenceLevel and credentials) {
        case _ ~ _ ~ _ ~ _ => Future.successful(PrimaryAgent)
      }.fallbackTo {
        authorisedFunctions
          .authorised(Enrolment(secondaryAgentEnrolmentName).withIdentifier(agentIdentifier, mtdItId)
            .withDelegatedAuthRule(secondaryAgentAuthRule)).retrieve(allEnrolments and affinityGroup and confidenceLevel and credentials)
          { case _ ~ _ ~ _ ~ _ => Future.successful(SecondaryAgent)
          }
      }
  }

    private def sendAudit(isSuccessful: Boolean, user: IncomeTaxAgentUser, validUTR: String, nino: String, mtdItId: String)(implicit request: Request[_]): Unit = {
      auditingService.extendedAudit(EnterClientUTRAuditModel(
        isSuccessful = isSuccessful,
        nino = nino,
        mtditid = mtdItId,
        arn = user.agentReferenceNumber,
        saUtr = validUTR,
        credId = user.credId
      )
      )
    }
}
