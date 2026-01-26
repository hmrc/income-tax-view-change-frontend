/*
 * Copyright 2024 HM Revenue & Customs
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

package services

import auth.MtdItUser
import connectors.ChargeHistoryConnector
import enums.ReversalReason.CreateReversalReason
import models.chargeHistory.*
import models.financialDetails.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeHistoryService @Inject()(chargeHistoryConnector: ChargeHistoryConnector) {

  def chargeHistoryResponse(isLatePaymentCharge: Boolean, chargeReference: Option[String],
                            isChargeHistoryEnabled: Boolean)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ChargesHistoryErrorModel, List[ChargeHistoryModel]]] = {
    if (!isLatePaymentCharge && isChargeHistoryEnabled) {
      chargeHistoryConnector.getChargeHistory(user.nino, chargeReference).map {
        case chargesHistory: ChargesHistoryModel => Right(chargesHistory.chargeHistoryDetails.getOrElse(Nil))
        case errorResponse: ChargesHistoryErrorModel => Left(errorResponse)
      }
    } else {
      Future.successful(Right(Nil))
    }
  }

  def getAdjustmentHistory(chargeHistory: List[ChargeHistoryModel], documentDetail: DocumentDetail): AdjustmentHistoryModel = {
    chargeHistory match {
      case Nil =>
        val creation = AdjustmentModel(amount = documentDetail.originalAmount,
          adjustmentDate = Some(documentDetail.documentDate), reasonCode = CreateReversalReason)
        AdjustmentHistoryModel(creation, List.empty)
      case _ =>
        val creation = AdjustmentModel(amount = chargeHistory.minBy(_.documentDate).totalAmount, adjustmentDate = Some(chargeHistory.minBy(_.documentDate).documentDate), reasonCode = CreateReversalReason)
        val adjustmentHistory: List[AdjustmentModel] = adjustments(chargeHistory.sortBy(_.reversalDate), documentDetail.originalAmount, documentDetail.documentDate)
        AdjustmentHistoryModel(creation, adjustmentHistory.sortBy(_.adjustmentDate))
    }
  }

  private def adjustments(chargeHistory: List[ChargeHistoryModel], finalAmount: BigDecimal, finalDate: LocalDate): List[AdjustmentModel] = {
    chargeHistory.foldRight(((finalAmount, finalDate), List.empty[AdjustmentModel])) { (current, acc) =>
      val (nextAmountAndDate, adjList) = acc
      val newAdjustment = AdjustmentModel(
        adjustmentDate = Some(nextAmountAndDate._2),
        reasonCode = current.reasonCode match {
          case Left(ex) => throw new Exception(ex)
          case Right(rc) => rc
        },
        amount = nextAmountAndDate._1
      )
      ((current.totalAmount, current.documentDate), newAdjustment :: adjList)
    } match {
      case (_, adjustmentModels) => adjustmentModels
    }
  }

  // TODO-[2]: we might need to move this function on the TransactionItem/ChargeItem level
  def getReviewAndReconcileCredit(targetChargeItem: ChargeItem,
                                  chargeDetailsForTaxYear: FinancialDetailsModel): Option[ChargeItem] = {
    chargeDetailsForTaxYear
      .asChargeItems
      .find { charge =>
        targetChargeItem.transactionType match {
          case PoaOneDebit => charge.transactionType == PoaOneReconciliationCredit
          case PoaTwoDebit => charge.transactionType == PoaTwoReconciliationCredit
          case _ => false
        }
      }
  }

}
