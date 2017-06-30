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

class IncomeSourceDetailsModelSpec extends UnitSpec with Matchers {

  "The IncomeSourceDetailsModel" when {
    "the user has both business and property income sources" should {
      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        incomeSourceDetails.businessDetails.selfEmploymentId shouldBe testSelfEmploymentId
      }
      s"have the business accounting period start date of ${accountingPeriodModel.start}" in {
        incomeSourceDetails.businessDetails.accountingPeriod.start shouldBe accountingPeriodModel.start
      }
      s"have the business accounting period end date of ${accountingPeriodModel.end}" in {
        incomeSourceDetails.businessDetails.accountingPeriod.end shouldBe accountingPeriodModel.end
      }
      s"should have the trading name of 'Test Business'" in {
        incomeSourceDetails.businessDetails.tradingName shouldBe "Test Business"
      }
      //Test Property details
      s"have the property accounting period start date of ${accountingPeriodModel.start}" in {
        incomeSourceDetails.propertyDetails.accountingPeriod.start shouldBe accountingPeriodModel.start
      }
      s"have the property accounting period end date of ${accountingPeriodModel.end}" in {
        incomeSourceDetails.propertyDetails.accountingPeriod.end shouldBe accountingPeriodModel.end
      }
    }

    "the user has just a business income source" should {

    }
    "the user has just a property income source" should {

    }
    "the user has no income source" should {

    }
  }

}
