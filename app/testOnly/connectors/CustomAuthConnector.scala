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

package testOnly.connectors


import play.api.http.HeaderNames
import play.api.http.Status.{CREATED, TOO_MANY_REQUESTS}
import play.api.libs.json._
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, TooManyRequestException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import testOnly.models.Nino
import testOnly.utils.FileUtil._
import testOnly.utils.LoginUtil._
case class EnrolmentData(name: String, state: String, taxIdentifier: scala.Seq[TaxIdentifierData])

case class TaxIdentifierData(key: String, value: String)
case class DelegatedEnrolmentData(key: String, taxIdentifier: Seq[TaxIdentifierData], delegatedAuthRule: String)

case class GovernmentGatewayToken(gatewayToken: String)

object GovernmentGatewayToken {
  implicit val reads: Reads[GovernmentGatewayToken] = {
    Json.reads[GovernmentGatewayToken]
  }
}

case class AuthExchange(bearerToken: String, sessionAuthorityUri: String)

@Singleton
class CustomAuthConnector @Inject()(servicesConfig: ServicesConfig,
                                    val http: HttpClient) extends PlayAuthConnector {
  override val serviceUrl: String = servicesConfig.baseUrl("auth-login")


  def login(nino: Nino, isAgent: Boolean)(implicit hc: HeaderCarrier): Future[(AuthExchange, GovernmentGatewayToken)] = {
    createPayload(nino, isAgent) match {
      case Left(ex) =>
        Future.failed(new RuntimeException(s"Internal Error: unable to create a payload: $ex"))
      case Right(payload) =>
        loginRequest(payload)
    }
  }

  def loginRequest(payload: JsValue)(implicit hc: HeaderCarrier): Future[(AuthExchange, GovernmentGatewayToken)] = {
    http.POST[JsValue, HttpResponse](s"$serviceUrl/government-gateway/session/login", payload) flatMap {
      case response@HttpResponse(CREATED, _, _) =>
        (
          response.header(HeaderNames.AUTHORIZATION),
          response.header(HeaderNames.LOCATION),
          (response.json \ "gatewayToken").asOpt[String]
        ) match {
          case (Some(token), Some(sessionUri), Some(receivedGatewayToken)) =>
            Future.successful((AuthExchange(token, sessionUri), GovernmentGatewayToken(receivedGatewayToken)))
          case _ => Future.failed(new RuntimeException("Internal Error, missing headers or gatewayToken in response from auth-login-api"))
        }
      case response@HttpResponse(TOO_MANY_REQUESTS, _, _) =>
        Future.failed(new TooManyRequestException(s"response from $serviceUrl/government-gateway/session/login was ${response.status}. Body ${response.body}"))
      case response =>
        Future.failed(new RuntimeException(s"response from $serviceUrl/government-gateway/session/login was ${response.status}. Body ${response.body}"))
    }
  }

  private def createPayload(nino: Nino, isAgent: Boolean): Either[Throwable, JsValue] = {
    getUserCredentials(nino.nino) match {
      case Left(ex) => Left(ex)
      case Right(userCredentials) =>
        val delegateEnrolments = getDelegatedEnrolmentData(isAgent = isAgent, userCredentials.enrolmentData)
        Right(
          Json.obj(
            "credId" -> userCredentials.credId,
            "affinityGroup" -> { if (isAgent) "Agent" else { "Individual"} } ,
            "confidenceLevel" -> userCredentials.confidenceLevel,
            "credentialStrength" -> userCredentials.credentialStrength,
            "credentialRole" -> userCredentials.Role,
            "usersName" -> "usersName",
            "enrolments" -> getEnrolmentData(isAgent = isAgent, userCredentials.enrolmentData),
            "delegatedEnrolments" -> delegateEnrolments
          ) ++ {
            if (isAgent) {
              removeEmptyValues(
                "email" -> Some("user@test.com")
              ) ++
                Json.obj(
                  "gatewayInformation" -> JsObject.empty)

            } else {
              removeEmptyValues(
                "nino" -> Some(nino.value),
                "groupIdentifier" -> Some("groupIdentifier"),
                "gatewayToken" -> Some("gatewayToken"),
                "agentId" -> Some("agentId"),
                "agentCode" -> Some("agentCode"),
                "agentFriendlyName" -> Some("agentFriendlyName"),
                "email" -> Some("email")
              )
            }
          }
        )
    }
  }

//  private def delegatedEnrolmentsJson(delegatedEnrolments: Seq[DelegatedEnrolmentData]) =
//    delegatedEnrolments
//      .filterNot(invalidDelegatedEnrolment)
//      .filter(validateDelegatedEnrolmentIdentifiers)
//      .map(toJson)

  private def invalidDelegatedEnrolment(delegatedEnrolment: DelegatedEnrolmentData) =
    delegatedEnrolment.key.isEmpty || delegatedEnrolment.delegatedAuthRule.isEmpty

  private def validateDelegatedEnrolmentIdentifiers(delegatedEnrolment: DelegatedEnrolmentData) =
    delegatedEnrolment.taxIdentifier.forall(taxId => !(taxId.key.isEmpty || taxId.value.isEmpty))

  private def toJson(enrolment: DelegatedEnrolmentData): JsObject =
    Json.obj(
      "key" -> enrolment.key,
      "identifiers" -> enrolment.taxIdentifier.map(taxId => Json.obj(
        "key" -> taxId.key,
        "value" -> taxId.value
      )),
      "delegatedAuthRule" -> enrolment.delegatedAuthRule
    )

  private def toJson(enrolment: EnrolmentData): JsObject =
    Json.obj(
      "key" -> enrolment.name,
      "identifiers" -> enrolment.taxIdentifier.map(taxId => Json.obj(
        "key" -> taxId.key,
        "value" -> taxId.value
      )),
      "state" -> enrolment.state
    )

  private def toJson[A: Writes](optData: Option[A], key: String): JsObject =
    optData.map(data => Json.obj(key -> Json.toJson(data))).getOrElse(Json.obj())

  private def removeEmptyValues(fields: (String, Option[String])*): JsObject = {
    val onlyDefinedFields = fields
      .collect {
        case (key, Some(value)) => key -> Json.toJsFieldJsValueWrapper(value)
      }
    Json.obj(onlyDefinedFields: _*)
  }

  private def toJson(gatewayToken: Option[String]): JsObject = {
    val ggToken = gatewayToken.map(token => "gatewayToken" -> Json.toJsFieldJsValueWrapper(token))
    Json.obj("gatewayInformation" -> Json.obj(scala.Seq(ggToken).flatten: _*))
  }

}