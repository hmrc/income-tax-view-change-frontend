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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import models.calculation.{CalculationDataErrorModel, CalculationDataModel, TaxBandModel}
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

        calculationDataSuccessModel.payAndPensionsProfitBands shouldBe List(
          TaxBandModel("BRT", 20.0, 20000.00, 4000.00),
          TaxBandModel("HRT", 40.0, 100000.00, 40000.00),
          TaxBandModel("ART", 45.0, 50000.00, 22500.00)
        )

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

        calculationDataSuccessModel.dividends.band(0).income shouldBe 1000
        calculationDataSuccessModel.dividends.band(0).rate shouldBe 7.5
        calculationDataSuccessModel.dividends.band(0).amount shouldBe 75

        calculationDataSuccessModel.dividends.band(1).income shouldBe 2000
        calculationDataSuccessModel.dividends.band(1).rate shouldBe 37.5
        calculationDataSuccessModel.dividends.band(1).amount shouldBe 750

        calculationDataSuccessModel.dividends.band(2).income shouldBe 3000
        calculationDataSuccessModel.dividends.band(2).rate shouldBe 38.1
        calculationDataSuccessModel.dividends.band(2).amount shouldBe 1143

        calculationDataSuccessModel.giftAid.paymentsMade shouldBe 0
        calculationDataSuccessModel.giftAid.rate shouldBe 0.0
        calculationDataSuccessModel.giftAid.taxableAmount shouldBe 0

        calculationDataSuccessModel.nic.class2 shouldBe 10000
        calculationDataSuccessModel.nic.class4 shouldBe 14000

        calculationDataSuccessModel.eoyEstimate.get.totalNicAmount shouldBe 66000
      }

    }

    "be formatted to JSON correctly" in {
      Json.toJson[CalculationDataModel](calculationDataSuccessModel) shouldBe calculationDataSuccessJson
    }

    "be able to parse a full JSON string into the Model" in {
      Json.fromJson[CalculationDataModel](calculationDataFullJson) shouldBe JsSuccess(calculationDataSuccessModel)
    }

    "If only mandatory values are returned from DES then defaulted others to 0" in {
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
