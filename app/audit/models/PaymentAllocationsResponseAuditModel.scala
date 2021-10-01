/*
 * Copyright 2021 HM Revenue & Customs
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
import models.paymentAllocationCharges.PaymentAllocationViewModel
import models.paymentAllocations.{AllocationDetail}
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities.JsonUtil

import java.time.LocalDate

case class PaymentAllocationsResponseAuditModel(mtdItUser: MtdItUser[_],
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

  private def paymentAllocationDetail(): JsObject = Json.obj() ++
    ("paymentMadeDate", paymentAllocations.paymentAllocationChargeModel.financialDetails.head.items.flatMap(_.head.dueDate)) ++
    ("paymentMadeAmount", getPaymentMadeAmount) ++
    Json.obj("paymentAllocations" -> paymentAllocations.originalPaymentAllocationWithClearingDate.map {
        case (_, allocationDetail: Option[AllocationDetail], dateAllocated) =>
          Json.obj() ++
            ("paymentAllocationDescription", allocationDetail.map(_.getPaymentAllocationKeyInPaymentAllocations)) ++
            ("dateAllocated", dateAllocated) ++
            ("amount", allocationDetail.flatMap { _.amount }) ++
            ("taxYear", allocationDetail.flatMap { _.to }.map(getTaxYearString))
      }
    ) ++ ("creditOnAccount", getCreditOnAccount)

  override val detail: JsValue = userAuditDetails(mtdItUser) ++ paymentAllocationDetail()

}
