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

package models.repaymentHistory

import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, JsValue, Json}
import testUtils.UnitSpec

import java.time.LocalDate

class RepaymentHistorySpec extends UnitSpec with Matchers {

  val repaymentHistoryOneRSI: RepaymentHistory = RepaymentHistory(
    amountApprovedforRepayment = Some(100.0),
    amountRequested = 200.0,
    repaymentMethod = Some("BACD"),
    totalRepaymentAmount = Some(300.0),
    repaymentItems = Some(
      Seq[RepaymentItem](
        RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002231"),
              amount = Some(400.0),
              fromDate = Some(LocalDate.parse("2021-07-23")),
              toDate = Some(LocalDate.parse("2021-08-23")),
              rate = Some(12.12)
            )
          )
        )
      )
    ),
    estimatedRepaymentDate = Some(LocalDate.parse("2021-08-21")),
    creationDate = Some(LocalDate.parse("2021-07-21")),
    repaymentRequestNumber = "000000003135",
    status = RepaymentHistoryStatus("A")
  )

  val repaymentHistoryOneRSIJson: JsValue = Json.obj(
    "amountApprovedforRepayment" -> Some(100.0),
    "amountRequested"            -> Some(200.0),
    "repaymentMethod"            -> Some("BACD"),
    "totalRepaymentAmount"       -> Some(300.0),
    "repaymentItems" -> Some(
      Json.arr(
        Json.obj(
          "repaymentSupplementItem" -> Json.arr(
            Json.obj(
              "parentCreditReference" -> Some("002420002231"),
              "amount"                -> Some(400.0),
              "fromDate"              -> Some(LocalDate.parse("2021-07-23")),
              "toDate"                -> Some(LocalDate.parse("2021-08-23")),
              "rate"                  -> Some(12.12)
            )
          )
        )
      )
    ),
    "estimatedRepaymentDate" -> Some(LocalDate.parse("2021-08-21")),
    "creationDate"           -> Some(LocalDate.parse("2021-07-21")),
    "repaymentRequestNumber" -> "000000003135",
    "status"                 -> "A"
  )

  "RepaymentHistory" should {
    "write to Json" when {
      "the model has all details" in {
        Json.toJson(repaymentHistoryOneRSI) shouldBe repaymentHistoryOneRSIJson
      }

      "be able to parse a JSON into the Model" in {
        Json.fromJson[RepaymentHistory](repaymentHistoryOneRSIJson) shouldBe JsSuccess(repaymentHistoryOneRSI)
      }
    }
  }

  "RepaymentHistoryStatus" should {
    "return Approved object" when {
      "called with 'A' indicator" in {
        RepaymentHistoryStatus("A").isInstanceOf[Approved] shouldBe true
      }
      "called with 'M' indicator" in {
        RepaymentHistoryStatus("M").isInstanceOf[Approved] shouldBe true
      }
    }

    "return SentForRisking object" in {
      RepaymentHistoryStatus("I") shouldBe SentForRisking
    }

    "return Rejected object" in {
      RepaymentHistoryStatus("C").isInstanceOf[Rejected] shouldBe true
      RepaymentHistoryStatus("Any other string").isInstanceOf[Rejected] shouldBe true
    }

    "have methods to check method of approval" in {
      RepaymentHistoryStatus("A").asInstanceOf[Approved].isApprovedByRisking shouldBe true
      RepaymentHistoryStatus("M").asInstanceOf[Approved].isApprovedManually shouldBe true
    }
  }
}
