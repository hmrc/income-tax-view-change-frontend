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

package common.connectors

import common.auth.AuthorisedAndEnrolledRequest
import common.config.FrontendAppConfig
import common.models.audit.IncomeSourceDetailsResponseAuditModel
import common.models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import common.services.AuditingService
import common.utils.sessionUtils.SessionKeys
import play.api.Logger
import play.api.http.Status.OK
import play.api.http.Status
import play.api.mvc.Request
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import common.utils.Headers.checkAndAddTestHeader

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceConnector @Inject()(
                                          httpClient: HttpClientV2,
                                          auditingService: AuditingService,
                                          appConfig: FrontendAppConfig
                                        )(implicit val ec: ExecutionContext) {

  private[connectors] def getIncomeSourcesUrl(mtditid: String): String = {
    s"${appConfig.incomeTaxBusinessDetailsBaseUrl}/income-tax-business-details/income-sources/$mtditid"
  }

  def modifyHeaderCarrier(
                           path: String,
                           headerCarrier: HeaderCarrier
                         )(implicit appConfig: FrontendAppConfig, request: Request[_]): HeaderCarrier = {

    val manageBusinessesPattern = """.*/manage-your-businesses/.*""".r
    val incomeSourcesPattern    = """.*/income-sources/.*""".r

    // After the user confirms migration, send the Gov-Test-Scenario: afterMigration header so local and staging
    // environments return the post-migration stub data instead of the default response.
    // The header is not included in production's headersAllowlist, so it is never forwarded downstream in production.
    //
    // To test the post-migration stub data in QA, add Gov-Test-Scenario to QA's headersAllowlist.
    if (request.session.get(SessionKeys.triggeredMigrationConfirmed).contains("true")) {
      return checkAndAddTestHeader(path, headerCarrier, appConfig.triggeredMigrationOverrides(), "afterMigration")
    }

    if (manageBusinessesPattern.matches(path) || incomeSourcesPattern.matches(path)) {
      return checkAndAddTestHeader(path, headerCarrier, appConfig.incomeSourceOverrides(), "afterIncomeSourceCreated")
    }

    headerCarrier
  }
  
  def getIncomeSources()(implicit headerCarrier: HeaderCarrier, mtdItUser: AuthorisedAndEnrolledRequest[_]): Future[IncomeSourceDetailsResponse] = {

    val url = getIncomeSourcesUrl(mtdItUser.mtditId)
    Logger("application").debug(s"GET $url")

    val hc: HeaderCarrier = modifyHeaderCarrier(mtdItUser.path, headerCarrier)(appConfig, mtdItUser)

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