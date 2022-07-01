/*
 * Copyright 2022 HM Revenue & Customs
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

package models.repaymentHistory

import org.scalatest.Matchers
import testUtils.UnitSpec
import play.api.libs.json.{JsSuccess, JsValue, Json}

import java.time.LocalDate

class RepaymentHistorySpec extends UnitSpec with Matchers {

  val repaymentHistoryFull: RepaymentHistory = RepaymentHistory(
    amountApprovedforRepayment = Some(100.0),
    amountRequested = 200.0,
    repaymentMethod = "BACD",
    totalRepaymentAmount = 300.0,
    repaymentItems = Seq[RepaymentItem](
      RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002231"),
              amount = Some(400.0),
              fromDate = Some( LocalDate.parse("2021-07-23") ),
              toDate = Some( LocalDate.parse("2021-08-23") ),
              rate = Some(500.0)
            )
          )
      )
    ),
    estimatedRepaymentDate = LocalDate.parse("2021-08-21"),
    creationDate = LocalDate.parse("2021-07-21"),
    repaymentRequestNumber = "000000003135"
  )

  val repaymentHistoryFullJson: JsValue = Json.obj(
    "amountApprovedforRepayment" -> Some(100.0),
    "amountRequested" -> 200.0,
    "repaymentMethod" -> "BACD",
    "totalRepaymentAmount" -> 300.0,
    "repaymentItems" -> Json.arr(
      Json.obj(
        "repaymentSupplementItem" -> Json.arr(
          Json.obj(
            "parentCreditReference" -> Some("002420002231"),
            "amount" -> Some(400.0),
            "fromDate" -> Some( LocalDate.parse("2021-07-23") ),
            "toDate" -> Some( LocalDate.parse("2021-08-23") ),
            "rate" -> Some(500.0)
          )
        )
      )
    ),
    "estimatedRepaymentDate" -> LocalDate.parse("2021-08-21"),
    "creationDate" -> LocalDate.parse("2021-07-21"),
    "repaymentRequestNumber" -> "000000003135"
  )

  "RepaymentHistory" should {
    "write to Json" when {
      "the model has all details" in {
        Json.toJson(repaymentHistoryFull) shouldBe repaymentHistoryFullJson
      }

      "be able to parse a JSON into the Model" in {
        Json.fromJson[RepaymentHistory](repaymentHistoryFullJson) shouldBe JsSuccess(repaymentHistoryFull)
      }
    }
  }
}
