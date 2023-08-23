/*
 * Copyright 2023 HM Revenue & Customs
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

import models.repaymentHistory.RepaymentHistoryModel
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json
import testConstants.RepaymentHistoryTestConstants.{repaymentHistoryOneRSI, repaymentHistoryTwoRSI, validRepaymentHistoryOneRSIJson, validRepaymentHistoryTwoRSIJson}
import testUtils.UnitSpec

class RepaymentHistoryResponseModelSpec extends UnitSpec with Matchers {

  "The RepaymentHistoryModel" should {

    "read from json" when {
      "the response has one repayment supplement item" in {
        Json.fromJson[RepaymentHistoryModel](validRepaymentHistoryOneRSIJson).fold(
          invalid => invalid,
          valid => valid) shouldBe RepaymentHistoryModel(List(repaymentHistoryOneRSI))
      }
      "the response has two repayment supplement items" in {
        Json.fromJson[RepaymentHistoryModel](validRepaymentHistoryTwoRSIJson).fold(
          invalid => invalid,
          valid => valid) shouldBe RepaymentHistoryModel(List(repaymentHistoryTwoRSI))
      }
    }

    "write to json" when {
      "the model has one repayment supplement item" in {
        Json.toJson[RepaymentHistoryModel](RepaymentHistoryModel(List(repaymentHistoryOneRSI))) shouldBe validRepaymentHistoryOneRSIJson
      }
      "the model has two repayment supplement items" in {
        Json.toJson[RepaymentHistoryModel](RepaymentHistoryModel(List(repaymentHistoryTwoRSI))) shouldBe validRepaymentHistoryTwoRSIJson
      }
    }
  }
}
