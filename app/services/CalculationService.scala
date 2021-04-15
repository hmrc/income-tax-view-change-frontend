/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.IndividualCalculationsConnector
import enums.{Crystallised, Estimate}
import javax.inject.{Inject, Singleton}
import models.calculation._
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationService @Inject()(val individualCalculationsConnector: IndividualCalculationsConnector,
                                   val frontendAppConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  def getCalculationDetail(nino: String, taxYear: Int)(implicit headerCarrier: HeaderCarrier): Future[CalcDisplayResponseModel] = {
    for {
      calcIdOrError <- getCalculationId(nino, taxYear)
      lastCalc <- getLatestCalculation(nino, calcIdOrError)
    } yield lastCalc match {
      case calc: Calculation =>
        Logger.debug("[CalculationService] Retrieved all Financial Data")
        CalcDisplayModel(calc.timestamp.getOrElse(""), calc.totalIncomeTaxAndNicsDue.getOrElse(0.0), calc, if (calc.crystallised) Crystallised else Estimate)
      case _: CalculationErrorModel =>
        Logger.error("[CalculationService] Could not retrieve Last Tax Calculation. Downstream error.")
        CalcDisplayError
    }
  }

  def getAllLatestCalculations(nino: String, orderedYears: List[Int])
                              (implicit headerCarrier: HeaderCarrier): Future[List[CalculationResponseModelWithYear]] = {
    Future.sequence(
      orderedYears.map { year =>
        for {
          calcIdOrError <- getCalculationId(nino, year)
          lastCalc <- getLatestCalculation(nino, calcIdOrError)
        } yield CalculationResponseModelWithYear(lastCalc, year)
      }
    )
  }

  def getLatestCalculation(nino: String, calcIdOrResponse: Either[CalculationResponseModel, String])
                          (implicit headerCarrier: HeaderCarrier): Future[CalculationResponseModel] = {
    calcIdOrResponse match {
      case Left(error) => Future.successful(error)
      case Right(calcId) =>
        Logger.debug("[CalculationService][getLatestCalculation] - Requesting calc data from the backend")
        individualCalculationsConnector.getCalculation(nino, calcId)
    }
  }

  def getCalculationId(nino: String, taxYear: Int)
                      (implicit headerCarrier: HeaderCarrier): Future[Either[CalculationResponseModel, String]] = {
    Logger.debug("[CalculationService][getCalculationId] - Requesting latest calc id from the backend")
    individualCalculationsConnector.getLatestCalculationId(nino, taxYearIntToString(taxYear))
  }

  private def taxYearIntToString(taxYear: Int): String = {
    val endYear = taxYear.toString
    val startYear = (taxYear - 1).toString
    s"$startYear-${endYear.takeRight(2)}"
  }
}
