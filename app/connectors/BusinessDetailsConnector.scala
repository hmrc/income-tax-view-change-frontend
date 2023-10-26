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
import audit.models.IncomeSourceDetailsResponseAuditModel
import auth.MtdItUserWithNino
import config.FrontendAppConfig
import models.core.{NinoResponse, NinoResponseError, NinoResponseSuccess}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import utils.Headers.checkAndAddTestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsConnector @Inject()(val http: HttpClient,
                                         val auditingService: AuditingService,
                                         val appConfig: FrontendAppConfig
                                           )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getBusinessDetailsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/get-business-details/nino/$nino"
  }

  def getIncomeSourcesUrl(mtditid: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/income-sources/$mtditid"
  }

  def getNinoLookupUrl(mtdRef: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"
  }



  def getBusinessDetails(nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {
    val url = getBusinessDetailsUrl(nino)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] $invalid")
              IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
            },
            valid => valid
          )
        case status =>
          if (status == 404) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getBusinessDetails] - Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getIncomeSources()(
    implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUserWithNino[_]): Future[IncomeSourceDetailsResponse] = {

    //Check and add test headers Gov-Test-Scenario for dynamic stub Income Sources Created Scenarios
    val hc = checkAndAddTestHeader(mtdItUser.path, headerCarrier, appConfig.incomeSourceOverrides())

    val url = getIncomeSourcesUrl(mtdItUser.mtditid)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getIncomeSources] - GET $url")

    //Passing the updated headercarrier implicitly to the request
    http.GET[HttpResponse](url)(implicitly, hc = hc, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] $invalid")
              IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
            },
            valid => {
              auditingService.extendedAudit(IncomeSourceDetailsResponseAuditModel(
                mtdItUser,
                valid.businesses.map(_.incomeSourceId),
                valid.properties.map(_.incomeSourceId),
                valid.yearOfMigration
              ))
              valid
            }
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getIncomeSources] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getIncomeSources] - Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getNino(mtdRef: String)(implicit headerCarrier: HeaderCarrier): Future[NinoResponse] = {

    val url = getNinoLookupUrl(mtdRef)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNino] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[NinoResponseSuccess].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getNino] - Json Validation Error - $invalid")
              NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getNino] - RESPONSE status: ${response.status}, body: ${response.body}")
          }
          NinoResponseError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getNino] - Unexpected future failed error, ${ex.getMessage}")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }
}