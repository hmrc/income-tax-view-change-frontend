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
import auth.authV2.AuthExceptions.MissingMtdId
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.predicates.agent.Constants
import models.OriginEnum
import play.api.mvc.Results.Redirect
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
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

class AuthoriseAndRetrieveIndividual @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                               val appConfig: FrontendAppConfig,
                                               override val config: Configuration,
                                               override val env: Environment,
                                               mcc: MessagesControllerComponents,
                                               val auditingService: AuditingService)
  extends AuthoriseHelper with ActionRefiner[Request, MtdItUserOptionNino]{

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val requiredConfidenceLevel: Int = appConfig.requiredConfidenceLevel

  lazy val logger = Logger(getClass)

  override protected def refine[A](request: Request[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val req: Request[A] = request

    // authorise on HMRC-MTD-IT enrolment and Individual / Organisation affinity group
    val predicate: Predicate =
      Enrolment(appConfig.mtdItEnrolmentKey) and
        (AffinityGroup.Organisation or AffinityGroup.Individual)

    authorisedFunctions.authorised(predicate or AffinityGroup.Agent)
      .retrieve(allEnrolments and name and credentials and affinityGroup and confidenceLevel) {
        redirectIfAgent() orElse
        redirectIfInsufficientConfidence() orElse constructMtdItUserOptionNino()
      }(hc, executionContext) recoverWith logAndRedirect
  }

  // this URL is incorrect in live - the completion and failure URLs must be URL encoded
  lazy val ivUpliftRedirectUrl: String = {
    val host = if (appConfig.relativeIVUpliftParams) "" else appConfig.itvcFrontendEnvironment
    val completionUrl: String = s"$host${controllers.routes.UpliftSuccessController.success(OriginEnum.PTA.toString).url}"
    val failureUrl: String = s"$host${controllers.errors.routes.UpliftFailedController.show.url}"
    s"${appConfig.ivUrl}/uplift?origin=ITVC&confidenceLevel=$requiredConfidenceLevel&completionURL=${URLEncoder.encode(completionUrl, "UTF-8")}&failureURL=${URLEncoder.encode(failureUrl, "UTF-8")}"
  }

  private def redirectIfInsufficientConfidence[A]()(
    implicit request: Request[A],
    hc: HeaderCarrier): PartialFunction[AuthRetrievals, Future[Either[Result, MtdItUserOptionNino[A]]]] = {

    case _ ~ _ ~ _ ~ ag ~ confidenceLevel
      if confidenceLevel.level < requiredConfidenceLevel =>
      auditingService.audit(IvUpliftRequiredAuditModel(ag.toString, confidenceLevel.level, requiredConfidenceLevel), Some(request.path))
      Future.successful(Left(Redirect(ivUpliftRedirectUrl)))
  }

  private def constructMtdItUserOptionNino[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case enrolments ~ userName ~ credentials ~ affinityGroup ~ _ =>
      def getValueFromEnrolment(enrolment: String, identifier: String): Option[String] =
        enrolments.getEnrolment(enrolment)
          .flatMap(_.getIdentifier(identifier)).map(_.value)

      lazy val optMtdId: Option[String] =
        getValueFromEnrolment(Constants.mtdEnrolmentName, Constants.mtdEnrolmentIdentifierKey)

      optMtdId.fold(throw new MissingMtdId) {
        mtditid =>
          Future.successful(
            Right(
              MtdItUserOptionNino(
                mtditid = mtditid,
                nino = getValueFromEnrolment(Constants.ninoEnrolmentName, Constants.ninoEnrolmentIdentifierKey),
                userName = userName,
                saUtr = getValueFromEnrolment(Constants.saEnrolmentName, Constants.saEnrolmentIdentifierKey),
                credId = credentials.map(credential => credential.providerId),
                userType = affinityGroup,
                arn = None
              )
            )
          )
      }
  }
}