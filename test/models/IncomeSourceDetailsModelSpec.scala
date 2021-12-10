/*
 * Copyright 2021 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants._
import testConstants.PropertyDetailsTestConstants._
import org.scalatest.Matchers
import testUtils.UnitSpec

class IncomeSourceDetailsModelSpec extends UnitSpec with Matchers {

  "The IncomeSourceDetailsModel" when {

    "the user has both businesses and property income sources" should {

      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        businessesAndPropertyIncome.businesses.head.incomeSourceId.get shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        businessesAndPropertyIncome.businesses.head.accountingPeriod.get.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        businessesAndPropertyIncome.businesses.head.accountingPeriod.get.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        businessesAndPropertyIncome.businesses.head.tradingName.get shouldBe testTradeName
      }
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        businessesAndPropertyIncome.property.get.accountingPeriod.get.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        businessesAndPropertyIncome.property.get.accountingPeriod.get.end shouldBe testPropertyAccountingPeriod.end
      }
    }

    "the user has just a business income source" should {
      s"have a business ID of $testSelfEmploymentId" in {
        singleBusinessIncome.businesses.head.incomeSourceId.get shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        singleBusinessIncome.businesses.head.accountingPeriod.get.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        singleBusinessIncome.businesses.head.accountingPeriod.get.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        singleBusinessIncome.businesses.head.tradingName.get shouldBe testTradeName
      }
      //Test Property details
      s"should not have property details" in {
        singleBusinessIncome.property shouldBe None
      }
    }
    "the user has just a property income source" should {
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        propertyIncomeOnly.property.get.accountingPeriod.get.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        propertyIncomeOnly.property.get.accountingPeriod.get.end shouldBe testPropertyAccountingPeriod.end
      }
      //Test Business Details
      "should not have business details" in {
        propertyIncomeOnly.businesses shouldBe List.empty
      }
    }
    "the user has no income source" should {
      "return None for both business and property sources" in {
        noIncomeDetails.property shouldBe None
        noIncomeDetails.businesses shouldBe List.empty
      }
    }
  }

}
