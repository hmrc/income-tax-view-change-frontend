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

import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import assets.TestConstants._
import assets.TestConstants.IncomeSourceDetails._
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.PropertyDetails._

class IncomeSourcesModelSpec extends UnitSpec with Matchers {

  "The IncomeSourceDetailsModel" when {
    "the user has both business and property income sources" should {
      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        bothIncomeSourceSuccessMisalignedTaxYear.businessDetails.get.selfEmploymentId shouldBe testSelfEmploymentId
      }
      s"have the business accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        bothIncomeSourceSuccessMisalignedTaxYear.businessDetails.get.accountingPeriod.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the business accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        bothIncomeSourceSuccessMisalignedTaxYear.businessDetails.get.accountingPeriod.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        bothIncomeSourceSuccessMisalignedTaxYear.businessDetails.get.tradingName shouldBe testTradeName
      }
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        bothIncomeSourceSuccessMisalignedTaxYear.propertyDetails.get.accountingPeriod.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        bothIncomeSourceSuccessMisalignedTaxYear.propertyDetails.get.accountingPeriod.end shouldBe testPropertyAccountingPeriod.end
      }
      s"return ${testPropertyAccountingPeriod.start} as the result for 'earliestAccountingPeriodStart'" in {
        bothIncomeSourceSuccessMisalignedTaxYear.earliestAccountingPeriodStart(2018) shouldBe Some(testPropertyAccountingPeriod.start)
      }
      s"return ${testPropertyAccountingPeriod.start} as the result for 'earliestAccountingPeriodStart' when both Accounting Periods have the same start" in {
        bothIncomeSourcesSuccessBusinessAligned.earliestAccountingPeriodStart(2018) shouldBe Some(testPropertyAccountingPeriod.start)
      }
    }

    "the user has just a business income source" should {
      s"have a business ID of $testSelfEmploymentId" in {
        businessIncomeSourceSuccess.businessDetails.get.selfEmploymentId shouldBe testSelfEmploymentId
      }
      s"have the business accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        businessIncomeSourceSuccess.businessDetails.get.accountingPeriod.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the business accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        businessIncomeSourceSuccess.businessDetails.get.accountingPeriod.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        businessIncomeSourceSuccess.businessDetails.get.tradingName shouldBe testTradeName
      }
      //Test Property details
      s"should not have property details" in {
        businessIncomeSourceSuccess.propertyDetails shouldBe None
      }
      s"return ${testBusinessAccountingPeriod.start} as the result for 'earliestAccountingPeriodStart'" in {
        businessIncomeSourceSuccess.earliestAccountingPeriodStart(2019) shouldBe Some(testBusinessAccountingPeriod.start)
      }
    }
    "the user has just a property income source" should {
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        propertyIncomeSourceSuccess.propertyDetails.get.accountingPeriod.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        propertyIncomeSourceSuccess.propertyDetails.get.accountingPeriod.end shouldBe testPropertyAccountingPeriod.end
      }
      //Test Business Details
      "should not have business details" in {
        propertyIncomeSourceSuccess.businessDetails shouldBe None
      }
      s"return ${testPropertyAccountingPeriod.start} as the result for 'earliestAccountingPeriodStart'" in {
        propertyIncomeSourceSuccess.earliestAccountingPeriodStart(2018) shouldBe Some(testPropertyAccountingPeriod.start)
      }
    }
    "the user has no income source" should {
      "return None for both business and property sources" in {
        noIncomeSourceSuccess.propertyDetails shouldBe None
        noIncomeSourceSuccess.businessDetails shouldBe None
      }
      s"return 'None' as the result for 'earliestAccountingPeriodStart'" in {
        noIncomeSourceSuccess.earliestAccountingPeriodStart(2018) shouldBe None
      }
    }
    "the user has both income sources, but only a business income source for 2019 then 'earliestAccountingPeriodStart'" should {
      "return the business income source accounting period start date" in {
        bothIncomeSourceSuccessMisalignedTaxYear.earliestAccountingPeriodStart(2019) shouldBe Some(testBusinessAccountingPeriod.start)
      }
    }
    "the 'earliestAccountingPeriodStart' should return 'None'" when {
      "Neither the property or business income sources match the tax year" in {
        bothIncomeSourceSuccessMisalignedTaxYear.earliestAccountingPeriodStart(2020) shouldBe None
      }
      "The property income source doesn't match the tax year" in {
        propertyIncomeSourceSuccess.earliestAccountingPeriodStart(2020) shouldBe None
      }
      "The business income source doesn't match the tax year" in {
        businessIncomeSourceSuccess.earliestAccountingPeriodStart(2020) shouldBe None
      }
    }
  }

}
