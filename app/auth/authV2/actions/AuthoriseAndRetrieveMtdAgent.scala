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

import auth.{FrontendAuthorisedFunctions, MtdItUserOptionNino}
import com.google.inject.Singleton
import config.FrontendAppConfig
import controllers.agent.AuthUtils._
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import play.api.{Configuration, Environment, Logger}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuthoriseAndRetrieveMtdAgent @Inject()(authorisedFunctions: FrontendAuthorisedFunctions,
                                             val appConfig: FrontendAppConfig,
                                                  mcc: MessagesControllerComponents)
  extends AuthoriseHelper with ActionRefiner[ClientDataRequest, MtdItUserOptionNino] {

  lazy val logger: Logger = Logger(getClass)

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val requiredConfidenceLevel: Int = appConfig.requiredConfidenceLevel


  override protected def refine[A](request: ClientDataRequest[A]): Future[Either[Result, MtdItUserOptionNino[A]]] = {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter
      .fromRequestAndSession(request, request.session)

    implicit val req: ClientDataRequest[A] = request

    val isAgent: Predicate = Enrolment("HMRC-AS-AGENT") and AffinityGroup.Agent
    val isNotAgent: Predicate = AffinityGroup.Individual or AffinityGroup.Organisation

    val hasDelegatedEnrolment: Predicate = if (request.isSupportingAgent) {
      Enrolment(
        key = secondaryAgentEnrolmentName,
        identifiers = Seq(EnrolmentIdentifier(agentIdentifier, request.clientMTDID)),
        state = "Activated",
        delegatedAuthRule = Some(secondaryAgentAuthRule)
      )
    } else {
      Enrolment(
        key = mtdEnrolmentName,
        identifiers = Seq(EnrolmentIdentifier(agentIdentifier, request.clientMTDID)),
        state = "Activated",
        delegatedAuthRule = Some(primaryAgentAuthRule)
      )
    }

    authorisedFunctions.authorised((isAgent and hasDelegatedEnrolment) or isNotAgent)
      .retrieve(allEnrolments and name and credentials and affinityGroup and confidenceLevel) {
        redirectIfNotAgent() orElse constructMtdIdUserOptNino()
      }(hc, executionContext) recoverWith logAndRedirect(true)
  }

  private def constructMtdIdUserOptNino[A]()(
    implicit request: ClientDataRequest[A]): PartialFunction[AuthRetrievals, Future[Either[Result, MtdItUserOptionNino[A]]]] = {
    case enrolments ~ userName ~ credentials ~ affinityGroup ~ _ =>
      Future.successful(
        Right(MtdItUserOptionNino(
          mtditid = request.clientMTDID,
          nino = Some(request.clientNino),
          userName = userName,
          btaNavPartial = None,
          saUtr = Some(request.clientUTR),
          credId = credentials.map(_.providerId),
          userType = affinityGroup.map(ag => (ag.toJson \ "affinityGroup").as[AffinityGroup]),
          arn = enrolments.getEnrolment(agentEnrolmentName).flatMap(_.getIdentifier(arnIdentifier)).map(_.value),
          optClientName = request.clientName,
          isSupportingAgent = request.isSupportingAgent,
          clientConfirmed = request.confirmed
        ))
      )
  }
}

