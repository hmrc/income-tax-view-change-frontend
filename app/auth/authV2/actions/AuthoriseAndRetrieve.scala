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

package auth.authV2.actions

import audit.AuditingService
import audit.models.IvUpliftRequiredAuditModel
import auth._
import auth.authV2.EnroledUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.agent.sessionUtils.SessionKeys
import models.OriginEnum
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import services.SessionDataService
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.net.URLEncoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthoriseAndRetrieve @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                     val appConfig: FrontendAppConfig,
                                     override val config: Configuration,
                                     override val env: Environment,
                                     mcc: MessagesControllerComponents,
                                     val auditingService: AuditingService,
                                     sessionDataService: SessionDataService)
  extends AuthRedirects with ActionRefiner[Request, EnroledUser] with FeatureSwitching {

  val requireClientSelected: Boolean = true

  implicit val executionContext: ExecutionContext = mcc.executionContext
  val requiredConfidenceLevel: Int = appConfig.requiredConfidenceLevel

  def requiredClientId(implicit request: Request[_], hc: HeaderCarrier): Future[Option[String]] = {
    if (appConfig.isSessionDataStorageEnabled){
      sessionDataService.getSessionData().map {
        case Left(_) => request.session.get(SessionKeys.clientMTDID).filter(_ => requireClientSelected)
        case Right(value) => Some(value.mtditid)
      }
    }
    else {
      Future.successful(request.session.get(SessionKeys.clientMTDID).filter(_ => requireClientSelected))
    }
  }

  override protected def refine[A](request: Request[A]): Future[Either[Result, EnroledUser[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val req: Request[A] = request

    // Has agent affinity group, HMRC-AS-AGENT enrolment, if has client then also
    // has HMRC-MTD-IT enrolment with client's ID
    def isAgent(clientMtdId: Option[String]): Predicate = {

      // Agent identified by ARN - previously parsed from enrolment here:
      // controllers.predicates.IncomeTaxAgentUser.agentReferenceNumber
      def isAgent(): Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent

      // previously authorised here:
      // auth.BaseFrontendController.AuthenticatedActions.asyncInternal
      def hasClient(id: String) = Enrolment(appConfig.mtdItEnrolmentKey)
        .withIdentifier(appConfig.mtdItIdentifierKey, id)
        .withDelegatedAuthRule("mtd-it-auth")

      clientMtdId match {
        case Some(mtdId) => isAgent() and hasClient(mtdId)
        case _ => isAgent()
      }
    }

    // if user is not agent, authorise on HMRC-MTD-IT enrolment and Individual /
    // Organisation affinity group
    val isIndividual: Predicate = Enrolment(appConfig.mtdItEnrolmentKey) and (AffinityGroup.Organisation or AffinityGroup.Individual)


    requiredClientId flatMap{ requiredId =>
      authorisedFunctions
        .authorised(isIndividual or isAgent(requiredId))
        .retrieve(allEnrolments and name and credentials and affinityGroup and confidenceLevel) {
          redirectIfInsufficientConfidence() orElse constructEnroledUser()
        }(hc, executionContext) recoverWith logAndRedirect
    }
  }

  // this URL is incorrect in live - the completion and failure URLs must be URL encoded
  val ivUpliftRedirectUrl: String = {
    val host = if (appConfig.relativeIVUpliftParams) "" else appConfig.itvcFrontendEnvironment
    val completionUrl: String = s"$host${controllers.routes.UpliftSuccessController.success(OriginEnum.PTA.toString).url}"
    val failureUrl: String = s"$host${controllers.errors.routes.UpliftFailedController.show.url}"
    s"${appConfig.ivUrl}/uplift?origin=ITVC&confidenceLevel=$requiredConfidenceLevel&completionURL=${URLEncoder.encode(completionUrl, "UTF-8")}&failureURL=${URLEncoder.encode(failureUrl, "UTF-8")}"
  }

  type AuthRetrievals =
    Enrolments ~ Option[Name] ~ Option[Credentials] ~ Option[AffinityGroup] ~ ConfidenceLevel

  private def logAndRedirect[A]: PartialFunction[Throwable, Future[Either[Result, EnroledUser[A]]]] = {
    case insufficientEnrolments: InsufficientEnrolments =>
      Logger(getClass).debug(s"Insufficient enrolments: ${insufficientEnrolments.msg}")
      Future.successful(Left(Redirect(controllers.errors.routes.NotEnrolledController.show)))
    case _: BearerTokenExpired =>
      Logger(getClass).debug("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout)))
    case authorisationException: AuthorisationException =>
      Logger(getClass).debug(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(controllers.routes.SignInController.signIn)))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }

  private def redirectIfInsufficientConfidence[A]()(
    implicit request: Request[A],
    hc: HeaderCarrier): PartialFunction[AuthRetrievals, Future[Either[Result, EnroledUser[A]]]] = {

    case _ ~ _ ~ _ ~ Some(ag@(Organisation | Individual)) ~ confidenceLevel
      if confidenceLevel.level < requiredConfidenceLevel =>
      auditingService.audit(IvUpliftRequiredAuditModel(ag.toString, confidenceLevel.level, requiredConfidenceLevel), Some(request.path))
      Future.successful(Left(Redirect(ivUpliftRedirectUrl)))

    // No support in original code for agent to uplift confidence?
    case _ ~ _ ~ _ ~ _ ~ confidenceLevel
      if confidenceLevel.level < requiredConfidenceLevel =>
      throw UnsupportedAuthProvider()
  }

  private def constructEnroledUser[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, EnroledUser[A]]]] = {
    case enrolments ~ userName ~ credentials ~ affinityGroup ~ confidenceLevel =>
      Future.successful(
        Right(EnroledUser(
          enrolments = enrolments,
          userName = userName,
          affinityGroup = affinityGroup,
          confidenceLevel = confidenceLevel,
          credentials = credentials)))
  }
}