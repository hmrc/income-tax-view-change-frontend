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

package models.liabilitycalculation

import models.liabilitycalculation.viewModels.IncomeBreakdownViewModel
import play.api.http.Status
import play.api.libs.json._
import testConstants.NewCalcBreakdownTestConstants.liabilityCalculationModelSuccessFull
import testUtils.UnitSpec

import scala.io.Source

class LiabilityCalculationResponseModelSpec extends UnitSpec {

  "LastTaxCalculationResponseMode model" when {
    "successful successModelMinimal" should {
      val successModelMinimal = LiabilityCalculationResponse(
        calculation = None,
        metadata = Metadata(
          calculationTimestamp = "2019-02-15T09:35:15.094Z",
          crystallised = true)
      )
      val expectedJson = s"""
                            |{
                            |  "metadata" : {
                            |    "calculationTimestamp" : "2019-02-15T09:35:15.094Z",
                            |    "crystallised" : true
                            |  }
                            |}
                            |""".stripMargin.trim


      "be translated to Json correctly" in {
        Json.toJson(successModelMinimal) shouldBe Json.parse(expectedJson)
      }
      "should convert from json to model" in {
        val calcResponse = Json.fromJson[LiabilityCalculationResponse](Json.parse(expectedJson))
        Json.toJson(calcResponse.get) shouldBe Json.parse(expectedJson)
      }

      "create a minimal IncomeBreakdownViewModel when there is a minimal Calculation" in {
        val expectedIncomeBreakdownViewModel = IncomeBreakdownViewModel(
          totalPayeEmploymentAndLumpSumIncome = None,
          totalBenefitsInKind = None,
          totalEmploymentExpenses = None,
          totalSelfEmploymentProfit = None,
          totalPropertyProfit = None,
          totalFHLPropertyProfit = None,
          totalForeignPropertyProfit = None,
          totalEeaFhlProfit = None,
          chargeableForeignDividends = None,
          chargeableForeignSavingsAndGains = None,
          chargeableOverseasPensionsStateBenefitsRoyalties = None,
          chargeableAllOtherIncomeReceivedWhilstAbroad = None,
          totalOverseasIncomeAndGains = None,
          totalForeignBenefitsAndGifts = None,
          savingsAndGainsTaxableIncome = None,
          totalOfAllGains = None,
          dividendsTaxableIncome = None,
          totalOccupationalPensionIncome = None,
          totalStateBenefitsIncome = None,
          totalShareSchemesIncome = None,
          totalIncomeReceived = None
        )

        Calculation.getIncomeBreakdownViewModel(Calculation()) shouldBe expectedIncomeBreakdownViewModel
      }
    }

    "successful successModelFull" should {

      val source = Source.fromURL(getClass.getResource("/liabilityResponsePruned.json"))
      val expectedJsonPruned = try source.mkString finally source.close()

      "be translated to Json correctly" in {
        Json.toJson(liabilityCalculationModelSuccessFull) shouldBe Json.parse(expectedJsonPruned)
      }

      "should convert from json to model" in {
        val calcResponse = Json.fromJson[LiabilityCalculationResponse](Json.parse(expectedJsonPruned))
        Json.toJson(calcResponse.get) shouldBe Json.parse(expectedJsonPruned)
      }

      "create a full IncomeBreakdownViewModel when there is a full Calculation" in {
        val expectedIncomeBreakdownViewModel = IncomeBreakdownViewModel(
          totalPayeEmploymentAndLumpSumIncome = Some(5000.99),
          totalBenefitsInKind = Some(5000.99),
          totalEmploymentExpenses = Some(5000.99),
          totalSelfEmploymentProfit = Some(12500),
          totalPropertyProfit = Some(12500),
          totalFHLPropertyProfit = Some(12500),
          totalForeignPropertyProfit = Some(12500),
          totalEeaFhlProfit = Some(12500),
          chargeableForeignDividends = Some(12500),
          chargeableForeignSavingsAndGains = Some(12500),
          chargeableOverseasPensionsStateBenefitsRoyalties = Some(5000.99),
          chargeableAllOtherIncomeReceivedWhilstAbroad = Some(5000.99),
          totalOverseasIncomeAndGains = Some(5000.99),
          totalForeignBenefitsAndGifts = Some(5000.99),
          savingsAndGainsTaxableIncome = Some(12500),
          totalOfAllGains = Some(12500),
          dividendsTaxableIncome = Some(12500),
          totalOccupationalPensionIncome = Some(5000.99),
          totalStateBenefitsIncome = Some(5000.99),
          totalShareSchemesIncome = Some(5000.99),
          totalIncomeReceived = Some(12500)
        )

        Calculation.getIncomeBreakdownViewModel(successModelFull.calculation.get) shouldBe expectedIncomeBreakdownViewModel
      }
    }

    "not successful" should {
      val errorStatus = 500
      val errorMessage = "Error Message"
      val errorModel = LiabilityCalculationError(Status.INTERNAL_SERVER_ERROR, "Error Message")

      "have the correct Status (500)" in {
        errorModel.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
      "have the correct message" in {
        errorModel.message shouldBe "Error Message"
      }
      "be translated into Json correctly" in {
        Json.prettyPrint(Json.toJson(errorModel)) shouldBe
          (s"""
              |{
              |  "status" : $errorStatus,
              |  "message" : "$errorMessage"
              |}
           """.stripMargin.trim)
      }
    }
  }

}
