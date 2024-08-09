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
import models.chargeHistory._
import models.financialDetails.DocumentDetail
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeHistoryService @Inject()(chargeHistoryConnector: ChargeHistoryConnector) {

  def chargeHistoryResponse(isLatePaymentCharge: Boolean, isPayeSelfAssessment: Boolean, chargeReference: Option[String],
                                    isChargeHistoryEnabled: Boolean, isCodingOutEnabled: Boolean)
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ChargesHistoryErrorModel, List[ChargeHistoryModel]]] = {
    if (!isLatePaymentCharge && isChargeHistoryEnabled && !(isCodingOutEnabled && isPayeSelfAssessment)) {
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
        val creation = AdjustmentModel(amount = documentDetail.originalAmount, adjustmentDate = Some(documentDetail.documentDate), reasonCode = "create")
        AdjustmentHistoryModel(creation, List.empty)
      case _ =>
        val creation = AdjustmentModel(amount = chargeHistory.minBy(_.documentDate).totalAmount, adjustmentDate = None, reasonCode = "create")
        val poaAdjustmentHistory: List[AdjustmentModel] = adjustments(chargeHistory.filter(_.poaAdjustmentReason.isDefined).sortBy(_.reversalDate), documentDetail.originalAmount)
        val otherAdjustmentHistory: List[AdjustmentModel] = chargeHistory.filter(_.poaAdjustmentReason.isEmpty).map(
          event => AdjustmentModel(event.totalAmount, Some(event.reversalDate), event.reasonCode)
        )
        val fullAdjustmentHistory: List[AdjustmentModel] = poaAdjustmentHistory ++ otherAdjustmentHistory
        AdjustmentHistoryModel(creation, fullAdjustmentHistory.sortBy(_.adjustmentDate))
    }
  }

  private def adjustments(chargeHistory: List[ChargeHistoryModel], finalAmount: BigDecimal): List[AdjustmentModel] = {
    chargeHistory.foldRight((finalAmount, List.empty[AdjustmentModel])) { (current, acc) =>
      val (nextAmount, adjList) = acc
      val newAdjustment = AdjustmentModel(
        adjustmentDate = Some(current.reversalDate),
        reasonCode = current.reasonCode,
        amount = nextAmount
      )
      (current.totalAmount, newAdjustment :: adjList)
    }._2
  }

}
