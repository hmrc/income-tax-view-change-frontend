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

import config.FrontendAppConfig
import models.core.RepaymentJourneyResponseModel
import models.core.RepaymentJourneyResponseModel.{RepaymentJourneyErrorResponse, RepaymentJourneyModel}
import play.api.Logger
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RepaymentConnector @Inject()(val http: HttpClient,
                                   val config: FrontendAppConfig
                                  )(implicit ec: ExecutionContext) {

  val url: String = s"${config.repaymentsUrl}/self-assessment-repayment-backend/start"

  def start(nino: String, fullAmount: BigDecimal)(implicit headerCarrier: HeaderCarrier): Future[RepaymentJourneyResponseModel] = {

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
        response.json.validate[RepaymentJourneyModel].fold(
          invalidJson => {
            Logger("application").error(s"Invalid Json with $invalidJson")
            RepaymentJourneyErrorResponse(response.status, "Invalid Json")
          },
          valid => valid
        )

      case response =>
        if (response.status >= INTERNAL_SERVER_ERROR) {
          Logger("application").error(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
        } else {
          Logger("application").warn(s"Repayment journey start error with response code: ${response.status} and body: ${response.body}")
        }
        RepaymentJourneyErrorResponse(response.status, response.body)
    }
  }
}
