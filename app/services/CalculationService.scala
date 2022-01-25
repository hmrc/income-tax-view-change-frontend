/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.{MtdItUser, MtdItUserWithNino}
import config.FrontendAppConfig
import connectors.{IncomeTaxCalculationConnector, IndividualCalculationsConnector}
import enums.{Crystallised, Estimate}

import javax.inject.{Inject, Singleton}
import models.calculation.{CalcDisplayError, CalcDisplayModel, CalcDisplayNoDataFound, CalcDisplayResponseModel, Calculation, CalculationErrorModel, CalculationResponseModel}
import models.liabilitycalculation.LiabilityCalculationResponseModel
import play.api.Logger
import play.api.http.Status
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CalculationService @Inject()(individualCalculationsConnector: IndividualCalculationsConnector,
                                   incomeTaxCalculationConnector: IncomeTaxCalculationConnector,
                                   frontendAppConfig: FrontendAppConfig)(implicit ec: ExecutionContext) {

  def getCalculationDetail(nino: String, taxYear: Int)(implicit headerCarrier: HeaderCarrier): Future[CalcDisplayResponseModel] = {
    for {
      calcIdOrError <- getCalculationId(nino, taxYear)
      lastCalc <- getLatestCalculation(nino, calcIdOrError)
    } yield lastCalc match {
      case calc: Calculation =>
        Logger("application").debug("[CalculationService] Retrieved all Financial Data")
        CalcDisplayModel(calc.timestamp.getOrElse(""), calc.totalIncomeTaxAndNicsDue.getOrElse(0.0), calc, if (calc.crystallised) Crystallised else Estimate)
      case errorModel: CalculationErrorModel if errorModel.code == Status.NOT_FOUND =>
        Logger("application").debug("[CalculationService] No Financial Data found")
        CalcDisplayNoDataFound
      case _: CalculationErrorModel =>
        Logger("application").error("[CalculationService] Could not retrieve Last Tax Calculation. Downstream error.")
        CalcDisplayError
    }
  }

  def getLatestCalculation(nino: String, calcIdOrResponse: Either[CalculationResponseModel, String])
                          (implicit headerCarrier: HeaderCarrier): Future[CalculationResponseModel] = {
    calcIdOrResponse match {
      case Left(error) => Future.successful(error)
      case Right(calcId) =>
        Logger("application").debug("[CalculationService][getLatestCalculation] - Requesting calc data from the backend")
        individualCalculationsConnector.getCalculation(nino, calcId)
    }
  }

  def getCalculationId(nino: String, taxYear: Int)
                      (implicit headerCarrier: HeaderCarrier): Future[Either[CalculationResponseModel, String]] = {
    Logger("application").debug("[CalculationService][getCalculationId] - Requesting latest calc id from the backend")
    individualCalculationsConnector.getLatestCalculationId(nino, taxYearIntToString(taxYear))
  }

  private def taxYearIntToString(taxYear: Int): String = {
    val endYear = taxYear.toString
    val startYear = (taxYear - 1).toString
    s"$startYear-${endYear.takeRight(2)}"
  }

  def getLiabilityCalculationDetail(mtditid: String, nino: String, taxYear: Int)
                                   (implicit headerCarrier: HeaderCarrier): Future[LiabilityCalculationResponseModel] = {
    incomeTaxCalculationConnector.getCalculationResponse(mtditid, nino, taxYear.toString)
  }
}
