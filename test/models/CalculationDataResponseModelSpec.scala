/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class CalculationDataResponseModelSpec extends UnitSpec with Matchers {

  "The CalculationDataResponseModel" should {

    "for the test response" should {

      "have the same values as the calculationDataSuccessWithEoYModel" in {

        calculationDataSuccessModel.totalIncomeTaxNicYtd shouldBe 90500
        calculationDataSuccessModel.totalTaxableIncome shouldBe 198500
        calculationDataSuccessModel.personalAllowance shouldBe 11500

        calculationDataSuccessModel.incomeReceived.selfEmployment shouldBe 200000
        calculationDataSuccessModel.incomeReceived.ukProperty shouldBe 10000
        calculationDataSuccessModel.incomeReceived.bankBuildingSocietyInterest shouldBe 1999
        calculationDataSuccessModel.incomeReceived.ukDividends shouldBe 10000

        calculationDataSuccessModel.taxReliefs shouldBe 0

        calculationDataSuccessModel.payPensionsProfit.basicBand.taxableIncome shouldBe 20000
        calculationDataSuccessModel.payPensionsProfit.basicBand.taxRate shouldBe 20
        calculationDataSuccessModel.payPensionsProfit.basicBand.taxAmount shouldBe 4000

        calculationDataSuccessModel.payPensionsProfit.higherBand.taxableIncome shouldBe 100000
        calculationDataSuccessModel.payPensionsProfit.higherBand.taxRate shouldBe 40
        calculationDataSuccessModel.payPensionsProfit.higherBand.taxAmount shouldBe 40000

        calculationDataSuccessModel.payPensionsProfit.additionalBand.taxableIncome shouldBe 50000
        calculationDataSuccessModel.payPensionsProfit.additionalBand.taxRate shouldBe 45
        calculationDataSuccessModel.payPensionsProfit.additionalBand.taxAmount shouldBe 22500

        calculationDataSuccessModel.savingsAndGains.startBand.taxableIncome shouldBe 1
        calculationDataSuccessModel.savingsAndGains.startBand.taxRate shouldBe 0
        calculationDataSuccessModel.savingsAndGains.startBand.taxAmount shouldBe 0

        calculationDataSuccessModel.savingsAndGains.zeroBand.taxableIncome shouldBe 20
        calculationDataSuccessModel.savingsAndGains.zeroBand.taxRate shouldBe 0
        calculationDataSuccessModel.savingsAndGains.zeroBand.taxAmount shouldBe 0

        calculationDataSuccessModel.savingsAndGains.basicBand.taxableIncome shouldBe 0
        calculationDataSuccessModel.savingsAndGains.basicBand.taxRate shouldBe 20
        calculationDataSuccessModel.savingsAndGains.basicBand.taxAmount shouldBe 0

        calculationDataSuccessModel.savingsAndGains.higherBand.taxableIncome shouldBe 0
        calculationDataSuccessModel.savingsAndGains.higherBand.taxRate shouldBe 40
        calculationDataSuccessModel.savingsAndGains.higherBand.taxAmount shouldBe 0

        calculationDataSuccessModel.savingsAndGains.additionalBand.taxableIncome shouldBe 0
        calculationDataSuccessModel.savingsAndGains.additionalBand.taxRate shouldBe 45
        calculationDataSuccessModel.savingsAndGains.additionalBand.taxAmount shouldBe 0

        calculationDataSuccessModel.dividends.basicBand.taxableIncome shouldBe 1000
        calculationDataSuccessModel.dividends.basicBand.taxRate shouldBe 7.5
        calculationDataSuccessModel.dividends.basicBand.taxAmount shouldBe 75

        calculationDataSuccessModel.dividends.higherBand.taxableIncome shouldBe 2000
        calculationDataSuccessModel.dividends.higherBand.taxRate shouldBe 37.5
        calculationDataSuccessModel.dividends.higherBand.taxAmount shouldBe 750

        calculationDataSuccessModel.dividends.additionalBand.taxableIncome shouldBe 3000
        calculationDataSuccessModel.dividends.additionalBand.taxRate shouldBe 38.1
        calculationDataSuccessModel.dividends.additionalBand.taxAmount shouldBe 1143

        calculationDataSuccessModel.nic.class2 shouldBe 10000
        calculationDataSuccessModel.nic.class4 shouldBe 14000

        calculationDataSuccessModel.eoyEstimate.get.incomeTaxNicAmount shouldBe 66000
      }

    }

    "be formatted to JSON correctly" in {
      Json.toJson[CalculationDataModel](calculationDataSuccessModel) shouldBe calculationDataSuccessJson
    }

    "be able to parse a full JSON string into the Model" in {
      Json.fromJson[CalculationDataModel](calculationDataFullJson) shouldBe JsSuccess(calculationDataSuccessModel)
    }

    "If only mandatory values aren returned from DES then defaulted others to 0" in {
      Json.fromJson[CalculationDataModel](mandatoryCalculationDataSuccessJson) shouldBe JsSuccess(mandatoryOnlyDataModel)
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
      Json.toJson[CalculationDataErrorModel](calculationDataErrorModel) shouldBe calculationDataErrorJson
    }

    "be able to parse a JSON to string into the Model" in {
      Json.fromJson[CalculationDataErrorModel](calculationDataErrorJson) shouldBe JsSuccess(calculationDataErrorModel)
    }
  }

}
