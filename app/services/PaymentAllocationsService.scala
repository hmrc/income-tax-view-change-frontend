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

import auth.MtdItUser
import config.FrontendAppConfig
import connectors.IncomeTaxViewChangeConnector
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.PaymentAllocations
import play.api.Logger
import services.PaymentAllocationsService.PaymentAllocationError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                          val appConfig: FrontendAppConfig)
                                         (implicit ec: ExecutionContext) {

  def getPaymentAllocation(nino: String, documentNumber: String)
                          (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentAllocationError.type, PaymentAllocationViewModel]] = {

    incomeTaxViewChangeConnector.getFinancialDataWithDocumentDetails(nino, documentNumber) flatMap {
      case documentDetailsWithFinancialDetailsModel: FinancialDetailsWithDocumentDetailsModel =>
        incomeTaxViewChangeConnector.getPaymentAllocations(documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLot.get,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLotItem.get) flatMap {
          case paymentAllocations: PaymentAllocations =>
            createPaymentAllocationWithClearingDate(nino, paymentAllocations, documentDetailsWithFinancialDetailsModel) map {
            case paymentAllocationWithClearingDate: Seq[(PaymentAllocations, Option[String])] if paymentAllocationWithClearingDate.find(_._2 == None).isEmpty =>
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


  def createPaymentAllocationWithClearingDate(nino: String, paymentCharge: PaymentAllocations,
                                              documentDetailsWithFinancialDetails: FinancialDetailsWithDocumentDetailsModel)
                                             (implicit hc: HeaderCarrier): Future[Seq[(PaymentAllocations, Option[String])]] = {
    Future.sequence(paymentCharge.allocations map {
      allocations =>
        incomeTaxViewChangeConnector.getFinancialDataWithDocumentDetails(nino, allocations.transactionId.get) map {
          case paymentAllocationChargesModel: FinancialDetailsWithDocumentDetailsModel =>
            (paymentCharge, paymentAllocationChargesModel.paymentDetails.find(_.chargeType == paymentCharge.allocations.head.`type`).head.items.get
              .find(paymentAllocation =>
                paymentAllocation.paymentLot == documentDetailsWithFinancialDetails.documentDetails.head.paymentLot
                  && paymentAllocation.paymentLotItem == documentDetailsWithFinancialDetails.documentDetails.head.paymentLotItem
              ).head.clearingDate
            )
          case _ => (paymentCharge, None)
        }
    })
  }
}

object PaymentAllocationsService {

  case object PaymentAllocationError
}
