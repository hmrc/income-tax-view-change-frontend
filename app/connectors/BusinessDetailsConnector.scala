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

package connectors

import audit.AuditingService
import audit.models.IncomeSourceDetailsResponseAuditModel
import auth.MtdItUserOptionNino
import config.FrontendAppConfig
import models.core.{NinoResponse, NinoResponseError, NinoResponseSuccess}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.{NOT_FOUND, OK}
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
    Logger("application").debug(s"GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger("application").error(s"$invalid")
              IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
            },
            valid => valid
          )
        case status =>
          if (status == NOT_FOUND) {
            Logger("application").warn(s"RESPONSE status: ${response.status}, body: ${response.body}")
          } else if (status >= 500) {
            Logger("application").error(s"RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def modifyHeaderCarrier(path: String,
                          headerCarrier: HeaderCarrier
                         )(implicit appConfig: FrontendAppConfig): HeaderCarrier = {
    val manageBusinessesPattern = """.*/manage-your-businesses/.*""".r
    val incomeSourcesPattern = """.*/income-sources/.*""".r

    path match {
      case manageBusinessesPattern(_*) | incomeSourcesPattern(_*) =>
        checkAndAddTestHeader(path, headerCarrier, appConfig.incomeSourceOverrides(), "afterIncomeSourceCreated")
      case _ =>
        headerCarrier
    }
  }


  def getIncomeSources()(
    implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUserOptionNino[_]): Future[IncomeSourceDetailsResponse] = {

    //Check and add test headers Gov-Test-Scenario for dynamic stub Income Sources Created Scenarios for Income Source Journey
    val hc = modifyHeaderCarrier(mtdItUser.path, headerCarrier)(appConfig)

    val url = getIncomeSourcesUrl(mtdItUser.mtditid)
    Logger("application").debug(s"GET $url")

    //Passing the updated headercarrier implicitly to the request
    http.GET[HttpResponse](url)(implicitly, hc = hc, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[IncomeSourceDetailsModel].fold(
            invalid => {
              Logger("application").error(s"$invalid")
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
            Logger("application").error(s"RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"RESPONSE status: ${response.status}, body: ${response.body}")
          }
          IncomeSourceDetailsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"Unexpected future failed error, ${ex.getMessage}")
        IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }

  def getNino(mtdRef: String)(implicit headerCarrier: HeaderCarrier): Future[NinoResponse] = {

    val url = getNinoLookupUrl(mtdRef)
    Logger("application").debug(s"GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"RESPONSE status: ${response.status}, json: ${response.json}")
          response.json.validate[NinoResponseSuccess].fold(
            invalid => {
              Logger("application").error(s"Json Validation Error - $invalid")
              NinoResponseError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Nino Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"RESPONSE status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"RESPONSE status: ${response.status}, body: ${response.body}")
          }
          NinoResponseError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"Unexpected future failed error, ${ex.getMessage}")
        NinoResponseError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
    }
  }
}