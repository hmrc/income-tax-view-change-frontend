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

package connectors

import config.FrontendAppConfig
import models.core.{SelfServeTimeToPayJourneyErrorResponse, SelfServeTimeToPayJourneyResponse, SelfServeTimeToPayJourneyResponseModel}
import play.api.Logger
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelfServeTimeToPayConnector @Inject()(http: HttpClientV2,
                                            config: FrontendAppConfig,
                                           )(implicit ec: ExecutionContext) {
  val journeyStartUrl: String = config.setUpAPaymentPlanUrl + "/essttp-backend/sa/itsa/journey/start"

  def startSelfServeTimeToPayJourney()(implicit hc: HeaderCarrier): Future[SelfServeTimeToPayJourneyResponse] = {
    val body = Json.parse(
      s"""
       {
        "returnUrl": "/report-quarterly/income-and-expenses/view",
        "backUrl": "/report-quarterly/income-and-expenses/view/your-self-assessment-charges"
       }
      """.stripMargin
    )
    http
      .post(url"$journeyStartUrl")
      .withBody(body)
      .execute[HttpResponse]
      .map {
        case response if response.status == CREATED =>
          response.json.validate[SelfServeTimeToPayJourneyResponseModel].fold(
            invalid => {
              Logger("application").error(s"Invalid Json with $invalid")
              SelfServeTimeToPayJourneyErrorResponse(response.status, "Invalid Json")
            },
            valid => valid
          )
        case response => if (response.status >= 400) {
          Logger("application").error(s"Self Serve Time To Pay journey start error with response code: ${response.status} and body: ${response.body}")
        } else {
          Logger("application").warn(s"Self Serve Time To Pay journey start error with response code: ${response.status} and body: ${response.body}")
        }
          SelfServeTimeToPayJourneyErrorResponse(response.status, response.body)
      }
  }
}
