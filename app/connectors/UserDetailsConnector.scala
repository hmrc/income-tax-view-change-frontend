/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

@Singleton
class UserDetailsConnector @Inject()(val http: HttpClient) extends RawResponseReads {

  def getUserDetails(userDetailsUrl: String)(implicit headerCarrier: HeaderCarrier): Future[UserDetailsResponseModel] = {
    Logger.debug(s"[UserDetailsConnector][getUserDetails] - GET $userDetailsUrl")
    http.GET[HttpResponse](userDetailsUrl) map {
      response =>
        response.status match {
          case OK =>
            Logger.debug(s"[UserDetailsConnector][getUserDetails] - RESPONSE status: ${response.status}, json: ${response.json}")
            response.json.validate[UserDetailsModel].fold(
              invalid => {
                Logger.warn(s"[UserDetailsConnector][getUserDetails] - Json Validation Error. Parsing User Details Response.")
                UserDetailsError
              },
              valid => valid
            )
          case _ =>
            Logger.debug(s"[UserDetailsConnector][getUserDetails] - RESPONSE status: ${response.status}, body: ${response.body}")
            Logger.warn(s"[UserDetailsConnector][getUserDetails] - Response status: [${response.status}] returned from User Details call")
            UserDetailsError
        }
    } recover {
      case _ =>
        Logger.warn(s"[UserDetailsConnector][getUserDetails] - Unexpected future failed error when calling $userDetailsUrl.")
        UserDetailsError
    }
  }
}
