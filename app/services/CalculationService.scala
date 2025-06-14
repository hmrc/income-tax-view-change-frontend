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

package services

import connectors.IncomeTaxCalculationConnector
import models.liabilitycalculation.LiabilityCalculationResponseModel
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationService @Inject()(incomeTaxCalculationConnector: IncomeTaxCalculationConnector)(implicit ec: ExecutionContext) {

  def getLatestCalculation(mtditid: String, nino: String, calcId: String, taxYear: Int)
                          (implicit headerCarrier: HeaderCarrier): Future[LiabilityCalculationResponseModel] = {
    Logger("application").debug("" +
      s"Requesting calc data from the backend by calc id and taxYear: $calcId - $taxYear")
    incomeTaxCalculationConnector.getCalculationResponseByCalcId(mtditid, nino, calcId, taxYear)
  }

  def getLiabilityCalculationDetail(mtditid: String, nino: String, taxYear: Int)
                                   (implicit headerCarrier: HeaderCarrier): Future[LiabilityCalculationResponseModel] = {
    incomeTaxCalculationConnector.getCalculationResponse(mtditid, nino, taxYear.toString).map(value =>{
      value
    })
  }
}
