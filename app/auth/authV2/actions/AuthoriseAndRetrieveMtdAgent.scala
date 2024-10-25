
package auth.authV2.actions

import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.agent.AuthUtils._
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

case class AuthoriseAndRetrieveMtdAgent @Inject()(authorisedFunctions: FrontendAuthorisedFunctions,
                                                  appConfig: FrontendAppConfig,
                                                  override val config: Configuration,
                                                  override val env: Environment,
                                                  mcc: MessagesControllerComponents)
  extends AuthRedirects with ActionRefiner[ClientDataRequest, MtdItUserOptionNino] with FeatureSwitching {

  lazy val logger: Logger = Logger(getClass)

  implicit val executionContext: ExecutionContext = mcc.executionContext

  override protected def refine[A](request: ClientDataRequest[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val req: ClientDataRequest[A] = request

    val isAgent: Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent

    val hasDelegatedEnrolment: Predicate = if (request.isSupportingAgent) {
      Enrolment(
        key = secondaryAgentEnrolmentName,
        identifiers = Seq(EnrolmentIdentifier(agentIdentifier, request.clientMTDID)),
        state = "Activated",
        delegatedAuthRule = Some(secondaryAgentAuthRule)
      )
    } else {
      Enrolment(
        key = primaryAgentEnrolmentName,
        identifiers = Seq(EnrolmentIdentifier(agentIdentifier, request.clientMTDID)),
        state = "Activated",
        delegatedAuthRule = Some(primaryAgentAuthRule)
      )
    }

    authorisedFunctions.authorised(isAgent and hasDelegatedEnrolment)
      .retrieve(allEnrolments and credentials and affinityGroup and name) {
        constructMtdIdUserOptNino()
      }(hc, executionContext) recoverWith logAndRedirect
  }

  def logAndRedirect[A]: PartialFunction[Throwable, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case _: BearerTokenExpired =>
      logger.debug("Bearer Token Timed Out.")
      Future.successful(Left(Redirect(controllers.timeout.routes.SessionTimeoutController.timeout)))
    case _: InsufficientEnrolments =>
      logger.debug(s"missing agent reference. Redirect to agent error page.")
      Future.successful(Left(Redirect(controllers.agent.errors.routes.AgentErrorController.show)))
    case authorisationException: AuthorisationException =>
      logger.debug(s"Unauthorised request: ${authorisationException.reason}. Redirect to Sign In.")
      Future.successful(Left(Redirect(controllers.routes.SignInController.signIn)))
    // No catch all block at end - bubble up to global error handler
    // See investigation: https://github.com/hmrc/income-tax-view-change-frontend/pull/2432
  }

  private type AuthAgentWithNinoRetrievals =
    Enrolments ~ Option[Credentials] ~ Option[AffinityGroup] ~ Option[Name]

  private def constructMtdIdUserOptNino[A]()(
    implicit request: ClientDataRequest[A]): PartialFunction[AuthAgentWithNinoRetrievals, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case enrolments ~ credentials ~ affinityGroup ~ name =>
      Future.successful(
        Right(MtdItUserOptionNino(
          mtditid = request.clientMTDID,
          nino = Some(request.clientNino),
          userName = name,
          btaNavPartial = None,
          saUtr = Some(request.clientUTR),
          credId = credentials.map(_.providerId),
          userType = affinityGroup.map(ag => (ag.toJson \ "affinityGroup").as[AffinityGroup]),
          arn = enrolments.getEnrolment(agentEnrolmentName).flatMap(_.getIdentifier(arnIdentifier)).map(_.value),
          optClientName = request.clientName,
          isSupportingAgent = request.isSupportingAgent
        ))
      )
  }
}

