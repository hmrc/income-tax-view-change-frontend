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

package services

import config.FrontendAppConfig
import connectors.IncomeTaxViewChangeConnector
import models.core.Nino
import models.financialDetails.{FinancialDetail, SubItem}
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.{AllocationDetail, PaymentAllocations}
import play.api.Logger
import services.PaymentAllocationsService.PaymentAllocationError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                          val appConfig: FrontendAppConfig)
                                         (implicit ec: ExecutionContext) {

  def getPaymentAllocation(nino: Nino, documentNumber: String)
                          (implicit hc: HeaderCarrier): Future[Either[PaymentAllocationError.type, PaymentAllocationViewModel]] = {

    incomeTaxViewChangeConnector.getFinancialDetailsByDocumentId(nino, documentNumber) flatMap {
      case documentDetailsWithFinancialDetailsModel: FinancialDetailsWithDocumentDetailsModel =>
        incomeTaxViewChangeConnector.getPaymentAllocations(nino,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLot.get,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLotItem.get) flatMap {
          case paymentAllocations: PaymentAllocations =>
            createPaymentAllocationWithClearingDate(nino, paymentAllocations, documentDetailsWithFinancialDetailsModel) map {
              case paymentAllocationWithClearingDate: Seq[(PaymentAllocations, Option[AllocationDetail], Option[String])] if paymentAllocationWithClearingDate.find(_._2 == None).isEmpty =>
                Right(PaymentAllocationViewModel(documentDetailsWithFinancialDetailsModel, paymentAllocationWithClearingDate))
              case _ =>
                Logger.error("[PaymentAllocationsService][getPaymentAllocation] Could not retrieve document with financial details for payment allocations")
                Left(PaymentAllocationError)
            }
          case _ =>
            Logger.error("[PaymentAllocationsService][getPaymentAllocation] Could not retrieve payment allocations with document details")
            Future.successful(Left(PaymentAllocationError))
        }
      case _ =>
        Logger.error("[PaymentAllocationsService][getPaymentAllocation] Could not retrieve document with financial details for payment charge model")
        Future.successful(Left(PaymentAllocationError))
    }
  }


  private def createPaymentAllocationWithClearingDate(nino: Nino, paymentCharge: PaymentAllocations,
                                                      documentDetailsWithFinancialDetails: FinancialDetailsWithDocumentDetailsModel)
                                                     (implicit hc: HeaderCarrier): Future[Seq[(PaymentAllocations, Option[AllocationDetail], Option[String])]] = {
    Future.sequence(paymentCharge.allocations map {
      allocation =>
        incomeTaxViewChangeConnector.getFinancialDetailsByDocumentId(nino, allocation.transactionId.get) map {
          case singleChargeModel: FinancialDetailsWithDocumentDetailsModel =>
            (paymentCharge,
              Some(allocation),
              singleChargeModel.financialDetails.find(_.chargeType == allocation.chargeType).head.items.get
              .find(paymentAllocation =>
                paymentAllocation.paymentLot == documentDetailsWithFinancialDetails.documentDetails.head.paymentLot
                  && paymentAllocation.paymentLotItem == documentDetailsWithFinancialDetails.documentDetails.head.paymentLotItem
							).head.clearingDate
            )
          case _ => (paymentCharge,None, None)
        }
    })
  }
}

object PaymentAllocationsService {

  case object PaymentAllocationError
}
