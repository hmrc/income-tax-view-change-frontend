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
import assets.BusinessDetailsTestConstants._
import assets.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import assets.PropertyDetailsTestConstants.propertyDetails
import auth.MtdItUser
import enums.{CalcStatus, Crystallised, Estimate}
import implicits.ImplicitDateFormatter
import models.calculation._
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import org.eclipse.jetty.server.Authentication.User
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.UnitSpec

class CalculationResponseModelSpec extends UnitSpec with Matchers with ImplicitDateFormatter{

  "The CalcDisplayModel" when {

    val calcDisplayModelTimestamp: String = LocalDateTime.now().toString
    val calcAmount: BigDecimal = 1

    def calcDisplayModelBBSInterestCalcStatus(calcStatus: CalcStatus, bankBuildingSocietyInterest: BigDecimal): CalcDisplayModel = {

      val incomeReceived: IncomeReceivedModel = IncomeReceivedModel(calculationDataSuccessModel.incomeReceived.selfEmployment,
        calculationDataSuccessModel.incomeReceived.ukProperty,
        bankBuildingSocietyInterest,
        calculationDataSuccessModel.incomeReceived.ukDividends)

      val calculationDataModel = CalculationDataModel(calculationDataSuccessModel.nationalRegime,
        calculationDataSuccessModel.totalIncomeTaxNicYtd,
        calculationDataSuccessModel.totalTaxableIncome: BigDecimal,
        calculationDataSuccessModel.personalAllowance: BigDecimal,
        calculationDataSuccessModel.taxReliefs: BigDecimal,
        calculationDataSuccessModel.totalIncomeAllowancesUsed: BigDecimal,
        incomeReceived: IncomeReceivedModel,
        calculationDataSuccessModel.savingsAndGains,
        calculationDataSuccessModel.dividends,
        calculationDataSuccessModel.nic,
        calculationDataSuccessModel.eoyEstimate,
        calculationDataSuccessModel.payAndPensionsProfitBands
      )

      val calcDataModel: Option[CalculationDataModel] = Some(calculationDataModel)

      CalcDisplayModel(calcDisplayModelTimestamp, calcAmount, calcDataModel, calcStatus)
    }

    def calcDisplayModelIncomeSourcesSA(selfEmployed: BigDecimal): CalculationDataModel = {

      val incomeReceived: IncomeReceivedModel = IncomeReceivedModel(selfEmployed,
        calculationDataSuccessModel.incomeReceived.ukProperty,
        calculationDataSuccessModel.incomeReceived.bankBuildingSocietyInterest,
        calculationDataSuccessModel.incomeReceived.ukDividends)

     CalculationDataModel(calculationDataSuccessModel.nationalRegime,
       calculationDataSuccessModel.totalIncomeTaxNicYtd,
       calculationDataSuccessModel.totalTaxableIncome: BigDecimal,
       calculationDataSuccessModel.personalAllowance: BigDecimal,
       calculationDataSuccessModel.taxReliefs: BigDecimal,
       calculationDataSuccessModel.totalIncomeAllowancesUsed: BigDecimal,
       incomeReceived: IncomeReceivedModel,
       calculationDataSuccessModel.savingsAndGains,
       calculationDataSuccessModel.dividends,
       calculationDataSuccessModel.nic,
       calculationDataSuccessModel.eoyEstimate,
       calculationDataSuccessModel.payAndPensionsProfitBands
     )

    }

    def calcDisplayModelIncomeSourcesProperty(property: BigDecimal): CalculationDataModel = {

      val incomeReceived: IncomeReceivedModel = IncomeReceivedModel(calculationDataSuccessModel.incomeReceived.selfEmployment,
        property,
        calculationDataSuccessModel.incomeReceived.bankBuildingSocietyInterest,
        calculationDataSuccessModel.incomeReceived.ukDividends)

      CalculationDataModel(calculationDataSuccessModel.nationalRegime,
        calculationDataSuccessModel.totalIncomeTaxNicYtd,
        calculationDataSuccessModel.totalTaxableIncome: BigDecimal,
        calculationDataSuccessModel.personalAllowance: BigDecimal,
        calculationDataSuccessModel.taxReliefs: BigDecimal,
        calculationDataSuccessModel.totalIncomeAllowancesUsed: BigDecimal,
        incomeReceived: IncomeReceivedModel,
        calculationDataSuccessModel.savingsAndGains,
        calculationDataSuccessModel.dividends,
        calculationDataSuccessModel.nic,
        calculationDataSuccessModel.eoyEstimate,
        calculationDataSuccessModel.payAndPensionsProfitBands
      )

    }


    val mockAccountingPeriod = AccountingPeriodModel(start = ("2017-6-1".toLocalDate), end = ("2018-5-30".toLocalDate))

    val businesses: BusinessDetailsModel = BusinessDetailsModel(
      incomeSourceId = testSelfEmploymentId,
      accountingPeriod = mockAccountingPeriod,
      cashOrAccruals = Some("CASH"),
      tradingStartDate = Some("2017-1-1"),
      cessation = None,
      tradingName = Some(testTradeName2),
      address = Some(testBizAddress),
      contactDetails = None,
      seasonal = None,
      paperless = None)

    val properties: PropertyDetailsModel = propertyDetails

    def mtdUser(businessIncome: List[BusinessDetailsModel], propertyIncome: Option[PropertyDetailsModel]): MtdItUser[_] = {

      val businessesAndPropertyIncome: IncomeSourceDetailsModel  = IncomeSourceDetailsModel(businessIncome, propertyIncome)

      MtdItUser(testMtditid, testNino, Some(testUserDetails), businessesAndPropertyIncome)(FakeRequest())
    }



    "displaying crystallisedWithBBSInterest as true" should  {
    "be crystallised and greater then zero" in {
      calcDisplayModelBBSInterestCalcStatus(Crystallised, 10).crystallisedWithBBSInterest shouldBe true
    }
  }

    "displaying crystallisedWithBBSInterest as false" should {
      "be estimate and greater then zero" in {
        calcDisplayModelBBSInterestCalcStatus(Estimate, 10).crystallisedWithBBSInterest shouldBe false
      }

      "be estimate and zero" in {
        calcDisplayModelBBSInterestCalcStatus(Estimate, 0).crystallisedWithBBSInterest shouldBe false
      }

      "be Crystallised and less then zero" in {
        calcDisplayModelBBSInterestCalcStatus(Crystallised, -1).crystallisedWithBBSInterest shouldBe false
      }

      "be crystallised and zero" in {
        calcDisplayModelBBSInterestCalcStatus(Crystallised, 0).crystallisedWithBBSInterest shouldBe false
      }
    }

    "displaying savingsAllowanceHeading" should {
      "show pa-estimates savings message" in{
        calcDisplayModelBBSInterestCalcStatus(Estimate, 10).savingsAllowanceHeading shouldBe ".pa-estimates-savings"
      }
      "show pa-bills saving message" in{
        calcDisplayModelBBSInterestCalcStatus(Crystallised, 10).savingsAllowanceHeading shouldBe ".pa-bills-savings"
      }

    }

    "display selfEmployedIncomeOrReceived as true" should {

      "have a self employed income source and be determined tax year is more then zero " in {
        CalcDisplayModel.selfEmployedIncomeOrReceived(mtdUser(List(businesses), None), 2019, calcDisplayModelIncomeSourcesSA(10)) shouldBe true
      }
       "have a self employed income source and be determined tax year is zero " in {
         CalcDisplayModel.selfEmployedIncomeOrReceived(mtdUser(List(businesses), None), 2019, calcDisplayModelIncomeSourcesSA(0)) shouldBe true
      }

      "have no self employed income source and be determined tax year is more then zero " in {
        CalcDisplayModel.selfEmployedIncomeOrReceived(mtdUser(List(), None), 2019, calcDisplayModelIncomeSourcesSA(10)) shouldBe true
      }
    }

    "display selfEmployedIncomeOrReceived as false" should {
      "have no self employed income source and be determined tax year is less then zero " in {
        CalcDisplayModel.selfEmployedIncomeOrReceived(mtdUser(List(), None), 2019, calcDisplayModelIncomeSourcesSA(-1)) shouldBe false
      }

      "have a self employed income source and be determined tax year does not match tax year and is less then zero  " in {
        CalcDisplayModel.selfEmployedIncomeOrReceived(mtdUser(List(businesses), None), 2018, calcDisplayModelIncomeSourcesSA(-1)) shouldBe false
      }

      "have a property income source and be determined tax year is less then zero " in {
        CalcDisplayModel.selfEmployedIncomeOrReceived(mtdUser(List(), Some(properties)), 2019, calcDisplayModelIncomeSourcesSA(-1)) shouldBe false
      }

    }

    "display propertyIncomeOrReceived as true" should {
      "have a property source and a property tax year matches current and be more then zero " in {
        CalcDisplayModel.propertyIncomeOrReceived(mtdUser(List(), Some(properties)), 2018, calcDisplayModelIncomeSourcesProperty(10) ) shouldBe true
      }
      "have a property source and a property tax year matches current and be less then zero " in {
        CalcDisplayModel.propertyIncomeOrReceived(mtdUser(List(), Some(properties)), 2018, calcDisplayModelIncomeSourcesProperty(-1)) shouldBe true
      }
      "not have a property source and tax year matches current and be more then zero" in {

        CalcDisplayModel.propertyIncomeOrReceived(mtdUser(List(), None), 2019 , calcDisplayModelIncomeSourcesProperty(10)) shouldBe true


      }
    }

    "display propertyIncomeOrReceived as false" should {
      "not have a property source and a property tax year matches current and be less than zero " in {
        CalcDisplayModel.propertyIncomeOrReceived(mtdUser(List(), None), 2019, calcDisplayModelIncomeSourcesProperty(-1) ) shouldBe false
      }

    "have a property source and a property tax year that does not match current and be equal than zero " in {
      CalcDisplayModel.propertyIncomeOrReceived(mtdUser(List(), Some(properties)), 2019, calcDisplayModelIncomeSourcesProperty(0) ) shouldBe false
    }
      "have a business income source and a property tax year that does  match current and be less than zero " in {
        CalcDisplayModel.propertyIncomeOrReceived(mtdUser(List(businesses), None), 2018, calcDisplayModelIncomeSourcesProperty(0) ) shouldBe false
      }
    }

    "display estimatedBankBuildingSocietyInterest as true" should {
      "have bank building society interest more then zero and calculation status is estimate" in {
        calcDisplayModelBBSInterestCalcStatus(Estimate, 10).estimatedWithBBSInterest shouldBe true
      }

    }

    "display estimatedBankBuildingSociety Interest as false" should {
      "have bank building society interest is zero and calculation status is estimate" in {
        calcDisplayModelBBSInterestCalcStatus(Estimate, 0).estimatedWithBBSInterest shouldBe false
      }
      "have bank building society interest more than zero and calculation status is not estimate" in {
        calcDisplayModelBBSInterestCalcStatus(Crystallised, 10).estimatedWithBBSInterest shouldBe false
      }
      "have bank building society interest less than zero and calculation status is not estimate" in {
        calcDisplayModelBBSInterestCalcStatus(Crystallised, -1).estimatedWithBBSInterest shouldBe false
      }


    }

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
