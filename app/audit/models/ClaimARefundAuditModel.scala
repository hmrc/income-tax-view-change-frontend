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
import enums.AuditType.ClaimARefundResponse
import enums.TransactionName.ClaimARefund
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class ClaimARefundAuditModel(balanceDetails: Option[BalanceDetails],
                                  creditDocuments: List[(DocumentDetailWithDueDate, FinancialDetail)])(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val auditType: String = ClaimARefundResponse
  override val transactionName: String = ClaimARefund

  private def getFullDueDate(dueDate: LocalDate) = s"${dueDate.format(DateTimeFormatter.ofPattern("dd MMMM YYYY"))}" // returns "17 January 2021"

  private def getAvailableCredit: Double = {
    balanceDetails.flatMap(_.availableCredit) match {
      case Some(_) => balanceDetails.get.availableCredit.get.toDouble.abs
      case (_) => 0.00
    }
  }

  private def getCreditType(credit: (DocumentDetailWithDueDate, FinancialDetail)): String = {
    val isMFA: Boolean = credit._2.isMFACredit
    val isCutOverCredit: Boolean = credit._2.isCutOverCredit
    val isPayment: Boolean = credit._1.documentDetail.paymentLot.isDefined

    (isMFA, isCutOverCredit, isPayment) match {
      case (true, _, _) => "Credit from HMRC adjustment"
      case (_, true, _) => "Credit from an earlier tax year"
      case (_, _, true) => s"Payment made on ${getFullDueDate(credit._1.dueDate.get)}"
      case error =>
        Logger("application").error(s"[ClaimARefundAuditModel][getCreditType] Missing or non-matching credit: $error found")
        "unknownCredit"
    }
  }

  private lazy val creditDocumentsJson: Seq[JsObject] = {
    creditDocuments.map(credit => Json.obj(
      ("description" -> getCreditType(credit)),
      ("amount" -> credit._1.documentDetail.paymentOrChargeCredit)))
  }

  private def getPendingRefundsJson(pendingRefund: Option[BigDecimal]): Seq[JsObject] = Seq(Json.obj() ++ Json.obj("description" -> "Refund in progress") ++
    Json.obj("amount" -> pendingRefund.get.abs))

  private lazy val refundDocumentsJson: Seq[JsObject] = {
    val firstPendingRefundExists = balanceDetails.map(balanceDetails => balanceDetails.firstPendingAmountRequested.isDefined)
    val secondPendingRefundExists = balanceDetails.map(balanceDetails => balanceDetails.secondPendingAmountRequested.isDefined)

    (firstPendingRefundExists, secondPendingRefundExists) match {
      case (Some(true), Some(true)) =>
        getPendingRefundsJson(balanceDetails.get.firstPendingAmountRequested) ++ getPendingRefundsJson(balanceDetails.get.secondPendingAmountRequested)
      case (Some(false), Some(true)) =>
        getPendingRefundsJson(balanceDetails.get.secondPendingAmountRequested)
      case (Some(true), Some(false)) =>
        getPendingRefundsJson(balanceDetails.get.firstPendingAmountRequested)
      case (_, _) => Seq()
    }
  }

  val claimARefundDetail: JsObject = {
    Json.obj(
      "creditOnAccount" -> getAvailableCredit,
      "creditDocuments" -> creditDocumentsJson,
      "refundDocuments" -> refundDocumentsJson)
  }
  override val detail: JsValue = userAuditDetails(user) ++ claimARefundDetail
}
