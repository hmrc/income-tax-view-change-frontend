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
import models.paymentAllocationCharges.{PaymentAllocationChargesModel, PaymentAllocationViewModel}
import models.paymentAllocations.PaymentAllocations
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

    incomeTaxViewChangeConnector.getPaymentAllocation(nino, documentNumber) flatMap {
      case paymentAllocationsChargesModel: PaymentAllocationChargesModel =>
        incomeTaxViewChangeConnector.getPaymentAllocations(paymentAllocationsChargesModel.documentDetails.head.paymentLot.get,
          paymentAllocationsChargesModel.documentDetails.head.paymentLotItem.get) flatMap {
          case paymentAllocations: PaymentAllocations => createPaymentAllocationWithClearingDate(nino, paymentAllocations) map {
            case paymentAllocationWithClearingDate: Seq[(PaymentAllocations, Option[String])] if paymentAllocationWithClearingDate.find(_._2 == None) == None =>
              Right(PaymentAllocationViewModel(paymentAllocationsChargesModel, paymentAllocationWithClearingDate))
            case _ => Left(PaymentAllocationError)
          }
          case _ => Future.successful(Left(PaymentAllocationError))
        }
      case _ => Future.successful(Left(PaymentAllocationError))
    }
  }


  def createPaymentAllocationWithClearingDate(nino: String, paymentCharge: PaymentAllocations)
                                             (implicit hc: HeaderCarrier): Future[Seq[(PaymentAllocations, Option[String])]] = {
    Future.sequence(paymentCharge.allocations map {
      allocations =>
        incomeTaxViewChangeConnector.getPaymentAllocation(nino, allocations.transactionId.get) map {
          case paymentAllocationChargesModel: PaymentAllocationChargesModel =>
            (paymentCharge, paymentAllocationChargesModel.paymentDetails.head.items.get.head.clearingDate)
          case _ => (paymentCharge, None)
        }
    })
  }
}

object PaymentAllocationsService {

  case object PaymentAllocationError

}


