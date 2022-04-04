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

package services

import auth.MtdItUser
import config.FrontendAppConfig
import connectors.IncomeTaxViewChangeConnector
import models.core.Nino
import models.financialDetails.FinancialDetailsModel
import models.paymentAllocationCharges._
import models.paymentAllocations.PaymentAllocations
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                          financialDetailsService: FinancialDetailsService,
                                          val appConfig: FrontendAppConfig)
                                         (implicit ec: ExecutionContext) {

  def getPaymentAllocation(nino: Nino, documentNumber: String)
                          (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentAllocationError, PaymentAllocationViewModel]] = {

    incomeTaxViewChangeConnector.getFinancialDetailsByDocumentId(nino, documentNumber) flatMap {
      case documentDetailsWithFinancialDetailsModel: FinancialDetailsWithDocumentDetailsModel if documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLot.isEmpty && documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLotItem.isEmpty =>
        Future.successful(Right(PaymentAllocationViewModel(documentDetailsWithFinancialDetailsModel, Seq.empty)))
      case documentDetailsWithFinancialDetailsModel: FinancialDetailsWithDocumentDetailsModel =>
        incomeTaxViewChangeConnector.getPaymentAllocations(nino,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLot.get,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLotItem.get) flatMap {
          case paymentAllocations: PaymentAllocations =>
            if (paymentAllocations.allocations.exists(_.mainType.get.contains("Late Payment Interest"))) {
              createPaymentAllocationForLpi(paymentAllocations, documentDetailsWithFinancialDetailsModel) map { lpiPaymentAllocationDetails =>
                lpiPaymentAllocationDetails.map(lpiPaymentAllocationDetails =>
                  Right(PaymentAllocationViewModel(paymentAllocationChargeModel = documentDetailsWithFinancialDetailsModel,
                    latePaymentInterestPaymentAllocationDetails = Some(lpiPaymentAllocationDetails), isLpiPayment = true))).getOrElse(Left(PaymentAllocationError()))
              }
            } else {
              createPaymentAllocationWithClearingDate(nino, paymentAllocations, documentDetailsWithFinancialDetailsModel) map {
                case paymentAllocationWithClearingDate: Seq[AllocationDetailWithClearingDate]
                  if !paymentAllocationWithClearingDate.exists(_.allocationDetail.isEmpty) =>
                  Right(PaymentAllocationViewModel(documentDetailsWithFinancialDetailsModel, paymentAllocationWithClearingDate))
                case _ =>
                  Logger("application").error("[PaymentAllocationsService][getPaymentAllocation] Could not retrieve document with financial details for payment allocations")
                  Left(PaymentAllocationError())
              }
            }
          case _ =>
            Logger("application").error("[PaymentAllocationsService][getPaymentAllocation] Could not retrieve payment allocations with document details")
            Future.successful(Left(PaymentAllocationError()))
        }
      case paymentAllocation: FinancialDetailsWithDocumentDetailsErrorModel if paymentAllocation.code == 404 =>
        Logger("application").error("[PaymentAllocationsService][getPaymentAllocation] payment allocation could not be found")
        Future.successful(Left(PaymentAllocationError(Some(paymentAllocation.code))))
      case _ =>
        Logger("application").error("[PaymentAllocationsService][getPaymentAllocation] Could not retrieve document with financial details for payment charge model")
        Future.successful(Left(PaymentAllocationError()))
    }
  }

  private def createPaymentAllocationForLpi(paymentCharge: PaymentAllocations,
                                            documentDetailsWithFinancialDetails: FinancialDetailsWithDocumentDetailsModel)
                                           (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Option[LatePaymentInterestPaymentAllocationDetails]] = {
    financialDetailsService.getAllFinancialDetails.map { financialDetailsWithTaxYear =>
      financialDetailsWithTaxYear.flatMap {
        case (_, financialDetails: FinancialDetailsModel) =>
          financialDetails.documentDetails.find(_.latePaymentInterestId == paymentCharge.allocations.head.chargeReference).map {
            documentDetailsWithLpiId =>
              LatePaymentInterestPaymentAllocationDetails(documentDetailsWithLpiId,
                documentDetailsWithFinancialDetails.documentDetails.head.originalAmount.get)
          }
        case _ => None
      }.headOption
    }
  }

  private def createPaymentAllocationWithClearingDate(nino: Nino, paymentCharge: PaymentAllocations,
                                                      documentDetailsWithFinancialDetails: FinancialDetailsWithDocumentDetailsModel)
                                                     (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Seq[AllocationDetailWithClearingDate]] = {
    Future.sequence(
      paymentCharge.allocations map { allocation =>
        incomeTaxViewChangeConnector.getFinancialDetailsByDocumentId(nino, allocation.transactionId.get) map {
          case singleChargeModel: FinancialDetailsWithDocumentDetailsModel =>
            AllocationDetailWithClearingDate(Some(allocation),
              singleChargeModel.financialDetails.find(_.chargeType == allocation.chargeType).head.items.get
                .find(paymentAllocation =>
                  paymentAllocation.paymentLot == documentDetailsWithFinancialDetails.documentDetails.head.paymentLot
                    && paymentAllocation.paymentLotItem == documentDetailsWithFinancialDetails.documentDetails.head.paymentLotItem
                ).head.clearingDate
            )
          case _ => AllocationDetailWithClearingDate(None, None)
        }
      })
  }
}
