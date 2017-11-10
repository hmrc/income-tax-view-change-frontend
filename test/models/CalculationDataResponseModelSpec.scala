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

package models

import assets.TestConstants._
import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import assets.TestConstants.CalcBreakdown._

class CalculationDataResponseModelSpec extends UnitSpec with Matchers {

  "The CalculationDataResponseModel" should {

    "for the test response" should {

      "have the same values as the calculationDataSuccessWithEoYModel" in {
        calculationDataSuccessModel.incomeTaxYTD shouldBe 90500
        calculationDataSuccessModel.incomeTaxThisPeriod shouldBe 2000
        calculationDataSuccessModel.profitFromSelfEmployment shouldBe 200000
        calculationDataSuccessModel.profitFromUkLandAndProperty shouldBe 10000
        calculationDataSuccessModel.totalIncomeReceived shouldBe 230000
        calculationDataSuccessModel.proportionAllowance shouldBe 11500
        calculationDataSuccessModel.totalIncomeOnWhichTaxIsDue shouldBe 198500
        calculationDataSuccessModel.payPensionsProfitAtBRT shouldBe Some(20000)
        calculationDataSuccessModel.incomeTaxOnPayPensionsProfitAtBRT shouldBe 4000
        calculationDataSuccessModel.payPensionsProfitAtHRT shouldBe Some(100000)
        calculationDataSuccessModel.incomeTaxOnPayPensionsProfitAtHRT shouldBe 40000
        calculationDataSuccessModel.payPensionsProfitAtART shouldBe Some(50000)
        calculationDataSuccessModel.incomeTaxOnPayPensionsProfitAtART shouldBe 22500
        calculationDataSuccessModel.incomeTaxDue shouldBe 66500
        calculationDataSuccessModel.nationalInsuranceClass2Amount shouldBe 10000
        calculationDataSuccessModel.totalClass4Charge shouldBe 14000
        calculationDataSuccessModel.rateBRT shouldBe 20
        calculationDataSuccessModel.rateHRT shouldBe 40
        calculationDataSuccessModel.rateART shouldBe 45
        calculationDataSuccessModel.eoyEstimate.get.incomeTaxNicAmount shouldBe 66000
      }

    }

    "be formatted to JSON correctly" in {
      Json.toJson[CalculationDataModel](calculationDataSuccessModel) shouldBe calculationDataSuccessJson
    }

    "be able to parse a full JSON string into the Model" in {
      Json.parse(calculationDataFullString).as[CalculationDataModel] shouldBe calculationDataSuccessModel
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
      Json.parse(calculationDataErrorString).as[CalculationDataErrorModel] shouldBe calculationDataErrorModel
    }
  }

}
