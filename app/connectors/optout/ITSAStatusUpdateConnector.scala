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

package connectors.optout

import config.FrontendAppConfig
import connectors.RawResponseReads
import connectors.optout.ITSAStatusUpdateConnector._
import models.incomeSourceDetails.TaxYear
import OptOutUpdateRequestModel._
import play.api.Logger
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

object ITSAStatusUpdateConnector {
  val CorrelationIdHeader = "CorrelationId"
  def toApiFormat(taxYear: TaxYear): String = {
    s"${taxYear.startYear}-${taxYear.endYear.toString.toSeq.drop(2)}"
  }
}

@Singleton
class ITSAStatusUpdateConnector @Inject()(val http: HttpClient, val appConfig: FrontendAppConfig)
                                         (implicit val ec: ExecutionContext) extends RawResponseReads {

  private val log = Logger("application")

  def buildRequestUrlWith(taxableEntityId: String): String =
    s"${appConfig.itvcProtectedService}/income-tax-view-change/itsa-status/update/$taxableEntityId"

  def requestOptOutForTaxYear(taxYear: TaxYear, taxableEntityId: String, updateReason: Int)
                             (implicit headerCarrier: HeaderCarrier): Future[OptOutUpdateResponse] = {

    val body = OptOutUpdateRequest(taxYear = toApiFormat(taxYear), updateReason = updateReason)

    http.PUT[OptOutUpdateRequest, HttpResponse](
      buildRequestUrlWith(taxableEntityId), body, Seq[(String, String)]()
    ).map { response =>
      val correlationId = response.headers.get(CorrelationIdHeader).map(_.head).getOrElse(s"Unknown_$CorrelationIdHeader")
      response.status match {
        case Status.NO_CONTENT => OptOutUpdateResponseSuccess(correlationId)
        case Status.NOT_FOUND =>
          log.error(s"response status: ${response.status}")
          OptOutUpdateResponseFailure.notFoundFailure(correlationId, buildRequestUrlWith(taxableEntityId))
        case _ =>
          response.json.validate[OptOutUpdateResponseFailure].fold(
            invalid => {
              log.error(s"Json validation error parsing itsa-status update response, error $invalid")
              OptOutUpdateResponseFailure.defaultFailure(correlationId)
            },
            valid => {
              log.error(s"response status: ${response.status}")
              valid.copy(correlationId = correlationId, statusCode = response.status)
            }
          )
      }
    }
  }
}