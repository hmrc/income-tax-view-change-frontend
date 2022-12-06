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

package utils

import models.financialDetails.MfaCreditUtils.validMFACreditType
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail}

object CreditAndRefundUtils {
  sealed trait UnallocatedCreditType

  object UnallocatedCreditType {
    case object UnallocatedCreditFromOnePayment extends UnallocatedCreditType

    case object UnallocatedCreditFromSingleCreditItem extends UnallocatedCreditType

    def maybeUnallocatedCreditType(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)],
                                   balanceDetails: Option[BalanceDetails]): Option[UnallocatedCreditType] = {
      (creditCharges, balanceDetails, creditCharges.size) match {
        case (List((_, financialDetails)), Some(BalanceDetails(_, _, _, Some(availableCredit), _, _, Some(_))), 1)
          if availableCredit != 0 && (validMFACreditType(financialDetails.mainType) || financialDetails.validCutoverCreditType()) =>
          Some(UnallocatedCreditFromSingleCreditItem)
        case (List((documentDetailWithDueDate, _)), Some(BalanceDetails(_, _, _, Some(availableCredit), _, _, Some(_))), 1)
          if availableCredit != 0 && documentDetailWithDueDate.documentDetail.paymentLot.isDefined && documentDetailWithDueDate.documentDetail.paymentLotItem.isDefined =>
          Some(UnallocatedCreditFromOnePayment)
        case _ => None
      }
    }
  }

}
