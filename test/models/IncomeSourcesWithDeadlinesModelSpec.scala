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
import assets.BusinessDetailsTestConstants._
import assets.IncomeSourcesWithDeadlinesTestConstants._
import assets.PropertyDetailsTestConstants._
import assets.ReportDeadlinesTestConstants.{obligations4xxDataErrorModel, obligationsDataErrorModel}
import models.incomeSourcesWithDeadlines.{BusinessIncomeWithDeadlinesModel, IncomeSourcesWithDeadlinesModel, PropertyIncomeWithDeadlinesModel}
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class IncomeSourcesWithDeadlinesModelSpec extends UnitSpec with Matchers {

  "The IncomeSourceDetailsWithDeadlinesModel" when {
    "the user has both businesses and property income sources" should {
      //Test Business details
      s"have a business ID of $testSelfEmploymentId" in {
        businessAndPropertyIncomeWithDeadlines.businessIncomeSources.head.incomeSource.incomeSourceId shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        businessAndPropertyIncomeWithDeadlines.businessIncomeSources.head.incomeSource.accountingPeriod.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        businessAndPropertyIncomeWithDeadlines.businessIncomeSources.head.incomeSource.accountingPeriod.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        businessAndPropertyIncomeWithDeadlines.businessIncomeSources.head.incomeSource.tradingName.get shouldBe testTradeName
      }
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        businessAndPropertyIncomeWithDeadlines.propertyIncomeSource.get.incomeSource.accountingPeriod.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        businessAndPropertyIncomeWithDeadlines.propertyIncomeSource.get.incomeSource.accountingPeriod.end shouldBe testPropertyAccountingPeriod.end
      }
    }

    "the user has just a business income source" should {
      s"have a business ID of $testSelfEmploymentId" in {
        singleBusinessIncomeWithDeadlines.businessIncomeSources.head.incomeSource.incomeSourceId shouldBe testSelfEmploymentId
      }
      s"have the businesses accounting period start date of ${testBusinessAccountingPeriod.start}" in {
        singleBusinessIncomeWithDeadlines.businessIncomeSources.head.incomeSource.accountingPeriod.start shouldBe testBusinessAccountingPeriod.start
      }
      s"have the businesses accounting period end date of ${testBusinessAccountingPeriod.end}" in {
        singleBusinessIncomeWithDeadlines.businessIncomeSources.head.incomeSource.accountingPeriod.end shouldBe testBusinessAccountingPeriod.end
      }
      s"should have the trading name of 'Test Business'" in {
        singleBusinessIncomeWithDeadlines.businessIncomeSources.head.incomeSource.tradingName.get shouldBe testTradeName
      }
      //Test Property details
      s"should not have property details" in {
        singleBusinessIncomeWithDeadlines.propertyIncomeSource shouldBe None
      }
    }
    "the user has just a property income source" should {
      //Test Property details
      s"have the property accounting period start date of ${testPropertyAccountingPeriod.start}" in {
        propertyIncomeOnlyWithDeadlines.propertyIncomeSource.get.incomeSource.accountingPeriod.start shouldBe testPropertyAccountingPeriod.start
      }
      s"have the property accounting period end date of ${testPropertyAccountingPeriod.end}" in {
        propertyIncomeOnlyWithDeadlines.propertyIncomeSource.get.incomeSource.accountingPeriod.end shouldBe testPropertyAccountingPeriod.end
      }
      //Test Business Details
      "should not have business details" in {
        propertyIncomeOnlyWithDeadlines.businessIncomeSources shouldBe List.empty
      }
    }
    "the user has no income source" should {
      "return None for both business and property sources" in {
        noIncomeDetailsWithNoDeadlines.propertyIncomeSource shouldBe None
        noIncomeDetailsWithNoDeadlines.businessIncomeSources shouldBe List.empty
      }
    }

    "the user has an error with a 5xx status in the business income sources" should {
      "return a true for hasAnyServerErrors" in {
        IncomeSourcesWithDeadlinesModel(List(BusinessIncomeWithDeadlinesModel(business1, obligationsDataErrorModel)),
          Some(PropertyIncomeWithDeadlinesModel(propertyDetails, obligationsDataSuccessModel))).hasAnyServerErrors shouldBe true
      }
    }
    "the user has an error with a 5xx status in the property income sources" should {
      "return a true for hasAnyServerErrors" in {
        IncomeSourcesWithDeadlinesModel(List(BusinessIncomeWithDeadlinesModel(business1, obligationsDataErrorModel)),
          Some(PropertyIncomeWithDeadlinesModel(propertyDetails, obligations4xxDataErrorModel))).hasAnyServerErrors shouldBe true
      }
    }
    "the user has an error with a 4xx status in the business income sources" should {
      "return a false for hasAnyServerErrors" in {
        IncomeSourcesWithDeadlinesModel(List(BusinessIncomeWithDeadlinesModel(business1, obligations4xxDataErrorModel)),
          Some(PropertyIncomeWithDeadlinesModel(propertyDetails, obligationsDataSuccessModel))).hasAnyServerErrors shouldBe false
      }
    }
    "the user has an error with a 4xx status in the property income sources" should {
      "return a false for hasAnyServerErrors" in {
        IncomeSourcesWithDeadlinesModel(List(BusinessIncomeWithDeadlinesModel(business1, obligationsDataSuccessModel)),
          Some(PropertyIncomeWithDeadlinesModel(propertyDetails, obligations4xxDataErrorModel))).hasAnyServerErrors shouldBe false
      }
    }
    "the user has no errors for any income sources" should {
      "return a false for hasAnyServerErrors" in {
        IncomeSourcesWithDeadlinesModel(List(BusinessIncomeWithDeadlinesModel(business1, obligationsDataSuccessModel)),
          Some(PropertyIncomeWithDeadlinesModel(propertyDetails, obligationsDataSuccessModel))).hasAnyServerErrors shouldBe false
      }
    }
    "the user has no income sources" should {
      "return a false for hasAnyServerErrors" in {
        IncomeSourcesWithDeadlinesModel(List.empty, None).hasAnyServerErrors shouldBe false
      }
    }
  }

}
