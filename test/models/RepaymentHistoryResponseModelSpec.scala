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

import common.testUtils.UnitSpec
import financials.models.repaymentHistory.{HipRepaymentHistoryResponse, RepaymentHistoryModel}
import financials.testConstants.RepaymentHistoryTestConstants.{repaymentHistoryOneRSI, repaymentHistoryTwoRSI, validRepaymentHistoryOneRSIJson, validRepaymentHistoryTwoRSIJson}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

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
      "the API5324 response is missing repaymentSupplementItem" in {
        val api5324Response = Json.parse(
          """
            |{
            |  "etmp_transaction_header": {
            |    "status": "OK",
            |    "processingDate": "2026-04-23T12:25:58Z"
            |  },
            |  "etmp_Response_Details": {
            |    "repaymentsViewerDetails": [
            |      {
            |        "repaymentRequestNumber": "000000004624",
            |        "actor": "Operator",
            |        "channel": "Manual Return",
            |        "status": "Approved",
            |        "amountRequested": 868.2,
            |        "amountApprovedforRepayment": 868.2,
            |        "totalRepaymentAmount": 868.2,
            |        "repaymentMethod": "BACS Payment out",
            |        "creationDate": "2026-04-02",
            |        "estimatedRepaymentDate": "2026-04-05",
            |        "repaymentItems": [
            |          {
            |            "creditItems": [
            |              {
            |                "creditReference": "XP002610256651",
            |                "creditChargeName": "SA Balancing Charge Credit",
            |                "amount": 868.2,
            |                "creationDate": "2026-04-02",
            |                "taxYear": "2025"
            |              }
            |            ],
            |            "creditReasons": [
            |              {
            |                "creditReference": "XP002610256651",
            |                "creditReason": "Excess Payment",
            |                "edp": "2027-01-31",
            |                "amount": 2486,
            |                "originalChargeReduced": "Income Tax Estimate",
            |                "amendmentDate": "2026-04-02",
            |                "taxYear": "2026"
            |              },
            |              {
            |                "creditReference": "XP002610256651",
            |                "creditReason": "Excess Payment",
            |                "edp": "2027-01-31",
            |                "amount": 145.8,
            |                "originalChargeReduced": "Income Tax Estimate",
            |                "amendmentDate": "2026-04-02",
            |                "taxYear": "2026"
            |              }
            |            ]
            |          }
            |        ]
            |      }
            |    ]
            |  }
            |}
            |""".stripMargin
        )

        Json.fromJson[HipRepaymentHistoryResponse](api5324Response).fold(
          invalid => invalid,
          valid => valid.etmp_Response_Details.repaymentsViewerDetails.head.repaymentItems.get.head.repaymentSupplementItem
        ) shouldBe Seq.empty
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
