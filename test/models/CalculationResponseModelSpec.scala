/*
 * Copyright 2020 HM Revenue & Customs
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
import enums.{CalcStatus, Crystallised, Estimate}
import implicits.ImplicitDateFormatter
import models.calculation._
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class CalculationResponseModelSpec extends UnitSpec with Matchers with ImplicitDateFormatter {

  def calcDisplayModel(status: CalcStatus, interestTaxableIncome: BigDecimal): CalcDisplayModel = {
    val crystallised: Boolean = status match {
      case Estimate => false
      case Crystallised => true
    }

    CalcDisplayModel(
      testTimeStampString,
      1010.00,
      Calculation(crystallised = crystallised, savingsAndGains = SavingsAndGains(taxableIncome = Some(interestTaxableIncome))),
      status
    )
  }

  "The CalcDisplayModel" when {

    "displaying crystallisedWithBBSInterest as true" should  {
      "be crystallised and greater then zero" in {
        calcDisplayModel(Crystallised, 1010.00).crystallisedWithBBSInterest shouldBe true
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

    "displaying savingsAllowanceHeading" should {
      "show pa-estimates savings message" in{
        calcDisplayModel(Estimate, 10).savingsAllowanceHeading shouldBe ".pa-estimates-savings"
      }
      "show pa-bills saving message" in{
        calcDisplayModel(Crystallised, 10).savingsAllowanceHeading shouldBe ".pa-bills-savings"
      }

    }

    "display estimatedBankBuildingSocietyInterest as true" should {
      "have bank building society interest more then zero and calculation status is estimate" in {
        calcDisplayModel(Estimate, 10).estimatedWithBBSInterest shouldBe true
      }

    }

    "display estimatedBankBuildingSociety Interest as false" should {
      "have bank building society interest is zero and calculation status is estimate" in {
        calcDisplayModel(Estimate, 0).estimatedWithBBSInterest shouldBe false
      }
      "have bank building society interest more than zero and calculation status is not estimate" in {
        calcDisplayModel(Crystallised, 10).estimatedWithBBSInterest shouldBe false
      }
      "have bank building society interest less than zero and calculation status is not estimate" in {
        calcDisplayModel(Crystallised, -1).estimatedWithBBSInterest shouldBe false
      }


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

  "CalculationResponseModelWithYear" should {

    val calculation = Calculation(crystallised = true)
    val errorModel = CalculationResponseModelWithYear(CalculationErrorModel(500, "test"), 2020)
    val errorModelNotFound = CalculationResponseModelWithYear(CalculationErrorModel(404, "not found"), 2020)
    val calcModelCrystallised = CalculationResponseModelWithYear(calculation.copy(crystallised = true), 2020)
    val calcModelEstimate = CalculationResponseModelWithYear(calculation.copy(crystallised = false), 2020)

    "return true for isError" when {
      "the model it contains is of type CalculationErrorModel and the status is >= 500" in {
        errorModel.isError shouldBe true
      }
    }
    "return false for isError" when {
      "the model it contains is of type CalculationErrorModel and the status < 500" in {
        errorModelNotFound.isError shouldBe false
      }
    }
    "return false for isError" when {
      "the model it contains is not of type CalculationErrorModel" in {
        calcModelCrystallised.isError shouldBe false
      }
    }
    "return true for isCrystallised" when {
      "the model it contains is a CalculationModel and is Crystallised" in {
        calcModelCrystallised.isCrystallised shouldBe true
      }
    }
    "return false for isCrystallised" when {
      "the model it contains is a CalculationModel but is an Estimate" in {
        calcModelEstimate.isCrystallised shouldBe false
      }
      "the model it contains is not a CalculationModel" in {
        errorModel.isCrystallised shouldBe false
      }
    }
    "return true for notCrystallised" when {
      "the model it contains is a CalculationModel and is an Estimate" in {
        calcModelEstimate.notCrystallised shouldBe true
      }
    }
    "return false for notCrystallised" when {
      "the model it contains is a CalculationModel but it is Crystallised" in {
        calcModelCrystallised.notCrystallised shouldBe false
      }
      "the model it contains is a CalculationErrorModel" in {
        errorModel.notCrystallised shouldBe false
      }
    }
  }
}
