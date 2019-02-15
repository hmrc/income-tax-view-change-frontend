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

package models

import java.time.LocalDateTime

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import enums.{CalcStatus, Crystallised, Estimate}
import models.calculation._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class CalculationResponseModelSpec extends UnitSpec with Matchers {

  "The CalcDisplayModel" when {

    val calcDisplayModelTimestamp: String = LocalDateTime.now().toString
    val calcAmount: BigDecimal = 1

    def calcDisplayModel(calcStatus: CalcStatus, bankBuildingSocietyInterest: BigDecimal): CalcDisplayModel = {

      val incomeReceived: IncomeReceivedModel = IncomeReceivedModel(calculationDataSuccessModel.incomeReceived.selfEmployment,
        calculationDataSuccessModel.incomeReceived.ukProperty,
        bankBuildingSocietyInterest,
        calculationDataSuccessModel.incomeReceived.ukDividends)

      val calculationDataModel = CalculationDataModel(calculationDataSuccessModel.totalIncomeTaxNicYtd,
        calculationDataSuccessModel.totalTaxableIncome: BigDecimal,
        calculationDataSuccessModel.personalAllowance: BigDecimal,
        calculationDataSuccessModel.taxReliefs: BigDecimal,
        calculationDataSuccessModel.totalIncomeAllowancesUsed: BigDecimal,
        incomeReceived: IncomeReceivedModel,
        calculationDataSuccessModel.payPensionsProfit,
        calculationDataSuccessModel.savingsAndGains,
        calculationDataSuccessModel.dividends,
        calculationDataSuccessModel.nic,
        calculationDataSuccessModel.eoyEstimate)

      val calcDataModel: Option[CalculationDataModel] = Some(calculationDataModel)

      CalcDisplayModel(calcDisplayModelTimestamp, calcAmount, calcDataModel, calcStatus)
    }

    "displaying crystallisedWithBBSInterest as true" should  {
    "be crystallised and greater then zero" in {
      calcDisplayModel(Crystallised, 10).crystallisedWithBBSInterest shouldBe true
    }
  }

    "displaying crystallisedWithBBSInterest as false" should {
      "be estimate and greater then zero" in {
        calcDisplayModel(Estimate, 10).crystallisedWithBBSInterest shouldBe false
      }

      "be estimate and zero" in {
        calcDisplayModel(Estimate, 0).crystallisedWithBBSInterest shouldBe false
      }

      "be Crystallised and less then zero" in {
        calcDisplayModel(Crystallised, -1).crystallisedWithBBSInterest shouldBe false
      }

      "be crystallised and zero" in {
        calcDisplayModel(Crystallised, 0).crystallisedWithBBSInterest shouldBe false
      }
    }

    "displaying personalAllowanceHeading" should {
      "show pa-estimates message" in{
        calcDisplayModel(Estimate, 0).personalAllowanceHeading shouldBe ".pa-estimates"
      }
      "show pa-estimates savings message" in{
        calcDisplayModel(Estimate, 10).personalAllowanceHeading shouldBe ".pa-estimates-savings"
      }
      "show pa-bills message" in{
        calcDisplayModel(Crystallised, 0).personalAllowanceHeading shouldBe ".pa-bills"
      }
      "show pa-bills saving message" in{
        calcDisplayModel(Crystallised, 10).personalAllowanceHeading shouldBe ".pa-bills-savings"
      }
    }

//    "display selfEmployedIncomeOrReceived" should {
//      "be ????????????" in {
//        CalcDisplayModel.selfEmployedIncomeOrReceived(, 2019, calculationDataSuccessModel ).
//      }
//    }
  }

  "The CalculationModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[CalculationModel](testCalcModelCrystalised) shouldBe testCalculationOutputJson
    }

    "be able to parse a full JSON string into the Model" in {
      Json.fromJson[CalculationModel](testCalculationInputJson) shouldBe JsSuccess(testCalcModelCrystalised)
    }
  }

  "The CalculationDataErrorModel" should {

    "have the correct status code in the model" in {
      calculationDataErrorModel.code shouldBe testErrorStatus
    }

    "have the correct Error Message in the model" in {
      calculationDataErrorModel.message shouldBe testErrorMessage
    }

    "be formatted to JSON correctly" in {
      Json.toJson[CalculationErrorModel](testCalculationErrorModel) shouldBe testCalculationErrorJson
    }

    "be able to parse a JSON to string into the Model" in {
      Json.fromJson[CalculationErrorModel](testCalculationErrorJson) shouldBe JsSuccess(testCalculationErrorModel)
    }
  }
}
