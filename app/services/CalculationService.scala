/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.IncomeTaxViewChangeConnector
import javax.inject.{Inject, Singleton}
import models.calculation._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class CalculationService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                   val frontendAppConfig: FrontendAppConfig) {

  def getCalculationDetail(nino: String, taxYear: Int)(implicit headerCarrier: HeaderCarrier): Future[CalcDisplayResponseModel] = {
    for {
      lastCalc <- getLatestCalculation(nino, taxYear)
    } yield lastCalc match {
      case calc: CalculationModel if calc.calculationDataModel.isDefined =>
        Logger.debug("[CalculationService] Retrieved all Financial Data")
        CalcDisplayModel(calc.calcTimestamp.getOrElse(""), calc.calcAmount.getOrElse(0.0), calc.calculationDataModel, calc.status)
      case calc: CalculationModel =>
        Logger.warn("[CalculationService] Could not retrieve Calculation Breakdown. Returning partial Calc Display Model")
        CalcDisplayModel(calc.calcTimestamp.getOrElse(""), calc.calcAmount.getOrElse(0.0), None, calc.status)
      case _: CalculationErrorModel =>
        Logger.error("[CalculationService] Could not retrieve Last Tax Calculation. Downstream error.")
        CalcDisplayError
    }
  }

  def getAllLatestCalculations(nino: String, orderedYears: List[Int])
                              (implicit headerCarrier: HeaderCarrier): Future[List[CalculationResponseModelWithYear]] = {
    Future.sequence(
      orderedYears.map { year =>
        getLatestCalculation(nino, year).map {
          model => CalculationResponseModelWithYear(model, year)
        }
      }
    )
  }

  def getLatestCalculation(nino: String, taxYear: Int)
                          (implicit headerCarrier: HeaderCarrier): Future[CalculationResponseModel] = {
    Logger.debug("[CalculationService][getLatestCalculation] - Requesting latest calc data from the backend")
    incomeTaxViewChangeConnector.getLatestCalculation(nino, taxYear)
  }
}
