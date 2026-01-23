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
import models.core.AccountingPeriodModel
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}

import java.time.LocalDate


case class PaymentAllocationsResponseAuditModel(mtdItUser: MtdItUser[_],
                                                paymentAllocations: PaymentAllocationViewModel
                                                )
  extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.PaymentAllocations
  override val auditType: String = enums.AuditType.PaymentAllocations

  private def getTaxYearString(periodTo: LocalDate): String = {
    val taxYear = AccountingPeriodModel.determineTaxYearFromPeriodEnd(periodTo)
    s"${taxYear - 1} to $taxYear"
  }

  private def getPaymentMadeAmount: Option[BigDecimal] = {
    Option(paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails.headOption.map(_.originalAmount.abs)).flatten
  }

  private def getCreditOnAccount: Option[BigDecimal] = {
    paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails
      .headOption.map(_.outstandingAmount)
      .flatMap(outstandingAmount => if (outstandingAmount != 0) Option(outstandingAmount.abs) else None)
    }

  private def getAllocationDescriptionFromKey(key: String): String = key match {
    case "paymentAllocation.paymentAllocations.poa1.incomeTax" => "Income Tax for first payment on account"
    case "paymentAllocation.paymentAllocations.poa1.nic4" => "Class 4 National Insurance contributions for first payment on account"
    case "paymentAllocation.paymentAllocations.poa2.incomeTax" => "Income Tax for second payment on account"
    case "paymentAllocation.paymentAllocations.poa2.nic4" => "Class 4 National Insurance contributions for second payment on account"
    case "paymentAllocation.paymentAllocations.bcd.incomeTax" => "Income Tax for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.nic2" => "Class 2 National Insurance for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.vcnic2" => "Voluntary Class 2 National Insurance for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.nic4" => "Class 4 National Insurance for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.sl" => "Student Loans for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.cgt" => "Capital Gains Tax for remaining balance"
    case "paymentAllocation.paymentAllocations.hmrcAdjustment.text" => "HMRC adjustment"
    case "paymentOnAccount1.text" => "Late payment interest for first payment on account"
    case "paymentOnAccount2.text" => "Late payment interest for second payment on account"
    case "balancingCharge.text" => "Late payment interest for remaining balance"
    case other =>
      Logger("application").warn("key not found: " + other)
      "allocation"
  }

  private def paymentAllocationDetail(): JsObject = Json.obj() ++
    Json.obj("paymentMadeDate"-> paymentAllocations.paymentAllocationChargeModel.financialDetails.head.items.flatMap(_.head.dueDate)) ++
    Json.obj("paymentMadeAmount"-> getPaymentMadeAmount) ++
    paymentAllocationType() ++
    paymentAllocationsAudit() ++
    Json.obj("creditOnAccount"-> getCreditOnAccount)

  private def paymentAllocationsAudit(): JsObject = {
    if (paymentAllocations.isLpiPayment) {
      Json.obj("paymentAllocations" -> Json.arr(
        paymentAllocations.latePaymentInterestPaymentAllocationDetails.map { lpiad =>
          Json.obj() ++
            Json.obj("paymentAllocationDescription"-> Some(getAllocationDescriptionFromKey(lpiad.documentDetail.getChargeTypeKey))) ++
            Json.obj("amount"-> Some(lpiad.amount)) ++
            Json.obj("taxYear"-> Some(getTaxYearString(LocalDate.parse(s"${lpiad.documentDetail.taxYear}-04-05"))))
        }
      ))
    } else {
      Json.obj("paymentAllocations" -> paymentAllocations.originalPaymentAllocationWithClearingDate.map {
        case AllocationDetailWithClearingDate(allocationDetail: Option[AllocationDetail], dateAllocated) =>
          Json.obj() ++
            Json.obj("paymentAllocationDescription"-> allocationDetail.map(ad =>
              getAllocationDescriptionFromKey(ad.getPaymentAllocationKeyInPaymentAllocations))) ++
            Json.obj("dateAllocated"-> dateAllocated) ++
            Json.obj("amount"-> allocationDetail.flatMap {
              _.amount
            }) ++
            Json.obj("taxYear"-> allocationDetail.flatMap {
              _.to
            }.map(getTaxYearString))
      })
    }
  }
  private def paymentAllocationType(): JsObject = {
    if (paymentAllocations.paymentAllocationChargeModel.documentDetails.exists(_.credit.isDefined)) {
      Json.obj("paymentType" -> "Payment made from earlier tax year")
    }
    else {
      Json.obj("paymentType" -> "Payment made to HMRC")
    }
  }

  override val detail: JsValue = userAuditDetails(mtdItUser) ++ paymentAllocationDetail()

}
