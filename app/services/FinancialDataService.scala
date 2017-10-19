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

import connectors.{CalculationDataConnector, LastTaxCalculationConnector}
import models.{NoLastTaxCalculation, _}
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

@Singleton
class FinancialDataService @Inject()(val lastTaxCalculationConnector: LastTaxCalculationConnector,
                                      val calculationDataConnector: CalculationDataConnector) {


  def getFinancialData(nino: String, taxYear: Int)(implicit headerCarrier: HeaderCarrier): Future[CalcDisplayResponseModel] = {
    for {
      lastCalc <- getLastEstimatedTaxCalculation(nino, taxYear)
      calcBreakdown <- lastCalc match {
        case calculationData: LastTaxCalculation => getCalculationData(nino, calculationData.calcID)
        case other => Future.successful(other)
      }
    } yield (lastCalc, calcBreakdown) match {
      case (calc: LastTaxCalculation, breakdown: CalculationDataModel) =>
        Logger.debug("[FinancialDataService] Retrieved all Financial Data")
        CalcDisplayModel(calc.calcTimestamp, calc.calcAmount, Some(breakdown))
      case (calc: LastTaxCalculation, _) =>
        Logger.debug("[FinancialDataService] Could not retrieve Calculation Breakdown. Returning partial Calc Display Model")
        CalcDisplayModel(calc.calcTimestamp, calc.calcAmount, None)
      case (_: LastTaxCalculationError, _) =>
        Logger.debug("[FinancialDataService] Could not retrieve Last Tax Calculation. Downstream error.")
        CalcDisplayError
      case (NoLastTaxCalculation, _) =>
        Logger.debug("[FinancialDataService] Could not retrieve Last Tax Calculation. No data was found.")
        CalcDisplayNoDataFound
    }
  }


  def getLastEstimatedTaxCalculation(nino: String,
                                     year: Int
                                    )(implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {

    Logger.debug("[FinancialDataService][getLastEstimatedTaxCalculation] - Requesting Last Tax from Backend via Connector")
    lastTaxCalculationConnector.getLastEstimatedTax(nino, year).map {
      case success: LastTaxCalculation =>
        Logger.debug(s"[FinancialDataService][getLastEstimatedTaxCalculation] - Retrieved Estimated Tax Liability: \n$success")
        success
      case NoLastTaxCalculation =>
        Logger.debug(s"[FinancialDataService][getLastEstimatedTaxCalculation] - No Data Found response returned from connector")
        NoLastTaxCalculation
      case error: LastTaxCalculationError =>
        Logger.debug(s"[FinancialDataService][getLastEstimatedTaxCalculation] - Error Response Status: ${error.status}, Message: ${error.message}")
        error
    }
  }

  private[FinancialDataService] def getCalculationData(nino: String,
                                                       taxCalculationId: String
                                                        )(implicit headerCarrier: HeaderCarrier): Future[CalculationDataResponseModel] = {

    Logger.debug("[FinancialDataService][getCalculationData] - Requesting calculation data from self-assessment api via Connector")
    calculationDataConnector.getCalculationData(nino, taxCalculationId).map {
      case success: CalculationDataModel =>
        Logger.debug(s"[FinancialDataService][getCalculationData] - Retrieved Calculation data: \n$success")
        success
      case error: CalculationDataErrorModel =>
        Logger.debug(s"[FinancialDataService][getCalculationData] - Error Response Status: ${error.code}, Message: ${error.message}")
        error
    }
  }
}
