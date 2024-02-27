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

package testConstants

import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryStatus, RepaymentItem, RepaymentSupplementItem}
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

object RepaymentHistoryTestConstants {


  val validRepaymentHistoryOneRSIJson: JsValue = Json.obj(
    "repaymentsViewerDetails" ->
      Json.arr(
        Json.obj(
          "amountApprovedforRepayment" -> Some(100.0),
          "amountRequested" -> Some(200.0),
          "repaymentMethod" -> Some("BACD"),
          "totalRepaymentAmount" -> Some(300.0),
          "repaymentItems" -> Some(Json.arr(
            Json.obj(
              "repaymentSupplementItem" -> Json.arr(
                Json.obj(
                  "parentCreditReference" -> Some("002420002231"),
                  "amount" -> Some(400.0),
                  "fromDate" -> Some(LocalDate.parse("2021-07-23")),
                  "toDate" -> Some(LocalDate.parse("2021-08-23")),
                  "rate" -> Some(12.12)
                )
              )
            )
          )),
          "estimatedRepaymentDate" -> LocalDate.parse("2021-08-21"),
          "creationDate" -> LocalDate.parse("2021-07-21"),
          "repaymentRequestNumber" -> "000000003135",
          "status" -> "A"
        )
      )
  )

  val validRepaymentHistoryTwoRSIJson: JsValue = Json.obj(
    "repaymentsViewerDetails" ->
      Json.arr(
        Json.obj(
          "amountApprovedforRepayment" -> Some(100.0),
          "amountRequested" -> Some(200.0),
          "repaymentMethod" -> Some("BACD"),
          "totalRepaymentAmount" -> Some(300.0),
          "repaymentItems" -> Some(Json.arr(
            Json.obj(
              "repaymentSupplementItem" -> Json.arr(
                Json.obj(
                  "parentCreditReference" -> Some("002420002231"),
                  "amount" -> Some(400.0),
                  "fromDate" -> Some(LocalDate.parse("2021-07-23")),
                  "toDate" -> Some(LocalDate.parse("2021-08-23")),
                  "rate" -> Some(12.12)
                ),
                Json.obj(
                  "parentCreditReference" -> Some("002420002232"),
                  "amount" -> Some(500.0),
                  "fromDate" -> Some(LocalDate.parse("2021-07-24")),
                  "toDate" -> Some(LocalDate.parse("2021-08-24")),
                  "rate" -> Some(13.13)
                )
              )
            )
          )),
          "estimatedRepaymentDate" -> LocalDate.parse("2021-08-21"),
          "creationDate" -> LocalDate.parse("2021-07-21"),
          "repaymentRequestNumber" -> "000000003135",
          "status" -> "A"
        )
      )
  )


  val validMultipleRepaymentHistoryJson: JsValue = Json.obj(
    "repaymentsViewerDetails" ->
      Json.arr(
        Json.obj(
          "amountApprovedforRepayment" -> Some(100.0),
          "amountRequested" -> Some(200.0),
          "repaymentMethod" -> Some("BACD"),
          "totalRepaymentAmount" -> Some(300.0),
          "repaymentItems" -> Some(Json.arr(
            Json.obj(
              "repaymentSupplementItem" -> Json.arr(
                Json.obj(
                  "parentCreditReference" -> Some("002420002231"),
                  "amount" -> Some(400.0),
                  "fromDate" -> Some(LocalDate.parse("2021-07-23")),
                  "toDate" -> Some(LocalDate.parse("2021-08-23")),
                  "rate" -> Some(12.12)
                )
              )
            )
          )),
          "estimatedRepaymentDate" -> Some(LocalDate.parse("2021-08-21")),
          "creationDate" -> Some(LocalDate.parse("2021-07-21")),
          "repaymentRequestNumber" -> "000000003135",
          "status" -> "A"
        ),
        Json.obj(
          "amountApprovedforRepayment" -> Some(100.0),
          "amountRequested" -> Some(200.0),
          "repaymentMethod" -> Some("BACD"),
          "totalRepaymentAmount" -> Some(300.0),
          "repaymentItems" -> Some(Json.arr(
            Json.obj(
              "repaymentSupplementItem" -> Json.arr(
                Json.obj(
                  "parentCreditReference" -> Some("002420002231"),
                  "amount" -> Some(400.0),
                  "fromDate" -> Some(LocalDate.parse("2021-07-23")),
                  "toDate" -> Some(LocalDate.parse("2021-08-23")),
                  "rate" -> Some(12.12)
                )
              )
            )
          )),
          "estimatedRepaymentDate" -> Some(LocalDate.parse("2021-08-21")),
          "creationDate" -> Some(LocalDate.parse("2021-07-21")),
          "repaymentRequestNumber" -> "000000003135",
          "status" -> "A"
        )
      )
  )
  val repaymentHistoryOneRSI: RepaymentHistory = RepaymentHistory(
    amountApprovedforRepayment = Some(100.0),
    amountRequested = 200.0,
    repaymentMethod = Some("BACD"),
    totalRepaymentAmount = Some(300.0),
    repaymentItems = Some(Seq[RepaymentItem](
      RepaymentItem(
        repaymentSupplementItem = Seq(RepaymentSupplementItem(
          parentCreditReference = Some("002420002231"),
          amount = Some(400.0),
          fromDate = Some(LocalDate.parse("2021-07-23")),
          toDate = Some(LocalDate.parse("2021-08-23")),
          rate = Some(12.12)
        )
        )
      )
    )),
    estimatedRepaymentDate = Some(LocalDate.parse("2021-08-21")),
    creationDate = Some(LocalDate.parse("2021-07-21")),
    repaymentRequestNumber = "000000003135",
    status = RepaymentHistoryStatus("A")
  )

  val repaymentHistoryTwoRSI: RepaymentHistory = RepaymentHistory(
    amountApprovedforRepayment = Some(100.0),
    amountRequested = 200.0,
    repaymentMethod = Some("BACD"),
    totalRepaymentAmount = Some(300.0),
    repaymentItems = Some(Seq[RepaymentItem](
      RepaymentItem(
        repaymentSupplementItem = Seq(RepaymentSupplementItem(
          parentCreditReference = Some("002420002231"),
          amount = Some(400.0),
          fromDate = Some(LocalDate.parse("2021-07-23")),
          toDate = Some(LocalDate.parse("2021-08-23")),
          rate = Some(12.12)
        ),
          RepaymentSupplementItem(
            parentCreditReference = Some("002420002232"),
            amount = Some(500.0),
            fromDate = Some(LocalDate.parse("2021-07-24")),
            toDate = Some(LocalDate.parse("2021-08-24")),
            rate = Some(13.13)
          )

        )))),
    estimatedRepaymentDate = Some(LocalDate.parse("2021-08-21")),
    creationDate = Some(LocalDate.parse("2021-07-21")),
    repaymentRequestNumber = "000000003135",
    status = RepaymentHistoryStatus("A")
  )

}
