/*
 * Copyright 2026 HM Revenue & Customs
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

package common.connectors

import common.config.FrontendAppConfig
import common.models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusYearOfMigrationModel}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps, HttpResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class YearOfMigrationConnector @Inject()(val http: HttpClientV2,
                                         val appConfig: FrontendAppConfig)
                                        (implicit val ec: ExecutionContext) {

  private def getYearOfMigrationUrl(nino: String) = {
    s"${appConfig.incomeTaxObligationsService}/income-tax-obligations/year-of-migration/$nino"
  }

  def getYearOfMigration(nino: String)(implicit headerCarrier: HeaderCarrier): Future[Either[ITSAStatusResponse, ITSAStatusYearOfMigrationModel]] = {
    val yearOfMigrationUrl = getYearOfMigrationUrl(nino)
    http.get(url"$yearOfMigrationUrl")
      .transform(_.addHttpHeaders("Accept" -> "application/vnd.hmrc.2.0+json"))
      .execute[HttpResponse] map { response =>
        response.status match {
          case OK =>
            response.json.validate[ITSAStatusYearOfMigrationModel].fold(
              invalid => {
                Logger("application").error(s"[YearOfMigrationConnector][getYearOfMigration] Json validation error parsing year of migration response, error $invalid")
                Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing year of migration response"))
              },
              valid => {
                Logger("application").debug(s"[YearOfMigrationConnector][getYearOfMigration] Get Year of Migration returned OK with valid response ${valid.toString}")
                Right(valid)
              }
            )
          case NOT_FOUND => Right(ITSAStatusYearOfMigrationModel(None))
          case status =>
            if (status >= INTERNAL_SERVER_ERROR) {
              Logger("application").error(s"[YearOfMigrationConnector][getYearOfMigration] Response status: ${response.status}, body: ${response.body}")
            } else {
              Logger("application").warn(s"[YearOfMigrationConnector][getYearOfMigration] Response status: ${response.status}, body: ${response.body}")
            }
            Left(ITSAStatusResponseError(response.status, response.body))
        }
      }
  }
}
