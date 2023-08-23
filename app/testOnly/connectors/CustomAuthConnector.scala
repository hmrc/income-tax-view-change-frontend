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

import play.api.Logger
import play.api.http.Status.{CREATED, TOO_MANY_REQUESTS}
import play.api.libs.json._
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, TooManyRequestException}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.HttpReads.Implicits._

import javax.inject.{Inject, Singleton}
import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import testOnly.models.Nino
import testOnly.utils.FileUtil._
import testOnly.utils.LoginUtil._
import testOnly.utils.UserRepository

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
                                    val userRepository: UserRepository,
                                    val http: HttpClient) extends PlayAuthConnector {
  override val serviceUrl: String = servicesConfig.baseUrl("auth-login")

  def login(nino: Nino, isAgent: Boolean)(implicit hc: HeaderCarrier): Future[(AuthExchange, GovernmentGatewayToken)] = {
    createPayload(nino, isAgent) flatMap {
      payload => loginRequest(payload)
    }
  }

  def loginRequest(payload: JsValue)(implicit hc: HeaderCarrier): Future[(AuthExchange, GovernmentGatewayToken)] = {

    def getHeader(headerName: String, headers: Map[String, Seq[String]]): Option[String] = {
      val headerNames = List(headerName, headerName.take(1).toUpperCase() + headerName.drop(1))
      headerNames.flatMap(name => headers.get(name)).headOption.map(_.mkString)
    }

    http.POST[JsValue, HttpResponse](s"$serviceUrl/government-gateway/session/login", payload) flatMap {
      case response@HttpResponse(CREATED, _, headers) =>
        (
          getHeader("authorization", headers),
          getHeader("location", headers),
          (response.json \ "gatewayToken").asOpt[String]
        ) match {
          case (Some(token), Some(sessionUri), Some(receivedGatewayToken)) =>
            Future.successful((AuthExchange(token, sessionUri), GovernmentGatewayToken(receivedGatewayToken)))
          case (token, sessionUri, gatewayToken) =>
            Logger("application").info("HEADERS: " + headers)
            Logger("application").info("response json:" + response.json)
            Logger("application").info(s"login response info: $token :: $sessionUri :: $gatewayToken")
            Future.failed(new RuntimeException("Internal Error, missing headers or gatewayToken in response from auth-login-api"))
        }
      case response@HttpResponse(TOO_MANY_REQUESTS, _, _) =>
        Future.failed(new TooManyRequestException(s"response from $serviceUrl/government-gateway/session/login was ${response.status}. Body ${response.body}"))
      case response =>
        Future.failed(new RuntimeException(s"response from $serviceUrl/government-gateway/session/login was ${response.status}. Body ${response.body}"))
    }
  }

  private def createPayload(nino: Nino, isAgent: Boolean): Future[JsValue] = {
    getUserCredentials(nino.nino, userRepository) map {
      userCredentials =>
        val delegateEnrolments = getDelegatedEnrolmentData(isAgent = isAgent, userCredentials.enrolmentData)
        Json.obj(
          "credId" -> userCredentials.credId,
          "affinityGroup" -> {
            if (isAgent) "Agent" else {
              "Individual"
            }
          },
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

    }
  }

  private def removeEmptyValues(fields: (String, Option[String])*): JsObject = {
    val onlyDefinedFields = fields
      .collect {
        case (key, Some(value)) => key -> Json.toJsFieldJsValueWrapper(value)
      }
    Json.obj(onlyDefinedFields: _*)
  }

}