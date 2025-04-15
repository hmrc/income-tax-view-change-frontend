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

package models.paymentAllocations

import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import models.core.AccountingPeriodModel
import models.financialDetails.FinancialDetail
import play.api.Logger
import play.api.libs.json.{Format, Json}

import java.time.LocalDate

case class AllocationDetail(transactionId: Option[String],
                            from: Option[LocalDate],
                            to: Option[LocalDate],
                            chargeType: Option[String],
                            mainType: Option[String],
                            amount: Option[BigDecimal],
                            clearedAmount: Option[BigDecimal],
                            chargeReference: Option[String]) {

  def getPaymentAllocationKeyInPaymentAllocations: String = {
    FinancialDetail.getMessageKeyByTypes(mainType, chargeType)
      .map(typesKey => s"paymentAllocation.paymentAllocations.$typesKey")
      .getOrElse {
        Logger("application").error(s"[PaymentAllocations] Non-matching document/charge found with main charge: $mainType and sub-charge: $chargeType")
        ""
      }
  }

  def getTaxYear(implicit implicitDateFormatter: ImplicitDateFormatter): Int = {

    AccountingPeriodModel.determineTaxYearFromPeriodEnd(
      to.getOrElse(throw new Exception("Missing tax period end date")))
  }

  def getTaxYearOpt(implicit implicitDateFormatter: ImplicitDateFormatter): Option[Int] = {
    to.map(AccountingPeriodModel.determineTaxYearFromPeriodEnd)
  }

  def transactionIdActual: String = transactionId.getOrElse(throw MissingFieldException("Document ID"))
  def amountActual: BigDecimal = amount.getOrElse(throw MissingFieldException("Payment Amount"))
}

object AllocationDetail {
  implicit val format: Format[AllocationDetail] = Json.format[AllocationDetail]
}