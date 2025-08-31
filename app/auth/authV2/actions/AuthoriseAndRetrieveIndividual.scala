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
import auth.authV2.Constants
import auth.authV2.models.{AuthUserDetails, AuthorisedAndEnrolledRequest}
import com.google.inject.Singleton
import config.FrontendAppConfig
import controllers.agent.AuthUtils.mtdEnrolmentName
import enums.MTDIndividual
import forms.utils.SessionKeys
import play.api.Logger
import play.api.mvc.Results.Redirect
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import java.net.URLEncoder
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthoriseAndRetrieveIndividual @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                               val appConfig: FrontendAppConfig,
                                               mcc: MessagesControllerComponents,
                                               val auditingService: AuditingService)
  extends AuthoriseHelper with ActionRefiner[Request, AuthorisedAndEnrolledRequest]{

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val requiredConfidenceLevel: Int = appConfig.requiredConfidenceLevel

  lazy val logger = Logger(getClass)

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedAndEnrolledRequest[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val req: Request[A] = request

    // authorise on HMRC-MTD-IT enrolment and Individual / Organisation affinity group
    val predicate: Predicate =
      Enrolment(mtdEnrolmentName) and
        (AffinityGroup.Organisation or AffinityGroup.Individual)

    authorisedFunctions.authorised(AffinityGroup.Agent or predicate)
      .retrieve(allEnrolments and name and credentials and affinityGroup and confidenceLevel) {
        redirectIfAgent() orElse
        redirectIfInsufficientConfidence() orElse constructAuthorisedAndEnrolledUser()
      }(hc, executionContext) recoverWith logAndRedirect()
  }

  // this URL is incorrect in live - the completion and failure URLs must be URL encoded
  def ivUpliftRedirectUrl[A](implicit request: Request[A]):String = {
    val host = if (appConfig.relativeIVUpliftParams) "" else appConfig.itvcFrontendEnvironment
    val origin = request.getQueryString(SessionKeys.origin)
    val completionUrl: String = s"$host${controllers.routes.UpliftSuccessController.success(origin).url}"
    val failureUrl: String = s"$host${controllers.errors.routes.UpliftFailedController.show().url}"
    s"${appConfig.ivUrl}/uplift?origin=ITVC&confidenceLevel=$requiredConfidenceLevel&completionURL=${URLEncoder.encode(completionUrl, "UTF-8")}&failureURL=${URLEncoder.encode(failureUrl, "UTF-8")}"
  }

  private def redirectIfInsufficientConfidence[A]()(
    implicit request: Request[A],
    hc: HeaderCarrier): PartialFunction[AuthRetrievals, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {

    case _ ~ _ ~ _ ~ ag ~ confidenceLevel
      if confidenceLevel.level < requiredConfidenceLevel =>
      auditingService.audit(IvUpliftRequiredAuditModel(ag.fold("")(_.toString), confidenceLevel.level, requiredConfidenceLevel), Some(request.path))
      Future.successful(Left(Redirect(ivUpliftRedirectUrl)))
  }

  private def constructAuthorisedAndEnrolledUser[A]()(
    implicit request: Request[A]): PartialFunction[AuthRetrievals, Future[Either[Result, AuthorisedAndEnrolledRequest[A]]]] = {
    case enrolments ~ userName ~ credentials ~ affinityGroup ~ confidenceLevel =>
      lazy val optMtdId: Option[String] =
        enrolments.getEnrolment(Constants.mtdEnrolmentName)
          .flatMap(_.getIdentifier(Constants.mtdEnrolmentIdentifierKey))
          .map(_.value)

      optMtdId.fold(throw InsufficientEnrolments("Missing MTDId Individual")) {
        mtdItId =>
          val authUserDetails = AuthUserDetails(
            enrolments,
            affinityGroup,
            credentials,
            userName,
            confidenceLevel
          )
          Future.successful(
            Right(
              AuthorisedAndEnrolledRequest(
                mtditId = mtdItId,
                MTDIndividual,
                authUserDetails = authUserDetails,
                clientDetails = None
              )
            )
          )
      }
  }
}