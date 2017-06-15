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
import play.api.http.Status._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, _}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EstimatedTaxLiabilityConnector @Inject()(val http: HttpGet) extends ServicesConfig with RawResponseReads {

  lazy val protectedMicroserviceUrl: String = baseUrl("income-tax-view-change")
  lazy val getEstimatedTaxLiabilityUrl: String => String = mtditid => s"$protectedMicroserviceUrl/income-tax-view-change/estimated-tax-liability/$mtditid"

  def getEstimatedTaxLiability(mtditid: String)(implicit headerCarrier: HeaderCarrier): Future[EstimatedTaxLiabilityResponseModel] = {

    val url = getEstimatedTaxLiabilityUrl(mtditid)
    Logger.debug(s"[EstimatedTaxLiabilityConnector][getEstimatedTaxLiability] - GET $url")

    http.GET[HttpResponse](url) flatMap {
      response =>
        response.status match {
          case OK =>
            Future.successful(response.json.validate[EstimatedTaxLiability].fold(
              invalid => {
                Logger.warn(s"[EstimatedTaxLiabilityConnector][getEstimatedTaxLiability] - Json Validation Error. Parsing Estimated Tax Liability Response.")
                EstimatedTaxLiabilityError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Estimated Tax Liability Response.")
              },
              valid => valid
            ))
          case _ =>
            Logger.warn(s"[EstimatedTaxLiabilityConnector][getEstimatedTaxLiability] - RESPONSE status: ${response.status}, body: ${response.body}")
            Future.successful(EstimatedTaxLiabilityError(response.status, response.body))
        }
    } recoverWith {
      case _ =>
        Logger.warn(s"[EstimatedTaxLiabilityConnector][getEstimatedTaxLiability] - Unexpected failed future on call to $url")
        Future.successful(EstimatedTaxLiabilityError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failed future on call to $url"))
    }
  }
}