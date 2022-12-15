package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import exceptions.MissingFieldException
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryModel, TotalInterest}
import play.api.i18n.Messages
import play.api.libs.json.{JsValue, Json}
import implicits.ImplicitDateFormatterImpl


case class RefundToTaxPayerAuditModel(repaymentHistory: RepaymentHistoryModel, implicitDateFormatter: ImplicitDateFormatterImpl)
                                     (implicit user: MtdItUser[_], messages: Messages) extends ExtendedAuditModel {

  import implicitDateFormatter.longDate

  override val transactionName: String = enums.TransactionName.RefundToTaxPayer
  override val auditType: String = enums.AuditType.RefundToTaxPayerResponse
  override val detail: JsValue = userAuditDetails(user) ++ repaymentHistoryDetail

  val repaymentHistoryItem: RepaymentHistory = repaymentHistory.repaymentsViewerDetails.headOption.getOrElse(throw MissingFieldException("Repayment History Item"))
  val repaymentInterestContent: TotalInterest = repaymentHistoryItem.aggregate.getOrElse(throw MissingFieldException("Total Interest Item"))
  val interestDescription: String = messages("refund-to-taxpayer.tableHead.interest-value", repaymentInterestContent.fromDate.toLongDate, repaymentInterestContent.toDate.toLongDate, repaymentInterestContent.fromRate)

  val repaymentHistoryDetail = Json.obj("estimatedDate" -> repaymentHistoryItem.estimatedRepaymentDate.toString,
    "method" -> repaymentHistoryItem.repaymentMethod,
    "totalRefund" -> repaymentHistoryItem.totalRepaymentAmount.toString,
    "requestedOn" -> repaymentHistoryItem.creationDate.toString,
    "refundReference" -> repaymentHistoryItem.repaymentRequestNumber,
    "requestedAmount" -> repaymentHistoryItem.amountRequested.toString,
    "refundAmount" -> repaymentHistoryItem.amountApprovedforRepayment.getOrElse(throw MissingFieldException("Refund Amount Item")).toString,
    "interestAmount" -> repaymentHistoryItem.aggregate.getOrElse(throw MissingFieldException("Total Interest Item")).total,
    "interestDescription" -> interestDescription)
}
