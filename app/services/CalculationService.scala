/*
 * Copyright 2019 HM Revenue & Customs
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

import config.FrontendAppConfig
import javax.inject.{Inject, Singleton}
import connectors.{CalculationDataConnector, LastTaxCalculationConnector}
import enums.{Crystallised, Estimate}
import models.calculation._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CalculationService @Inject()(val lastTaxCalculationConnector: LastTaxCalculationConnector,
                                   val calculationDataConnector: CalculationDataConnector,
                                  val frontendAppConfig: FrontendAppConfig) {

  def getCalculationDetail(nino: String, taxYear: Int)(implicit headerCarrier: HeaderCarrier): Future[CalcDisplayResponseModel] = {
    for {
      lastCalc <- getLatestCalculation(nino, taxYear)
      calcBreakdown <- lastCalc match {
        case calculationData: CalculationModel => getCalculationData(nino, calculationData.calcID)
        case other => Future.successful(other)
      }
    } yield (lastCalc, calcBreakdown) match {
      case (calc: CalculationModel, breakdown: CalculationDataModel) =>
        Logger.debug("[CalculationService] Retrieved all Financial Data")
        CalcDisplayModel(calc.calcTimestamp.getOrElse(""), calc.calcAmount.getOrElse(0.0), Some(breakdown), calc.status)
      case (calc: CalculationModel, _) =>
        Logger.warn("[CalculationService] Could not retrieve Calculation Breakdown. Returning partial Calc Display Model")
        CalcDisplayModel(calc.calcTimestamp.getOrElse(""), calc.calcAmount.getOrElse(0.0), None, calc.status)
      case (_: CalculationErrorModel, _) =>
        Logger.error("[CalculationService] Could not retrieve Last Tax Calculation. Downstream error.")
        CalcDisplayError
      case _ =>
        Logger.warn("[CalculationService] Could not retrieve Last Tax Calculation. No data was found.")
        CalcDisplayNoDataFound
    }
  }

  def getLastEstimatedTaxCalculation(nino: String, year: Int)
                                    (implicit headerCarrier: HeaderCarrier): Future[LastTaxCalculationResponseModel] = {

    Logger.debug("[CalculationService][getLastEstimatedTaxCalculation] - Requesting Last Tax from Backend via Connector")
    lastTaxCalculationConnector.getLastEstimatedTax(nino, year).map {
      case success: LastTaxCalculation =>
        Logger.debug(s"[CalculationService][getLastEstimatedTaxCalculation] - Retrieved Estimated Tax Liability: \n$success")
        success
      case NoLastTaxCalculation =>
        Logger.warn(s"[CalculationService][getLastEstimatedTaxCalculation] - No Data Found response returned from connector")
        NoLastTaxCalculation
      case error: LastTaxCalculationError =>
        Logger.error(s"[CalculationService][getLastEstimatedTaxCalculation] - Error Response Status: ${error.status}, Message: ${error.message}")
        error
    }
  }

  def getAllLatestCalculations(nino: String, orderedYears: List[Int])
                              (implicit headerCarrier: HeaderCarrier): Future[List[LastTaxCalculationWithYear]] = {
    Future.sequence(orderedYears.map {
      year => {
        if (frontendAppConfig.features.calcDataApiEnabled()) {
          getLastEstimatedTaxCalculation(nino, year).map {
            model => LastTaxCalculationWithYear(model, year)
          }
        } else {
          getLatestCalculation(nino, year).map {
            case x: CalculationModel => {
              val status = if (x.isBill) Crystallised else Estimate
              LastTaxCalculationWithYear(LastTaxCalculation(x.calcID, x.calcTimestamp.get, x.calcAmount.get, status), year)
            }
            case x: CalculationErrorModel =>
              LastTaxCalculationWithYear(LastTaxCalculationError(x.code, x.message), year)
          }
        }
      }
    })
  }

  private[CalculationService] def getCalculationData(nino: String, taxCalculationId: String)
                                                    (implicit headerCarrier: HeaderCarrier): Future[CalculationDataResponseModel] = {

    Logger.debug("[CalculationService][getCalculationData] - Requesting calculation data from self-assessment api via Connector")
    calculationDataConnector.getCalculationData(nino, taxCalculationId).map {
      case success: CalculationDataModel =>
        Logger.debug(s"[CalculationService][getCalculationData] - Retrieved Calculation data: \n$success")
        success
      case error: CalculationDataErrorModel =>
        Logger.error(s"[CalculationService][getCalculationData] - Error Response Status: ${error.code}, Message: ${error.message}")
        error
    }
  }

  def getLatestCalculation(nino: String, taxYear: Int)
                          (implicit headerCarrier: HeaderCarrier): Future[CalculationResponseModel] = {
    Logger.debug("[CalculationService][getLatestCalculation] - Requesting latest calc data from the backend")
    calculationDataConnector.getLatestCalculation(nino, taxYear) map {
      case success: CalculationModel =>
        Logger.debug(s"[CalculationService][getLatestCalculation] - Retrieved Calculation data: \n$success")
        success
      case error: CalculationErrorModel =>
        Logger.error(s"[CalculationService][getLatestCalculation] - Error Response Status: ${error.code}, Message: ${error.message}")
        error
    }
  }
}
