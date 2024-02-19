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

package testOnly.connectors

import connectors.RawResponseReads
import play.api.http.Status.OK
import testOnly.TestOnlyAppConfig
import testOnly.models.{DataModel, Nino, SchemaModel}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DynamicStubConnector @Inject()(val appConfig: TestOnlyAppConfig,
                                     val http: HttpClient
                                    )(implicit ec: ExecutionContext) extends RawResponseReads {

  def addSchema(schemaModel: SchemaModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/schema"
    http.POST[SchemaModel, HttpResponse](url, schemaModel)
  }

  def addData(dataModel: DataModel)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/data"
    http.POST[DataModel, HttpResponse](url, dataModel)
  }

  def deleteAllData()(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/all-data"
    http.DELETE[HttpResponse](url)
  }

  def deleteAllSchemas()(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/all-schemas"
    http.DELETE[HttpResponse](url)
  }

  def showLogin(resourceUrl: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/$resourceUrl"
    http.GET[HttpResponse](url)
  }

  def postLogin(resourceUrl: String, nino: String, isAgent: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/$resourceUrl"
    http.POSTForm(url, Map("nino" -> Seq(nino), "isAgent" -> Seq(isAgent)))
  }

  def getOverwriteItsaStatusUrl(nino: String, taxYearRange: String, itsaStatus: String): String = {
    s"${appConfig.itvcDynamicStubUrl}/income-tax-view-change/itsa-status/$nino/$taxYearRange/overwrite/$itsaStatus"
  }


  def overwriteItsaStatus(nino: Nino, taxYearRange: String, itsaStatus: String)
                         (implicit headerCarrier: HeaderCarrier): Future[Unit] = {

    http.GET[HttpResponse](getOverwriteItsaStatusUrl(nino.value, taxYearRange, itsaStatus))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          (): Unit
        case _ =>
          throw new Exception(s"Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
      }
    }
  }

  def getOverwriteCalculationListUrl(nino: String, taxYearRange: String, crystallisationStatus: String): String = {
    s"${appConfig.itvcDynamicStubUrl}/income-tax-view-change/calculation-list/$nino/$taxYearRange/overwrite/$crystallisationStatus"
  }

  def overwriteCalculationList(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Unit] = {

    http.GET[HttpResponse](getOverwriteCalculationListUrl(nino.value, taxYearRange, crystallisationStatus))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          (): Unit
        case _ => throw new Exception(s"Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
      }
    }
  }

}
