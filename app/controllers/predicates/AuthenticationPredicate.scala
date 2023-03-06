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

package controllers.predicates

import audit.AuditingService
import audit.models.IvUpliftRequiredAuditModel
import auth._
import config.featureswitch.{FeatureSwitching, IvUplift}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.BaseController
import models.OriginEnum
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AffinityGroup.{Individual, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name, ~}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthenticationPredicate @Inject()(implicit val ec: ExecutionContext,
                                        implicit val authorisedFunctions: FrontendAuthorisedFunctions,
                                        val appConfig: FrontendAppConfig,
                                        override val config: Configuration,
                                        override val env: Environment,
                                        val itvcErrorHandler: ItvcErrorHandler,
                                        mcc: MessagesControllerComponents,
                                        val auditingService: AuditingService,
                                        val headerExtractor: HeaderExtractor
                                       )
  extends BaseController with AuthRedirects with ActionBuilder[MtdItUserOptionNino, AnyContent]
    with ActionFunction[Request, MtdItUserOptionNino] with FeatureSwitching {

  override val parser: BodyParser[AnyContent] = mcc.parsers.defaultBodyParser
  override val executionContext: ExecutionContext = mcc.executionContext
  val requiredConfidenceLevel: Int = appConfig.requiredConfidenceLevel


  override def invokeBlock[A](request: Request[A], f: MtdItUserOptionNino[A] => Future[Result]): Future[Result] = {

    implicit val hc: HeaderCarrier = headerExtractor.extractHeader(request, request.session)

    implicit val req: Request[A] = request

    authorisedFunctions.authorised(Enrolment(appConfig.mtdItEnrolmentKey)).retrieve(allEnrolments and name and credentials and affinityGroup and confidenceLevel) {
      case enrolments ~ userName ~ credentials ~ affinityGroup ~ confidenceLevel => {
        if (confidenceLevel.level < requiredConfidenceLevel && isEnabled(IvUplift)) {
          affinityGroup match {
            case Some(Organisation) => {
              auditingService.audit(IvUpliftRequiredAuditModel("organisation", confidenceLevel.level, requiredConfidenceLevel), Some(request.path))
              Future.successful(Redirect(ivUpliftRedirectUrl))
            }
            case Some(Individual) => {
              auditingService.audit(IvUpliftRequiredAuditModel("individual", confidenceLevel.level, requiredConfidenceLevel), Some(request.path))
              Future.successful(Redirect(ivUpliftRedirectUrl))
            }
            case _ => throw UnsupportedAuthProvider()
          }
        } else f(buildMtdUserOptionNino(enrolments, userName, credentials, affinityGroup))
      }
    } recover {
      case _: InsufficientEnrolments =>
        Logger("application").info("[AuthenticationPredicate][async] No HMRC-MTD-IT Enrolment and/or No NINO.")
        Redirect(controllers.errors.routes.NotEnrolledController.show)
      case _: BearerTokenExpired =>
        Logger("application").info("[AuthenticationPredicate][async] Bearer Token Timed Out.")
        Redirect(controllers.timeout.routes.SessionTimeoutController.timeout)
      case _: AuthorisationException =>
        Logger("application").info("[AuthenticationPredicate][async] Unauthorised request. Redirect to Sign In.")
        Redirect(controllers.routes.SignInController.signIn)
      case s =>
        Logger("application").error(s"[AuthenticationPredicate][async] Unexpected Error Caught. Show ISE.\n$s\n", s)
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def buildMtdUserOptionNino[A](enrolments: Enrolments, userName: Option[Name],
                                        credentials: Option[Credentials], affinityGroup: Option[AffinityGroup])
                                       (implicit request: Request[A]): MtdItUserOptionNino[A] = {
    MtdItUserOptionNino(
      mtditid = enrolments.getEnrolment(appConfig.mtdItEnrolmentKey).flatMap(_.getIdentifier(appConfig.mtdItIdentifierKey)).map(_.value).get,
      nino = enrolments.getEnrolment(appConfig.ninoEnrolmentKey).flatMap(_.getIdentifier(appConfig.ninoIdentifierKey)).map(_.value),
      userName,
      saUtr = enrolments.getEnrolment(appConfig.saEnrolmentKey).flatMap(_.getIdentifier(appConfig.saIdentifierKey)).map(_.value),
      credId = credentials.map(credential => credential.providerId),
      userType = affinityGroup.map(ag => (ag.toJson \ "affinityGroup").as[AffinityGroup])
    )
  }

  val ivUpliftRedirectUrl: String = {
    val completionUrl: String = s"${appConfig.itvcFrontendEnvironment}/${appConfig.baseUrl}" +
      s"${controllers.routes.UpliftSuccessController.success(OriginEnum.PTA.toString).url}"
    val failureUrl: String = s"${appConfig.itvcFrontendEnvironment}/${appConfig.baseUrl}" +
      s"${controllers.errors.routes.UpliftFailedController.show.url}"
    s"${appConfig.ivUrl}/uplift?origin=ITVC&confidenceLevel=$requiredConfidenceLevel&completionURL=$completionUrl&failureURL=$failureUrl"
  }
}
