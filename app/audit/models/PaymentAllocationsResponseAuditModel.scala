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
import auth.MtdItUserBase
import models.core.AccountingPeriodModel
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities.JsonUtil

import java.time.LocalDate

case class PaymentAllocationsResponseAuditModel(mtdItUser: MtdItUserBase[_],
                                                paymentAllocations: PaymentAllocationViewModel)
  extends ExtendedAuditModel {

  override val transactionName: String = "payment-allocations-response"
  override val auditType: String = "PaymentAllocations"

  private def getTaxYearString(periodTo: String): String = {
    val taxYear = AccountingPeriodModel.determineTaxYearFromPeriodEnd(LocalDate.parse(periodTo))
    s"${taxYear - 1} to $taxYear"
  }

  private def getPaymentMadeAmount: Option[BigDecimal] = {
    paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails.head.originalAmount.map(_.abs)
  }

  private def getCreditOnAccount: Option[BigDecimal] = {
    paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails.head.outstandingAmount.flatMap {
      outstandingAmount => if (outstandingAmount != 0) Some(outstandingAmount.abs) else None
    }
  }

  private def getAllocationDescriptionFromKey(key: String): String = key match {
    case "paymentAllocation.paymentAllocations.poa1.incomeTax" => "Income Tax for payment on account 1 of 2"
    case "paymentAllocation.paymentAllocations.poa1.nic4" => "Class 4 National Insurance for payment on account 1 of 2"
    case "paymentAllocation.paymentAllocations.poa2.incomeTax" => "Income Tax for payment on account 2 of 2"
    case "paymentAllocation.paymentAllocations.poa2.nic4" => "Class 4 National Insurance for payment on account 2 of 2"
    case "paymentAllocation.paymentAllocations.bcd.incomeTax" => "Income Tax for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.nic2" => "Class 2 National Insurance for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.vcnic2" => "Voluntary Class 2 National Insurance for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.nic4" => "Class 4 National Insurance for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.sl" => "Student Loans for remaining balance"
    case "paymentAllocation.paymentAllocations.bcd.cgt" => "Capital Gains Tax for remaining balance"
    case "paymentOnAccount1.text" => "Late payment interest for payment on account 1 of 2"
    case "paymentOnAccount2.text" => "Late payment interest for payment on account 2 of 2"
    case "balancingCharge.text" => "Late payment interest for remaining balance"
  }

  private def paymentAllocationDetail(): JsObject = Json.obj() ++
    ("paymentMadeDate", paymentAllocations.paymentAllocationChargeModel.financialDetails.head.items.flatMap(_.head.dueDate)) ++
    ("paymentMadeAmount", getPaymentMadeAmount) ++
    paymentAllocationsAudit() ++
    ("creditOnAccount", getCreditOnAccount)

  private def paymentAllocationsAudit(): JsObject = {
    if (paymentAllocations.isLpiPayment) {
      Json.obj("paymentAllocations" -> Json.arr(
        paymentAllocations.latePaymentInterestPaymentAllocationDetails.map { lpiad =>
          Json.obj() ++
            ("paymentAllocationDescription", Some(getAllocationDescriptionFromKey(lpiad.documentDetail.getChargeTypeKey()))) ++
            ("amount", Some(lpiad.amount)) ++
            ("taxYear", Some(getTaxYearString(s"${lpiad.documentDetail.taxYear}-04-05")))
        }
      ))
    } else {
      Json.obj("paymentAllocations" -> paymentAllocations.originalPaymentAllocationWithClearingDate.map {
        case AllocationDetailWithClearingDate(allocationDetail: Option[AllocationDetail], dateAllocated) =>
          Json.obj() ++
            ("paymentAllocationDescription", allocationDetail.map(ad =>
              getAllocationDescriptionFromKey(ad.getPaymentAllocationKeyInPaymentAllocations))) ++
            ("dateAllocated", dateAllocated) ++
            ("amount", allocationDetail.flatMap{_.amount}) ++
            ("taxYear", allocationDetail.flatMap {_.to}.map(getTaxYearString))
      })
    }
  }

  override val detail: JsValue = userAuditDetails(mtdItUser) ++ paymentAllocationDetail()

}
