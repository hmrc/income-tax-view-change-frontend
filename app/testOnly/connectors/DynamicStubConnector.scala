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
import play.api.Logger
import play.api.http.Status.OK
import play.api.libs.json.Json
import testOnly.TestOnlyAppConfig
import testOnly.models.{DataModel, Nino, SchemaModel}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import play.api.libs.ws.writeableOf_JsValue
import play.api.libs.ws.writeableOf_urlEncodedForm
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DynamicStubConnector @Inject()(val appConfig: TestOnlyAppConfig,
                                     val http: HttpClientV2
                                    )(implicit ec: ExecutionContext) extends RawResponseReads {

  def addSchema(schemaModel: SchemaModel)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/schema"
    http.post(url"$url")
      .withBody(Json.toJson[SchemaModel](schemaModel))
      .execute[HttpResponse]
  }

  def addData(dataModel: DataModel)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/data"
    http.post(url"$url")
      .withBody(Json.toJson[DataModel](dataModel))
      .execute[HttpResponse]
  }

  def deleteAllData()(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/all-data"
    http.delete(url"$url")
      .execute[HttpResponse]

  }

  def deleteAllSchemas()(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/setup/all-schemas"
    http.delete(url"$url")
      .execute[HttpResponse]
  }

  def showLogin(resourceUrl: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/$resourceUrl"
    http.get(url"$url")
      .execute[HttpResponse]
  }

  def postLogin(resourceUrl: String, nino: String, isAgent: String)(implicit headerCarrier: HeaderCarrier): Future[HttpResponse] = {
    lazy val url = s"${appConfig.dynamicStubUrl}/$resourceUrl"
    lazy val data = Map("nino" -> Seq(nino), "isAgent" -> Seq(isAgent))
    http.post(url"$url")
      .withBody(data)
      .execute[HttpResponse]
  }

  def getOverwriteItsaStatusUrl(nino: String, taxYearRange: String, itsaStatus: String): String = {
    s"${appConfig.dynamicStubUrl}/income-tax-view-change/itsa-status/$nino/$taxYearRange/overwrite/$itsaStatus"
  }

  def getOverwriteCustomUserUrl(nino: String, mtdid: String): String = {
    s"${appConfig.dynamicStubUrl}/income-tax-view-change/override/custom-user/$nino/$mtdid"
  }

  def overwriteCustomUser(nino: Nino, mtdid: String, channel: String)
                         (implicit headerCarrier: HeaderCarrier): Future[Unit] = {


    val url = getOverwriteCustomUserUrl(nino.value, mtdid)

    val requestJson = Json.obj(
      "channel" -> channel
    )

    http.post(url"$url")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .withBody(requestJson)
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          (): Unit
        case _ =>
          Logger("application").error(s" Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
          throw new Exception(s"Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
      }
    }
    Future(())
  }


  def overwriteItsaStatus(nino: Nino, taxYearRange: String, itsaStatus: String)
                         (implicit headerCarrier: HeaderCarrier): Future[Unit] = {


    val url = getOverwriteItsaStatusUrl(nino.value, taxYearRange, itsaStatus)
    http.get(url"$url")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          (): Unit
        case _ =>
          Logger("application").error(s" Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
          throw new Exception(s"Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
      }
    }
    Future(())
  }

  def getOverwriteCalculationListUrl(nino: String, taxYearRange: String, crystallisationStatus: String): String = {
    s"${appConfig.dynamicStubUrl}/income-tax-view-change/calculation-list/$nino/$taxYearRange/overwrite/$crystallisationStatus"
  }

  def overwriteCalculationList(nino: Nino, taxYearRange: String, crystallisationStatus: String)
                              (implicit headerCarrier: HeaderCarrier): Future[Unit] = {

    val url = getOverwriteCalculationListUrl(nino.value, taxYearRange, crystallisationStatus)
    http.get(url"$url")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse] map { response =>
      response.status match {
        case OK =>
          (): Unit
        case _ =>
          Logger("application").error(s" Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
          throw new Exception(s"Overwrite unsuccessful. ~ Response status: ${response.status} ~. < Response body: ${response.body} >")
      }
    }
  }

}
