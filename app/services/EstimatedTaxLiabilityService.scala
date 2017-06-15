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

import connectors.EstimatedTaxLiabilityConnector
import models._
import play.api.Logger
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class EstimatedTaxLiabilityService @Inject()(val estimatedTaxLiabilityConnector: EstimatedTaxLiabilityConnector) {

  def getEstimatedTaxLiability(mtditid: String)(implicit headerCarrier: HeaderCarrier): Future[EstimatedTaxLiabilityResponseModel] = {
    Logger.debug("[EstimatedTaxLiabilityService][getEstimateTaxLiability] - Requesting Estimate Liability from Backend via Connector")
    estimatedTaxLiabilityConnector.getEstimatedTaxLiability(mtditid).map {
      case success: EstimatedTaxLiability =>
        Logger.debug(s"[EstimatedTaxLiabilityService][getEstimateTaxLiability] - Retrieved Estimated Tax Liability: \n\n$success")
        success
      case error: EstimatedTaxLiabilityError =>
        Logger.debug(s"[EstimatedTaxLiabilityService][getEstimateTaxLiability] - Error Response Status: ${error.status}, Message: ${error.message}")
        error
    }
  }
}
