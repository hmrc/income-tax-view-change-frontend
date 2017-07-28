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
import utils.ImplicitDateFormatter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class PropertyDetailsConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads with ImplicitDateFormatter {

  lazy val apiContextRoute: String = baseUrl("self-assessment-api")
  lazy val getPropertyDetailsUrl: String => String = nino => s"$apiContextRoute/ni/$nino/uk-properties"

  // TODO: For MVP the only accounting period for Property is 2017/18. This needs to be enhanced post-MVP
  val defaultSuccessResponse = PropertyDetailsModel(AccountingPeriodModel("2017-04-06", "2018-04-05"))

  def getPropertyDetails(nino: String)(implicit headerCarrier: HeaderCarrier): Future[PropertyDetailsResponseModel] = {

    val url = getPropertyDetailsUrl(nino)

    http.GET[HttpResponse](url)(httpReads, headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.1.0+json")) flatMap {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[PropertyDetailsConnector][getPropertyDetails] - RESPONSE status: ${response.status}, json: ${response.json}")
            Future.successful(defaultSuccessResponse)
          case _ =>
            Logger.warn(s"[PropertyDetailsConnector][getPropertyDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(PropertyDetailsErrorModel(response.status, response.body))
        }
    } recoverWith {
      case _ =>
        Logger.warn(s"[PropertyDetailsConnector][getPropertyDetails] - Unexpected future failed error when calling $url.")
        Future.successful(PropertyDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error when calling $url."))
    }
  }

}
