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

import config.FrontendAppConfig
import connectors.IndividualCalculationsConnector
import enums.{Crystallised, Estimate}

import javax.inject.{Inject, Singleton}
import models.calculation._
import models.liabilitycalculation.{AllowancesAndDeductions, ChargeableEventGainsIncome, LiabilityCalculationResponse, LiabilityCalculationResponseModel, MarriageAllowanceTransferOut, Metadata}
import play.api.Logger
import play.api.http.Status
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

  def getLiabilityCalculationDetail(nino: String, taxYear: Int)(implicit headerCarrier: HeaderCarrier): Future[LiabilityCalculationResponseModel] = {
    Future.successful(
      LiabilityCalculationResponse(
        metadata = Metadata(calculationTimestamp = "2019-02-15T09:35:15.094Z", crystallised = true),
        calculation = Some(models.liabilitycalculation.Calculation(
          allowancesAndDeductions = Some(AllowancesAndDeductions(
            personalAllowance = Some(12500),
            reducedPersonalAllowance = Some(12500),
            marriageAllowanceTransferOut = Some(MarriageAllowanceTransferOut(
              personalAllowanceBeforeTransferOut = 5000.99,
              transferredOutAmount = 5000.99)),
            pensionContributions = Some(5000.99),
            lossesAppliedToGeneralIncome = Some(12500),
            giftOfInvestmentsAndPropertyToCharity = Some(12500),
            grossAnnuityPayments = Some(5000.99),
            qualifyingLoanInterestFromInvestments = Some(5000.99),
            postCessationTradeReceipts = Some(5000.99),
            paymentsToTradeUnionsForDeathBenefits = Some(5000.99))
          ),
          taxCalculation = Some(models.liabilitycalculation.taxcalculation.TaxCalculation(
            incomeTax = models.liabilitycalculation.taxcalculation.IncomeTax(
              totalIncomeReceivedFromAllSources = 12500,
              totalTaxableIncome = 12500,
              totalReliefs = Some(5000.99),
              totalAllowancesAndDeductions = 12500
            ),
            totalIncomeTaxAndNicsDue = 5000.99
          ))
        ))
      )
    )
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
}
