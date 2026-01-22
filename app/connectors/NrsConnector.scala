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
import models.nrs.NrsSubmissionFailure.{NrsErrorResponse, NrsExceptionThrown}
import models.nrs.NrsSubmissionResponse.NrsSubmissionResponse
import models.nrs.{NrsSubmission, NrsSuccessResponse}
import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.Status
import play.api.http.Status.{ACCEPTED, TOO_MANY_REQUESTS}
import play.api.libs.ws.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class NrsConnector @Inject()(http: HttpClientV2, appConfig: FrontendAppConfig)(
  implicit val ec: ExecutionContext) extends RawResponseReads with Logging {

  private val nrsOrchestratorSubmissionUrl: String = s"${appConfig.nrsBaseUrl}/nrs-orchestrator/submission"
  private val apiKey: String = appConfig.nrsApiKey
  private val numberOfRetries: Int = appConfig.nrsRetries
  private val CLIENT_CLOSED_REQUEST = 499

  private def shouldRetry(response: HttpResponse): Boolean =
    Status.isServerError(response.status) ||
      Seq(TOO_MANY_REQUESTS, CLIENT_CLOSED_REQUEST).contains(response.status)

  def submit(nrsSubmission: NrsSubmission, remainingAttempts: Int = numberOfRetries)
            (implicit headerCarrier: HeaderCarrier): Future[NrsSubmissionResponse] = {
    http
      .post(url"$nrsOrchestratorSubmissionUrl")
      .withBody(Json.toJson(nrsSubmission))
      .setHeader("X-API-Key" -> apiKey)
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == ACCEPTED =>
          logger.info("NRS submission successful")
          Future.successful(Right(response.json.as[NrsSuccessResponse]))

        case response if shouldRetry(response) && remainingAttempts > 0 =>
          logger.warn(s"NRS submission retry due to status: ${response.status}, body: ${response.body}")
          submit(nrsSubmission, remainingAttempts = remainingAttempts - 1)

        case response =>
          logger.info(s"NRS submission failed with status: ${response.status}, details: ${response.body}")
          Future.successful(Left(NrsErrorResponse(response.status)))
      }
      .recover {
        case NonFatal(e) =>
          logger.info(s"NRS submission failed with exception: $e")
          Left(NrsExceptionThrown)
      }
  }
}
