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
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.OK
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class BusinessDetailsConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads {

  lazy val businessListUrl: String = baseUrl("self-assessment-api")
  lazy val getBusinessListUrl: String => String = nino => s"$businessListUrl/self-assessment/ni/$nino/self-employments"

  def getBusinessList(nino: String)(implicit headerCarrier: HeaderCarrier): Future[BusinessListResponseModel] = {

    val url = getBusinessListUrl(nino)

    http.GET[HttpResponse](url) flatMap {
      response =>
        response.status match {
          case OK => Logger.debug(s"[BusinessDetailsConnector][getBusinessList] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(response.json.validate[List[BusinessModel]].fold(
              invalid => {
                Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - Json Validation Error. Parsing Business Details Response")
                BusinessDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Business Details Response.")
              },
              valid => BusinessDetailsModel(valid)
            ))
          case _ =>
            Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(BusinessDetailsErrorModel(response.status, response.body))
        }
    } recoverWith {
      case _ =>
        Logger.warn(s"[BusinessDetailsConnector][getBusinessList] - Unexpected future failed error when calling $url.")
        Future.successful(BusinessDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $url."))
    }
  }

}
