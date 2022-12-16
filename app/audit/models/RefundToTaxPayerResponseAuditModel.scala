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

package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, TotalInterest}
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import implicits.ImplicitDateFormatterImpl


case class RefundToTaxPayerResponseAuditModel(repaymentHistory: RepaymentHistoryModel, implicitDateFormatter: ImplicitDateFormatterImpl)
                                             (implicit user: MtdItUser[_], messages: Messages) extends ExtendedAuditModel {

  import implicitDateFormatter.longDate

  override val transactionName: String = enums.TransactionName.RefundToTaxPayer
  override val auditType: String = enums.AuditType.RefundToTaxPayerResponse

  val repaymentHistoryItem: Option[RepaymentHistory] = repaymentHistory.repaymentsViewerDetails.headOption
  val repaymentInterestContent: Option[TotalInterest] = repaymentHistoryItem.flatMap(_.aggregate)
  val interestDescription: String = {
    val from = repaymentInterestContent.map(_.fromDate.toLongDate).getOrElse("")
    val to = repaymentInterestContent.map(_.toDate.toLongDate).getOrElse("")
    val rate = repaymentInterestContent.map(_.fromRate.toString()).getOrElse("")
    messages("refund-to-taxpayer.tableHead.interest-value", from, to, rate)
  }

  val repaymentHistoryDetail = Json.obj("estimatedDate" -> repaymentHistoryItem.map(_.estimatedRepaymentDate),
    "method" -> repaymentHistoryItem.map(_.repaymentMethod),
    "totalRefund" -> repaymentHistoryItem.map(_.totalRepaymentAmount.toString),
    "requestedOn" -> repaymentHistoryItem.map(_.creationDate.toString),
    "refundReference" -> repaymentHistoryItem.map(_.repaymentRequestNumber),
    "requestedAmount" -> repaymentHistoryItem.map(_.amountRequested.toString),
    "refundAmount" -> repaymentHistoryItem.map(_.amountApprovedforRepayment.map(_.toString)),
    "interestAmount" -> repaymentInterestContent.map(_.total),
    "interestDescription" -> interestDescription)
  override val detail: JsValue = userAuditDetails(user) ++ repaymentHistoryDetail
}
