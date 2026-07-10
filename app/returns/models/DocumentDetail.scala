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

package returns.models

import common.services.DateServiceInterface
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{Json, Reads, Writes, __}
import shared.enums.CodingOutType.*
import shared.enums.DocumentType
import shared.enums.DocumentType.*

import java.time.LocalDate

case class DocumentDetail(taxYear: Int,
                          transactionId: String,
                          documentDescription: Option[String],
                          documentText: Option[String],
                          outstandingAmount: BigDecimal,
                          originalAmount: BigDecimal,
                          documentDate: LocalDate,
                          interestOutstandingAmount: Option[BigDecimal] = None,
                          interestRate: Option[BigDecimal] = None,
                          latePaymentInterestId: Option[String] = None,
                          latePaymentInterestAmount: Option[BigDecimal] = None,
                          interestFromDate: Option[LocalDate] = None,
                          interestEndDate: Option[LocalDate] = None,
                          accruingInterestAmount: Option[BigDecimal] = None,
                          lpiWithDunningLock: Option[BigDecimal] = None,
                          paymentLotItem: Option[String] = None,
                          paymentLot: Option[String] = None,
                          effectiveDateOfPayment: Option[LocalDate] = None,
                          amountCodedOut: Option[BigDecimal] = None,
                          documentDueDate: Option[LocalDate] = None,
                          poaRelevantAmount: Option[BigDecimal] = None
                         ) {
  
  val isAccruingInterest: Boolean = accruingInterestAmount.exists(_ > 0)

  def isNotCodingOutDocumentDetail: Boolean = !isPayeSelfAssessment && !isCancelledPayeSelfAssessment

  def isPayeSelfAssessment: Boolean = (documentDescription, documentText) match {
    case (Some(TRMNewCharge.key) | Some(TRMAmendCharge.key), Some(CODING_OUT_ACCEPTED.name)) => true
    case _ => false
  }

  def isCancelledPayeSelfAssessment: Boolean = (documentDescription, documentText) match {
    case (Some(TRMNewCharge.key) | Some(TRMAmendCharge.key), Some(CODING_OUT_CANCELLED.name)) => true
    case _ => false
  }
  
  def isCodingOutFullyCollectedPoa(financialDetails: List[FinancialDetail]): Boolean = {
    financialDetails.exists { financialDetail =>
      financialDetail.items.exists { subItems =>
        subItems.exists { subItem =>
          subItem.codedOutStatus.contains(CODING_OUT_FULLY_COLLECTED.code)
        }
      }
    }
  }

  def getDueDate: Option[LocalDate] = {
    if (isAccruingInterest) {
      interestEndDate
    } else {
      documentDueDate
    }
  }

}


case class DocumentDetailWithDueDate(documentDetail: DocumentDetail, dueDate: Option[LocalDate],
                                     isLatePaymentInterest: Boolean = false, dunningLock: Boolean = false,
                                     isMFADebit: Boolean = false,
                                     isReviewAndReconcilePoaOneDebit: Boolean = false,
                                     isReviewAndReconcilePoaTwoDebit: Boolean = false
                                    )(implicit val dateService: DateServiceInterface)

object DocumentDetail {
  implicit val writes: Writes[DocumentDetail] = Json.writes[DocumentDetail]
  implicit val reads: Reads[DocumentDetail] = (
    (__ \ "taxYear").read[Int] and
      (__ \ "transactionId").read[String] and
      (__ \ "documentDescription").readNullable[String] and
      (__ \ "documentText").readNullable[String] and
      (__ \ "outstandingAmount").read[BigDecimal] and
      (__ \ "originalAmount").read[BigDecimal] and
      (__ \ "documentDate").read[LocalDate] and
      (__ \ "interestOutstandingAmount").readNullable[BigDecimal] and
      (__ \ "interestRate").readNullable[BigDecimal] and
      (__ \ "latePaymentInterestId").readNullable[String] and
      (__ \ "latePaymentInterestAmount").readNullable[BigDecimal] and
      (__ \ "interestFromDate").readNullable[LocalDate] and
      (__ \ "interestEndDate").readNullable[LocalDate] and
      (__ \ "accruingInterestAmount").readNullable[BigDecimal] and
      (__ \ "lpiWithDunningLock").readNullable[BigDecimal] and
      (__ \ "paymentLotItem").readNullable[String] and
      (__ \ "paymentLot").readNullable[String] and
      (__ \ "effectiveDateOfPayment").readNullable[LocalDate] and
      (__ \ "amountCodedOut").readNullable[BigDecimal] and
      (__ \ "documentDueDate").readNullable[LocalDate] and
      (__ \ "poaRelevantAmount").readNullable[BigDecimal]
    )(DocumentDetail.apply _)
}
