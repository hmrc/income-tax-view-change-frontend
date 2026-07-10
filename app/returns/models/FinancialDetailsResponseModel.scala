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

import common.models.incomeSourceDetails.TaxYear
import common.models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import common.services.DateServiceInterface
import play.api.Logger
import play.api.libs.json.{Format, Json}
import shared.enums.DocumentType.{Poa1Charge, Poa2Charge}
import shared.models.ReviewAndReconcileUtils.{isReviewAndReconcilePoaOne, isReviewAndReconcilePoaTwo}

import scala.util.{Failure, Success, Try}
import java.time.LocalDate

sealed trait FinancialDetailsResponseModel

// TODO-[1]: make balanceDetails private val -> apply re-design and fix test failures where needed
// TODO-[2]: make financialDetails private val -> ~
case class FinancialDetailsModel(balanceDetails: BalanceDetails,
                                 codingDetails: List[CodingDetails] = List(),
                                 documentDetails: List[DocumentDetail],
                                 financialDetails: List[FinancialDetail]) extends FinancialDetailsResponseModel {
  
  // TODO: method possibly is not required at all: TaxYearSummaryController -> withTaxYearFinancials
  def findDueDateByDocumentDetails(documentDetail: DocumentDetail): Option[LocalDate] = {
    financialDetails.find { fd =>
      fd.transactionId.contains(documentDetail.transactionId) &&
        fd.taxYear.toInt == documentDetail.taxYear
    } flatMap (_ => documentDetail.documentDueDate)
  }

  private final val POA1: String = Poa1Charge.key
  private final val POA2: String = Poa2Charge.key

  val poaDocumentDescriptions: List[String] = List(POA1, POA2)

  def arePoaPaymentsPresent(): Option[TaxYear] = {
    documentDetails.filter(_.documentDescription.exists(description => poaDocumentDescriptions.contains(description)))
      .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
  }

  def toChargeItem: List[ChargeItem] = {
    Try {
      this.documentDetails
        .map(documentDetail =>
          ChargeItem.fromDocumentPair(documentDetail, financialDetails)
        )
    } match {
      case Success(res) =>
        res
      case Failure(ex) =>
        Logger("application").warn(s"Failed conversion - toChargeItem - ${ex.getMessage}")
        List[ChargeItem]()
    }
  }

  def dunningLockExists(documentId: String): Boolean = {
    documentDetails.filter(_.transactionId == documentId)
      .exists { documentDetail =>
        financialDetails.exists(financialDetail => financialDetail.transactionId.contains(documentDetail.transactionId) && financialDetail.dunningLockExists)
      }
  }

  def isMFADebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && MfaDebitUtils.isMFADebitMainType(fd.mainType)
    }
  }

  def getAllDocumentDetailsWithDueDates()(implicit dateService: DateServiceInterface): List[DocumentDetailWithDueDate] = {
    documentDetails.map(documentDetail =>
      DocumentDetailWithDueDate(documentDetail, documentDetail.getDueDate,
        documentDetail.isAccruingInterest, dunningLockExists(documentDetail.transactionId),
        isMFADebit = isMFADebit(documentDetail.transactionId),
        isReviewAndReconcilePoaOneDebit = isReviewAndReconcilePoaOneDebit(documentDetail.transactionId),
        isReviewAndReconcilePoaTwoDebit = isReviewAndReconcilePoaTwoDebit(documentDetail.transactionId)))
  }

  // TODO: we need to identify this on the chargeItem level -> mark as deprecated
  def isReviewAndReconcilePoaOneDebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && isReviewAndReconcilePoaOne(fd.mainTransaction)
    }
  }

  // TODO: we need to identify this on the chargeItem level -> mark as deprecated
  def isReviewAndReconcilePoaTwoDebit(documentId: String): Boolean = {
    financialDetails.exists { fd =>
      fd.transactionId.contains(documentId) && isReviewAndReconcilePoaTwo(fd.mainTransaction)
    }
  }
}


object FinancialDetailsModel {
  implicit val format: Format[FinancialDetailsModel] = Json.format[FinancialDetailsModel]

  def getDueDateForFinancialDetail(financialDetail: FinancialDetail): Option[LocalDate] = {
    financialDetail.items.flatMap(_.headOption.flatMap(_.dueDate))
  }
}

case class FinancialDetailsErrorModel(code: Int, message: String) extends FinancialDetailsResponseModel

object FinancialDetailsErrorModel {
  implicit val format: Format[FinancialDetailsErrorModel] = Json.format[FinancialDetailsErrorModel]
}
