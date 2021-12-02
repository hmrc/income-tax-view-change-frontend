/*
 * Copyright 2021 HM Revenue & Customs
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

package connectors

import audit.AuditingService
import config.FrontendAppConfig
import play.api.Logger
import play.api.http.Status.{ACCEPTED, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RepaymentConnector @Inject()(val http: HttpClient,
                                   val auditingService: AuditingService,
                                   val config: FrontendAppConfig)(implicit ec: ExecutionContext) {

  val url: String = s"${config.repaymentUrl}/self-assessment-repayment-backend/start"

  // TODO is it right that here we returning Future[String] instead of Future[RepaymentResponse] ?
  def start(nino: String, fullAmount: BigDecimal)(implicit headerCarrier: HeaderCarrier): Future[String] = {

    val body = Json.parse(
      s"""
         |{
         | "nino": "$nino",
         | "fullAmount": $fullAmount
         |}
      """.stripMargin
    )

    http.POST(url, body).map {
      case response if response.status == ACCEPTED =>
        // TODO should we return it as a case class instead of just a String ?
        (Json.parse(response.body) \ "nextUrl").get.toString()

      case response =>
        if (response.status == UNAUTHORIZED) {
          Logger("application").error(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
        } else {
          Logger("application").warn(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
        }
        // TODO What do we return here if we are not returning business error response ?
        ???
    }
  }
}
