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

import config.FrontendAppConfig
import models.optOut.{OptOutApiCallFailureResponse, OptOutApiCallResponse, OptOutApiCallSuccessfulResponse}
import models.updateIncomeSource._
import play.api.Logger
import play.api.http.Status.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OptOutConnector2 @Inject()(val http: HttpClient,
                                 val appConfig: FrontendAppConfig

                                           )(implicit val ec: ExecutionContext) extends RawResponseReads {

  //todo appConfig.itvcProtectedService?
  def getUrl(taxableEntityId: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax/itsa-status/update/$taxableEntityId"
  }

  def updateCessationDate(taxableEntityId: String)(implicit headerCarrier: HeaderCarrier): Future[OptOutApiCallResponse] = {
    val body = ""

    http.PUT[String, HttpResponse](
      getUrl(taxableEntityId),
      body, Seq[(String, String)]()).map { response =>
      response.status match {
        case OK => response.json.validate[OptOutApiCallSuccessfulResponse].fold(
          invalid => {
            Logger("application").error(s"Json validation error parsing update income source response, error $invalid")
            OptOutApiCallFailureResponse("Json validation error parsing response")
          },
          valid => valid
        )
        case _ =>
          response.json.validate[OptOutApiCallFailureResponse].fold(
            invalid => {
              Logger("application").error(s"Json validation error parsing update income source response, error $invalid")
              OptOutApiCallFailureResponse("Json validation error parsing response")
            },
            valid => valid
          )
      }
    }
  }
}
