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
import enums.AuditType.AuditType.ClaimARefundResponse
import enums.TransactionName.TransactionName.ClaimARefund
import models.creditsandrefunds.{CreditsModel, Transaction}
import models.financialDetails.*
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.language.implicitConversions

case class ClaimARefundAuditModel(creditsModel: CreditsModel)(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val auditType: String = ClaimARefundResponse
  override val transactionName: String = ClaimARefund

  private def getFullDueDate(dueDate: LocalDate) = s"${dueDate.format(DateTimeFormatter.ofPattern("dd MMMM YYYY"))}" // returns "17 January 2021"

  private def getAvailableCredit: Double = {
    creditsModel.availableCreditForRepayment.abs.toDouble
  }

  case class Credit(transaction: Transaction) {
    def creditType: Option[CreditType] = Some(transaction.transactionType)
    def isPayment: Boolean = transaction.transactionType == PaymentType
    def taxYearString: String = {
      transaction.taxYear
        .map(taxYear => s"${taxYear.startYear} to ${taxYear.endYear} tax year"
      ).getOrElse("")
    }

    def dueDate: Option[LocalDate] = transaction.dueDate
  }

  private def getCreditDescription(credit: Credit): String = {
    (credit.creditType, credit.dueDate) match {
      case (Some(MfaCreditType), _) => "Credit from HMRC adjustment"
      case (Some(CutOverCreditType), _) => "Credit from an earlier tax year"
      case (Some(BalancingChargeCreditType), _) => "Balancing charge credit"
      case (Some(PoaOneReconciliationCredit), _) => "First payment on account: credit from your tax return"
      case (Some(PoaTwoReconciliationCredit), _) => "Second payment on account: credit from your tax return"
      case (Some(ITSAReturnAmendmentCredit), _) => "Credit from your amended tax return"
      case (Some(RepaymentInterest), _) => s"Credit from repayment interest - ${credit.taxYearString}"
      case (_, Some(date)) if credit.isPayment => s"Payment made on ${getFullDueDate(date)}"
      case (_, None) if credit.isPayment =>
        Logger("application").error("Missing or non-matching credit: not a valid payment date")
        "unknownDate"
      case (_, _) =>
        Logger("application").error("Missing or non-matching credit: not a valid credit type")
        "unknownCredit"
    }
  }

  private lazy val creditDocumentsJson: Seq[JsObject] = {
    creditsModel.transactions.filterNot(_.transactionType == Repayment)
      .map(transaction => {
        val credit = Credit(transaction)
        Json.obj(
          "description" -> getCreditDescription(credit),
          "amount" -> transaction.amount
        )
      })
  }

  private def getPendingRefundsJson(pendingRefund: BigDecimal):JsObject =
    Json.obj(
      "description" -> "Refund in progress",
      "amount" -> pendingRefund.abs )

  private lazy val refundDocumentsJson: Seq[JsObject] =
    creditsModel.transactions
      .filter(_.transactionType == Repayment)
      .map(_.amount)
      .map(getPendingRefundsJson)

  val claimARefundDetail: JsObject = {
    Json.obj(
      "creditOnAccount" -> getAvailableCredit,
      "creditDocuments" -> creditDocumentsJson,
      "refundDocuments" -> refundDocumentsJson)
  }
  override val detail: JsValue = userAuditDetails(user) ++ claimARefundDetail
}
