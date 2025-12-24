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
import auth.authV2.models.AuthorisedAndEnrolledRequest
import config.FrontendAppConfig
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.Headers.checkAndAddTestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessDetailsConnector @Inject()(
                                          httpClient: HttpClientV2,
                                          auditingService: AuditingService,
                                          appConfig: FrontendAppConfig
                                        )(implicit val ec: ExecutionContext) {

  private[connectors] def getBusinessDetailsUrl(nino: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/get-business-details/nino/$nino"
  }

  private[connectors] def getIncomeSourcesUrl(mtditid: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/income-sources/$mtditid"
  }

  private[connectors] def getNinoLookupUrl(mtdRef: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/nino-lookup/$mtdRef"
  }

  def getBusinessDetails(nino: String)(implicit headerCarrier: HeaderCarrier): Future[IncomeSourceDetailsResponse] = {
    val url = getBusinessDetailsUrl(nino)
    Logger("application").debug(s"GET $url")

    httpClient
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"[FE Business Details Connector][getBusinessDetails] RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[IncomeSourceDetailsModel].fold(
              invalid => {
                Logger("application").error(s"$invalid")
                IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
              },
              valid => valid
            )
          case status if status >= 500 =>
            Logger("application").error(s"[FE Business Details Connector][getBusinessDetails] RESPONSE status: ${response.status}, body: ${response.body}")
            IncomeSourceDetailsError(response.status, response.body)
          case _ =>
            Logger("application").warn(s"[FE Business Details Connector][getBusinessDetails] RESPONSE status: ${response.status}, body: ${response.body}")
            IncomeSourceDetailsError(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"[FE Business Details Connector][getBusinessDetails] Unexpected future failed error, ${ex.getMessage}")
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


  def getIncomeSources()(implicit headerCarrier: HeaderCarrier, mtdItUser: AuthorisedAndEnrolledRequest[_]): Future[IncomeSourceDetailsResponse] = {

    val url = getIncomeSourcesUrl(mtdItUser.mtditId)
    Logger("application").debug(s"GET $url")

    val hc: HeaderCarrier = modifyHeaderCarrier(mtdItUser.path, headerCarrier)(appConfig)

    httpClient
      .get(url"$url")
      .setHeader(hc.extraHeaders:_*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"[FE Business Details Connector][getIncomeSources] RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[IncomeSourceDetailsModel].fold(
              invalid => {
                Logger("application").error(s"[FE Business Details Connector][getIncomeSources] $invalid")
                IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
              },
              valid => {
                auditingService.extendedAudit(
                  IncomeSourceDetailsResponseAuditModel(
                    mtdItUser,
                    valid.nino,
                    valid.businesses.map(_.incomeSourceId),
                    valid.properties.map(_.incomeSourceId),
                    valid.yearOfMigration
                  ))
                valid
              }
            )
          case status if (status >= 500) =>
            Logger("application").error(s"[FE Business Details Connector][getIncomeSources] RESPONSE status: ${response.status}, body: ${response.body}")
            IncomeSourceDetailsError(response.status, response.body)
          case _ =>
            Logger("application").warn(s"[FE Business Details Connector][getIncomeSources] RESPONSE status: ${response.status}, body: ${response.body}")
            IncomeSourceDetailsError(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"[FE Business Details Connector][getIncomeSources] Unexpected future failed error, ${ex.getMessage}")
          IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error, ${ex.getMessage}")
      }
  }
}