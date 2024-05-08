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
import connectors.OptOutConnector.CorrelationIdHeader
import models.incomeSourceDetails.TaxYear
import models.optOut.OptOutUpdateRequestModel._
import play.api.Logger
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object OptOutConnector {
  val CorrelationIdHeader = "CorrelationId"
}

@Singleton
class OptOutConnector @Inject()(val http: HttpClient, val appConfig: FrontendAppConfig)
                               (implicit val ec: ExecutionContext) extends RawResponseReads {

  private val log = Logger("application")


  def getUrl(taxableEntityId: String): String =
    s"${appConfig.itvcProtectedService}/income-tax/itsa-status/update/$taxableEntityId"

  def requestOptOutForTaxYear(taxYear: TaxYear, taxableEntityId: String)
                             (implicit headerCarrier: HeaderCarrier): Future[OptOutUpdateResponse] = {

    val body = OptOutUpdateRequest(taxYear = taxYear.toString)

    http.PUT[OptOutUpdateRequest, HttpResponse](
      getUrl(taxableEntityId), body, Seq[(String, String)]()
    ).map { response =>
      response.status match {
        case Status.NO_CONTENT =>
          response.json.validate[OptOutUpdateResponseSuccess].fold(
          invalid => {
            log.error(s"Json validation error parsing update income source response, error $invalid")
            OptOutUpdateResponseFailure(response.status, List(ErrorItem("INTERNAL_SERVER_ERROR", "Json validation error parsing response")))
          },
          valid => {
            valid.copy(
              statusCode = response.status,
              correlationId = response.headers(CorrelationIdHeader).headOption.getOrElse(s"Unknown_$CorrelationIdHeader")
            )
          }
        )
        case _ =>
          response.json.validate[OptOutUpdateResponseFailure].fold(
            invalid => {
              log.error(s"Json validation error parsing update income source response, error $invalid")
              OptOutUpdateResponseFailure(response.status, List(ErrorItem("INTERNAL_SERVER_ERROR", "Json validation error parsing response")))
            },
            valid => valid
          )
      }
    }
  }
}
