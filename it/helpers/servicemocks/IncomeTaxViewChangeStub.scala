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

package helpers.servicemocks

import helpers.{IntegrationTestConstants, WiremockHelper}
import models.{CalculationDataModel, LastTaxCalculation}
import play.api.http.Status
import play.api.i18n.I18nSupport

object IncomeTaxViewChangeStub {

  val lastCalcUrl: (String,String) => String = (nino, year) =>
    s"/income-tax-view-change/estimated-tax-liability/$nino/$year/it"

  val calcUrl: (String,String) => String = (nino, taxCalculationId) => s"/ni/$nino/calculations/$taxCalculationId"

  def stubGetLastTaxCalc(nino: String, year: String, lastCalc: LastTaxCalculation): Unit = {
    val financialDataResponse =
      IntegrationTestConstants
        .GetLastCalculation.successResponse(
        lastCalc.calcID,
        lastCalc.calcTimestamp,
        lastCalc.calcAmount)
        .toString()
    WiremockHelper.stubGet(lastCalcUrl(nino, year), Status.OK, financialDataResponse)
  }

  def verifyGetLastTaxCalc(nino: String, year: String): Unit =
    WiremockHelper.verifyGet(lastCalcUrl(nino, year))


  def stubGetCalcData(nino: String, year: String, calc: CalculationDataModel): Unit = {
    val financialDataResponse =
      IntegrationTestConstants
        .GetCalculationData.successResponse(
        calc.incomeTaxYTD.get,
        calc.incomeTaxThisPeriod.get,
        calc.profitFromSelfEmployment.get,
        calc.profitFromUkLandAndProperty.get,
        calc.totalIncomeReceived.get,
        calc.personalAllowance.get,
        calc.totalIncomeOnWhichTaxIsDue.get,
        calc.payPensionsProfitAtBRT.get,
        calc.incomeTaxOnPayPensionsProfitAtBRT.get,
        calc.payPensionsProfitAtHRT.get,
        calc.incomeTaxOnPayPensionsProfitAtHRT.get,
        calc.payPensionsProfitAtART.get,
        calc.incomeTaxOnPayPensionsProfitAtART.get,
        calc.incomeTaxDue.get,
        calc.nicTotal.get,
        calc.rateBRT.get,
        calc.rateHRT.get,
        calc.rateART.get)
        .toString()
    WiremockHelper.stubGet(calcUrl(nino, year), Status.OK, financialDataResponse)
  }

  def verifyGetCalcData(nino: String, taxCalculationId: String): Unit =
    WiremockHelper.verifyGet(calcUrl(nino, taxCalculationId))
}
