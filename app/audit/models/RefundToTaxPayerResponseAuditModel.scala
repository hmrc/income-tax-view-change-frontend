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

package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, TotalInterest}
import play.api.libs.json.{JsValue, Json}

import java.time.format.DateTimeFormatter
import scala.language.implicitConversions


case class RefundToTaxPayerResponseAuditModel(repaymentHistory: RepaymentHistoryModel)
                                             (implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.TransactionName.RefundToTaxPayer
  override val auditType: String = enums.AuditType.AuditType.RefundToTaxPayerResponse

  val repaymentHistoryItem: Option[RepaymentHistory] = repaymentHistory.repaymentsViewerDetails.headOption
  val repaymentInterestContent: Option[TotalInterest] = repaymentHistoryItem.flatMap(_.aggregate)
  val interestDescription: String = {
    val pattern = DateTimeFormatter.ofPattern("dd MMMM YYYY")
    val from = repaymentInterestContent.map(_.fromDate.format(pattern)).getOrElse("")
    val to = repaymentInterestContent.map(_.toDate.format(pattern)).getOrElse("")
    val rate = repaymentInterestContent.map(_.fromRate.toString()).getOrElse("")
    s"${from} to ${to} at ${rate}%"
  }

  val totalRefund: String = repaymentHistoryItem.flatMap(_.totalRepaymentAmount).map(_.toString).getOrElse("")
  val requestedOn: String = repaymentHistoryItem.flatMap(_.creationDate).map(_.toString).getOrElse("")
  val requestedAmount: String = repaymentHistoryItem.map(_.amountRequested).map(_.toString).getOrElse("")

  val repaymentHistoryDetail = Json.obj("estimatedDate" -> repaymentHistoryItem.map(_.estimatedRepaymentDate),
    "method" -> repaymentHistoryItem.map(_.repaymentMethod),
    "totalRefund" -> totalRefund,
    "requestedOn" -> requestedOn,
    "refundReference" -> repaymentHistoryItem.map(_.repaymentRequestNumber),
    "requestedAmount" -> requestedAmount,
    "refundAmount" -> repaymentHistoryItem.map(_.amountApprovedforRepayment.map(_.toString)),
    "interestAmount" -> repaymentInterestContent.map(_.total),
    "interestDescription" -> interestDescription)
  override val detail: JsValue = userAuditDetails(user) ++ repaymentHistoryDetail
}
