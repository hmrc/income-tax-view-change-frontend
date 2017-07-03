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

/**
 * Created by sam on 03/07/17.
 */
class CalculationDataResponseModelSpec extends UnitSpec with Matchers {

  "The CalculationDataResponseModel" should {

    "for the test response" should {

      "have the same values as the calculationDataSuccessModel" in {
        calculationDataSuccessModel.incomeTaxYTD.getOrElse("fail") shouldBe 1
        calculationDataSuccessModel.incomeTaxThisPeriod.getOrElse("fail") shouldBe 2000
        calculationDataSuccessModel.profitFromSelfEmployment.getOrElse("fail") shouldBe 200000
        calculationDataSuccessModel.profitFromUkLandAndProperty.getOrElse("fail") shouldBe 10000
        calculationDataSuccessModel.totalIncomeReceived.getOrElse("fail") shouldBe 230000
        calculationDataSuccessModel.personalAllowance.getOrElse("fail") shouldBe 11500
        calculationDataSuccessModel.totalIncomeOnWhichTaxIsDue.getOrElse("fail") shouldBe 198500
        calculationDataSuccessModel.payPensionsProfitAtBRT.getOrElse("fail") shouldBe 20000
        calculationDataSuccessModel.incomeTaxOnPayPensionsProfitAtBRT.getOrElse("fail") shouldBe 4000
        calculationDataSuccessModel.payPensionsProfitAtHRT.getOrElse("fail") shouldBe 100000
        calculationDataSuccessModel.incomeTaxOnPayPensionsProfitAtHRT.getOrElse("fail") shouldBe 40000
        calculationDataSuccessModel.payPensionsProfitAtART.getOrElse("fail") shouldBe 50000
        calculationDataSuccessModel.incomeTaxOnPayPensionsProfitAtART.getOrElse("fail") shouldBe 22500
        calculationDataSuccessModel.incomeTaxDue.getOrElse("fail") shouldBe 66500
        calculationDataSuccessModel.nicTotal.getOrElse("fail") shouldBe 24000
        calculationDataSuccessModel.rateBRT.getOrElse("fail") shouldBe 20
        calculationDataSuccessModel.rateHRT.getOrElse("fail") shouldBe 40
        calculationDataSuccessModel.rateART.getOrElse("fail") shouldBe 45
      }

    }

    "be formatted to JSON correctly" in {
      Json.toJson[CalculationDataModel](calculationDataSuccessModel) shouldBe calculationDataSuccessJson
    }

    "be able to parse a JSON to string into the Model" in {
      Json.parse(calculationDataSuccessString).as[CalculationDataModel] shouldBe calculationDataSuccessModel
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
