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

import implicits.ImplicitCurrencyFormatter._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.paymentAllocationCharges.PaymentAllocationViewModel
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities.JsonUtil

case class PaymentAllocationResponseAuditModel(mtdItUser: MtdItUser[_],
                                               paymentAllocations: PaymentAllocationViewModel)
                                              (dateFormatter: ImplicitDateFormatterImpl) extends ExtendedAuditModel with ImplicitDateFormatter {

  override val transactionName: String = "payment-allocations-response"
  override val auditType: String = "PaymentAllocationsResponse"
//  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  private def getTaxYearString(taxYear:Int): String =
    s"${taxYear - 1} to $taxYear"

  private def paymentAllocationDetail(): JsObject = Json.obj(
    "paymentMadeDate" -> paymentAllocations.paymentAllocationChargeModel.financialDetails.head.items.get.head.dueDate,
    "paymentMadeAmount" -> paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails.head.originalAmount.get.abs.toString,
    "paymentAllocations" -> Json.arr(paymentAllocations.originalPaymentAllocationWithClearingDate.map((paymentAllocation, _, _) => Json.obj(
      "paymentAllocationDescription" -> paymentAllocation._2.get.getPaymentAllocationKeyInPaymentAllocations,
      "dateAllocated" -> paymentAllocation._3.get,
      "amount" -> paymentAllocation._2.get.amount.get.toString,
      "taxYear" -> getTaxYearString(paymentAllocation._2.get.getTaxYear(dateFormatter)))
    )))

//  ("paymentMadeDate", paymentAllocations.paymentAllocationChargeModel.financialDetails.head.items.get.head.dueDate.get.toLocalDate.toLongDate) ++
//    ("paymentMadeAmount", paymentAllocations.paymentAllocationChargeModel.filteredDocumentDetails.head.originalAmount.get.abs.toCurrencyString) ++
//    ("paymentAllocations", Json.arr(paymentAllocations.originalPaymentAllocationWithClearingDate.map((paymentAllocation, _, _) => Json.obj() ++
//      ("paymentAllocationDescription", paymentAllocation._2.get.getPaymentAllocationKeyInPaymentAllocations) ++
//      ("dateAllocated", paymentAllocation._3.get.toLocalDate.toLongDateShort) ++
//      ("amount", paymentAllocation._2.get.amount.get.toCurrencyString) ++
//      ("taxYear", getTaxYearString(paymentAllocation._2.get.getTaxYear(implicitDateFormatter)))
//    )))

  override val detail: JsValue = userAuditDetails(mtdItUser) ++
    Json.obj("paymentAllocation" -> paymentAllocationDetail)

}
