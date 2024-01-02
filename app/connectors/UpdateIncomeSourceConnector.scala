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
import models.core.IncomeSourceId
import models.updateIncomeSource._
import play.api.Logger
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateIncomeSourceConnector @Inject()(val http: HttpClient,
                                            val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) extends RawResponseReads {
  def getUpdateIncomeSourceUrl: String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/update-income-source"
  }

  def updateCessationDate(nino: String, incomeSourceId: String, cessationDate: Option[LocalDate])(
    implicit headerCarrier: HeaderCarrier): Future[UpdateIncomeSourceResponse] = {
    val body = UpdateIncomeSourceRequestModel(nino = nino, incomeSourceID = incomeSourceId,
      cessation = Some(Cessation(cessationIndicator = true, cessationDate = cessationDate)))

    http.PUT[UpdateIncomeSourceRequestModel, HttpResponse](
      getUpdateIncomeSourceUrl,
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[UpdateIncomeSourceResponseModel].fold(
          invalid => {
            Logger("application").error("[IncomeTaxViewChangeConnector][updateCessationDate] - " +
              s"Json validation error parsing update income source response, error $invalid")
            UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
          },
          valid => valid
        )
        case _ =>
          response.json.validate[UpdateIncomeSourceResponseError].fold(
            invalid => {
              Logger("application").error("[IncomeTaxViewChangeConnector][updateCessationDate] - " +
                s"Json validation error parsing update income source response, error $invalid")
              UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
            },
            valid => valid
          )
      }
    }
  }

  def updateIncomeSourceTaxYearSpecific(nino: String, incomeSourceId: String, taxYearSpecific: TaxYearSpecific)
                                       (implicit headerCarrier: HeaderCarrier): Future[UpdateIncomeSourceResponse] = {
    val body = UpdateIncomeSourceRequestModel(nino = nino, incomeSourceID = incomeSourceId, taxYearSpecific = Some(taxYearSpecific))

    http.PUT[UpdateIncomeSourceRequestModel, HttpResponse](
      getUpdateIncomeSourceUrl,
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[UpdateIncomeSourceResponseModel].fold(
          invalid => {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateIncomeSourceTaxYearSpecific] - Json validation error parsing repayment response, error $invalid")
            UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
          },
          valid => valid
        )
        case _ =>
          response.json.validate[UpdateIncomeSourceResponseError].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][updateIncomeSourceTaxYearSpecific] - Json validation error parsing repayment response, error $invalid")
              UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Json validation error parsing response")
            },
            valid => valid
          )
      }
    }
  }
}
