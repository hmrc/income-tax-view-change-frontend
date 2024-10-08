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

package connectors.itsastatus

import config.FrontendAppConfig
import connectors.RawResponseReads
import connectors.itsastatus.ITSAStatusUpdateConnectorModel._
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ITSAStatusUpdateConnector @Inject()(val httpClient: HttpClientV2, val appConfig: FrontendAppConfig)
                                         (implicit val ec: ExecutionContext) extends RawResponseReads {

  private val log = Logger("application")

  def optOut(taxYear: TaxYear, taxableEntityId: String)
            (implicit headerCarrier: HeaderCarrier): Future[ITSAStatusUpdateResponse] = {
    makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason)
  }

  def optIn(taxYear: TaxYear, taxableEntityId: String)
           (implicit headerCarrier: HeaderCarrier): Future[ITSAStatusUpdateResponse] = {
    makeITSAStatusUpdate(taxYear, taxableEntityId, optInUpdateReason)
  }

  private def buildRequestUrlWith(taxableEntityId: String): String =
    s"${appConfig.itvcProtectedService}/income-tax-view-change/itsa-status/update/$taxableEntityId"

  private[connectors] def updateITSAStatus(taxableEntityId: String, requestBody: ITSAStatusUpdateRequest)
                                          (implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {

    httpClient
      .put(url"${buildRequestUrlWith(taxableEntityId)}")
      .withBody(Json.toJson(requestBody))
      .execute[HttpResponse]
  }

  def makeITSAStatusUpdate(taxYear: TaxYear, taxableEntityId: String, updateReason: String)
                          (implicit headerCarrier: HeaderCarrier): Future[ITSAStatusUpdateResponse] = {

    val body = ITSAStatusUpdateRequest(taxYear = taxYear.shortenTaxYearEnd, updateReason = updateReason)

    updateITSAStatus(taxableEntityId, requestBody = body)
      .map { response =>
        response.status match {
          case Status.NO_CONTENT => ITSAStatusUpdateResponseSuccess()
          case _ =>
            response.json.validate[ITSAStatusUpdateResponseFailure].fold(
              invalid => {
                log.error(s"Json validation error parsing itsa-status update response, error $invalid")
                ITSAStatusUpdateResponseFailure.defaultFailure(s"json response: $invalid")
              },
              valid => {
                val message =
                  valid.failures.headOption
                    .map(failure => s"code: ${failure.code}, reason: ${failure.reason}")
                    .getOrElse("unknown reason")

                log.error(s"response status: ${response.status}, message: $message")
                valid
              }
            )
        }
      }
  }
}