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
import models.nrs.NrsSubmissionResponse.NrsSubmissionResponse
import models.nrs.{NrsSubmissionFailure, NrsSuccessResponse}
import org.apache.pekko.actor.Scheduler
import play.api.Logging
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.Status
import play.api.http.Status.ACCEPTED
import utils.{Delayer, Retrying}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import scala.util.control.NonFatal

@Singleton
class NrsConnector @Inject()(http: HttpClientV2, appConfig: FrontendAppConfig)(
  implicit val scheduler: Scheduler,
  val hc: HeaderCarrier,
  val ec: ExecutionContext) extends RawResponseReads with Logging with Delayer with Retrying {

  private val nrsOrchestratorSubmissionUrl: String = s"${appConfig.nrsBaseUrl}/nrs-orchestrator/submission"
  private val apiKey: String = appConfig.nrsApiKey

  val retryCondition: Try[NrsSubmissionResponse] => Boolean = {
    case Success(Left(failure)) => failure.retryable
    case _                      => false
  }

  def submit(nrsSubmission: JsValue): Future[NrsSubmissionResponse] = {
    retry(appConfig.nrsRetries, retryCondition) { attemptNumber =>

      logger.info(s"NRS submission attempt number: $attemptNumber to POST request: $nrsOrchestratorSubmissionUrl")

      http
        .post(url"$nrsOrchestratorSubmissionUrl")
        .withBody(nrsSubmission)
        .setHeader("X-API-Key" -> apiKey)
        .execute[HttpResponse]
        .map {
          case response if response.status == ACCEPTED =>
            logger.info("NRS submission successful")
            Right(response.json.as[NrsSuccessResponse])
          case response =>
            logger.info(s"NRS submission failed with status ${response.status}")
            Left(NrsSubmissionFailure.ErrorResponse(response.status))
        }
        .recover {
          case NonFatal(e) =>
            logger.info(s"NRS submission failed with exception: $e")
            Left(NrsSubmissionFailure.ExceptionThrown)
        }
    }
  }
}
