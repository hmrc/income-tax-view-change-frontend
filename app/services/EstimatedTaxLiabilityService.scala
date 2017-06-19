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

package services

import javax.inject.{Inject, Singleton}

import connectors.LastTaxCalculationConnector
import models._
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EstimatedTaxLiabilityService @Inject()(val lastTaxCalculationConnector: LastTaxCalculationConnector) {

  def getLastEstimatedTaxCalculation(nino: String,
                                     year: String,
                                     calcType: String)
                                    (implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {

    Logger.debug("[EstimatedTaxLiabilityService][getLastEstimatedTaxCalculation] - Requesting Last Tax from Backend via Connector")
    lastTaxCalculationConnector.getLastEstimatedTax(nino, year, calcType).map {
      case success: LastTaxCalculation =>
        Logger.debug(s"[EstimatedTaxLiabilityService][getLastEstimatedTaxCalculation] - Retrieved Estimated Tax Liability: \n\n$success")
        success
      case error: LastTaxCalculationError =>
        Logger.warn(s"[EstimatedTaxLiabilityService][getLastEstimatedTaxCalculation] - Error Response Status: ${error.status}, Message: ${error.message}")
        error
    }
  }
}
