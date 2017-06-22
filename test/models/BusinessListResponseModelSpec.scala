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

import assets.TestConstants.BusinessDetails._
import assets.TestConstants._
import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class BusinessListResponseModelSpec extends UnitSpec with Matchers {

  "The BusinessListModel" should {

    "for the 1st Business" should {

      s"have the id set as $testSelfEmploymentId" in {
        businessesSuccessModel.business.head.id shouldBe testSelfEmploymentId
      }

      "when calling the method to detrmine the Tax Year to which the accounting periods" in {
        businessesSuccessModel.business.head.accountingPeriod.determineTaxYear shouldBe "2018"
      }
    }

    "for the 2nd Business" should {

      "have the id set as 5678" in {
        businessesSuccessModel.business.last.id shouldBe "5678"
      }

      "when calling the method to detrmine the Tax Year to which the accounting periods" in {
        businessesSuccessModel.business.head.accountingPeriod.determineTaxYear shouldBe "2018"
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson[BusinessDetailsModel](businessesSuccessModel) shouldBe businessSuccessJson
    }

    "be able to parse a JSON input as a string into the Model" in {
      Json.parse(businessSuccessString).as[BusinessDetailsModel] shouldBe businessesSuccessModel
    }
  }

  "The BusinessListError" should {

    "have the correct status code in the model" in {
      businessErrorModel.code shouldBe testErrorStatus
    }

    "have the correct Error Message in the model" in {
      businessErrorModel.message shouldBe testErrorMessage
    }

    "be formatted to JSON correctly" in {
      Json.toJson(businessErrorModel) shouldBe businessListErrorJson
    }

    "be able to parse a JSON to string into the Model" in {
      Json.parse(businessErrorString).as[BusinessDetailsErrorModel] shouldBe businessErrorModel
    }
  }

  "The AccountingPeriodModel Model" when {
    "the end date is before the Start of the next Tax Year" should {
      "return the current Tax Year" in {
        AccountingPeriodModel("2017-04-06", "2018-04-05").determineTaxYear shouldBe "2018"
      }
    }
    "the end date is on the Start of the next Tax Year" should {
      "return the next Tax Year" in {
        AccountingPeriodModel("2017-04-07", "2018-04-06").determineTaxYear shouldBe "2019"
      }
    }
    "the end date is after the Start of the next Tax Year" should {
      "return the next Tax Year" in {
        AccountingPeriodModel("2017-04-08", "2018-04-07").determineTaxYear shouldBe "2019"
      }
    }
  }
}