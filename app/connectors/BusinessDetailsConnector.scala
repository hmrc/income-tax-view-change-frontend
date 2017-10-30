/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}

import models._
import play.api.Mode.Mode
import play.api.{Configuration, Environment, Logger}
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpResponse}

@Singleton
class BusinessDetailsConnector @Inject()(val http: HttpGet,
                                         val environment: Environment,
                                         val conf: Configuration) extends ServicesConfig with RawResponseReads {

  override protected def mode: Mode = environment.mode
  override protected def runModeConfiguration: Configuration = conf
  lazy val businessListUrl: String = baseUrl("self-assessment-api")
  lazy val getBusinessListUrl: String => String = nino => s"$businessListUrl/ni/$nino/self-employments"

  def getBusinessList(nino: String)(implicit headerCarrier: HeaderCarrier): Future[BusinessListResponseModel] = {

    val url = getBusinessListUrl(nino)
    Logger.debug(s"[BusinessDetailsConnector][getBusinessList] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json"), implicitly) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[BusinessDetailsConnector][getBusinessList] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[List[BusinessModel]].fold(
              invalid => {
                Logger.debug(s"[BusinessDetailsConnector][getBusinessList] - Json Error: $invalid")
                Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - Json Validation Error. Parsing Business Details Response")
                BusinessDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Business Details Response")
              },
              valid => BusinessDetailsModel(valid)
            )
          case _ =>
            Logger.debug(s"[BusinessDetailsConnector][getBusinessList] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - Status: [${response.status}] Returned from business details call")
            BusinessDetailsErrorModel(response.status, response.body)
        }
    } recover {
      case _ =>
        Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - Unexpected future failed error")
        BusinessDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }

}
