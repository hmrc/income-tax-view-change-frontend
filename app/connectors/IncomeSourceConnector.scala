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

import audit.AuditingService
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import models.addIncomeSource.{AddIncomeSourceResponse, _}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import play.api.Logger

class IncomeSourceConnectorImpl @Inject()(val http: HttpClient,
                                          val appConfig: FrontendAppConfig
                                         )(implicit val ec: ExecutionContext) extends RawResponseReads with FeatureSwitching {
}

trait IncomeSourceConnector {
  val http: HttpClient
  val appConfig: FrontendAppConfig
  implicit val ec: ExecutionContext

  private def addBusinessDetailsUrl(authTag: String): String = s"${appConfig.itvcProtectedService}/income-tax-view-change/create-income-source/business/$authTag"

  def create(businessDetails: BusinessDetails)(implicit headerCarrier: HeaderCarrier): Future[AddIncomeSourceResponse] = {
    val body = AddBusinessIncomeSourcesRequest(businessDetails = Some(
      List(businessDetails)
    ))

    http.POST[AddBusinessIncomeSourcesRequest, HttpResponse](
      addBusinessDetailsUrl(""),
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[AddIncomeSourceSuccessResponse].fold(
          invalid => {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateCessationDate] - Json validation error parsing repayment response, error $invalid")
            AddIncomeSourceResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing response")
          },
          valid => valid
        )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][updateCessationDate] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][updateCessationDate] - Response status: ${response.status}, body: ${response.body}")
          }
          AddIncomeSourceResponseError(response.status, response.body)
      }
    }
  }

}