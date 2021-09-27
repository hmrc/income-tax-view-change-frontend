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

import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.core.AccountingPeriodModel
import models.paymentAllocationCharges.PaymentAllocationViewModel
import models.paymentAllocations.{AllocationDetail, PaymentAllocations}
import play.api.libs.json.{JsObject, JsValue, Json}

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

  private def paymentAllocationDetail(): JsObject = Json.obj(
    "paymentMadeDate" -> paymentAllocations.paymentAllocationChargeModel.financialDetails.head.items.get.head.dueDate,
    "paymentMadeAmount" -> paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails.head.originalAmount.get.abs.toString,
    "paymentAllocations" -> paymentAllocations.originalPaymentAllocationWithClearingDate.map {
        case (_, allocationDetail: Option[AllocationDetail], dateAllocated) =>
        Json.obj(
          "paymentAllocationDescription" -> allocationDetail.flatMap(_.getPaymentAllocationKeyInPaymentAllocations).get,
          "dateAllocated" -> dateAllocated.get,
          "amount" -> allocationDetail.flatMap {_.amount.toString }.get,
          "taxYear" -> allocationDetail.flatMap { _.to }.map(getTaxYearString).get
        )
      }
    )

  override val detail: JsValue = userAuditDetails(mtdItUser) ++ paymentAllocationDetail()

}
