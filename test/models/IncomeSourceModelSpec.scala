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

class IncomeSourceModelSpec extends UnitSpec with Matchers {

  "The IncomeSourceDetailsModel" when {
    "the user has both business and property income sources" should {
      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        bothIncomeSourceSuccess.businessDetails.get.selfEmploymentId shouldBe testSelfEmploymentId
      }
      s"have the business accounting period start date of ${accountingPeriodModel.start}" in {
        bothIncomeSourceSuccess.businessDetails.get.accountingPeriod.start shouldBe accountingPeriodModel.start
      }
      s"have the business accounting period end date of ${accountingPeriodModel.end}" in {
        bothIncomeSourceSuccess.businessDetails.get.accountingPeriod.end shouldBe accountingPeriodModel.end
      }
      s"should have the trading name of 'Test Business'" in {
        bothIncomeSourceSuccess.businessDetails.get.tradingName shouldBe "Test Business"
      }
      //Test Property details
      s"have the property accounting period start date of ${accountingPeriodModel.start}" in {
        bothIncomeSourceSuccess.propertyDetails.get.accountingPeriod.start shouldBe accountingPeriodModel.start
      }
      s"have the property accounting period end date of ${accountingPeriodModel.end}" in {
        bothIncomeSourceSuccess.propertyDetails.get.accountingPeriod.end shouldBe accountingPeriodModel.end
      }
    }

    "the user has just a business income source" should {
      s"have a business ID of $testSelfEmploymentId" in {
        businessIncomeSourceSuccess.businessDetails.get.selfEmploymentId shouldBe testSelfEmploymentId
      }
      s"have the business accounting period start date of ${accountingPeriodModel.start}" in {
        businessIncomeSourceSuccess.businessDetails.get.accountingPeriod.start shouldBe accountingPeriodModel.start
      }
      s"have the business accounting period end date of ${accountingPeriodModel.end}" in {
        businessIncomeSourceSuccess.businessDetails.get.accountingPeriod.end shouldBe accountingPeriodModel.end
      }
      s"should have the trading name of 'Test Business'" in {
        businessIncomeSourceSuccess.businessDetails.get.tradingName shouldBe "Test Business"
      }
      //Test Property details
      s"should not have property details" in {
        businessIncomeSourceSuccess.propertyDetails shouldBe None
      }
    }
    "the user has just a property income source" should {
      //Test Property details
      s"have the property accounting period start date of ${accountingPeriodModel.start}" in {
        propertyIncomeSourceSuccess.propertyDetails.get.accountingPeriod.start shouldBe accountingPeriodModel.start
      }
      s"have the property accounting period end date of ${accountingPeriodModel.end}" in {
        propertyIncomeSourceSuccess.propertyDetails.get.accountingPeriod.end shouldBe accountingPeriodModel.end
      }
      //Test Business Details
      "should not have business details" in {
        propertyIncomeSourceSuccess.businessDetails shouldBe None
      }
    }
    "the user has no income source" should {
      "return None for both business and property sources" in {
        noIncomeSourceSuccess.propertyDetails shouldBe None
        noIncomeSourceSuccess.businessDetails shouldBe None
      }
    }
  }

}
