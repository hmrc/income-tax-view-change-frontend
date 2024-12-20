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

package services

import auth.MtdItUser
import config.FrontendAppConfig
import connectors.FinancialDetailsConnector
import models.core.Nino
import models.financialDetails.FinancialDetailsModel
import models.paymentAllocationCharges._
import models.paymentAllocations.PaymentAllocations
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsService @Inject()(financialDetailsConnector: FinancialDetailsConnector,
                                          financialDetailsService: FinancialDetailsService,
                                          val appConfig: FrontendAppConfig)
                                         (implicit ec: ExecutionContext) {
  def getPaymentAllocation(nino: Nino, documentNumber: String)
                          (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentAllocationError, PaymentAllocationViewModel]] = {

    val functionName = ""
    financialDetailsConnector.getFinancialDetailsByDocumentId(nino, documentNumber) flatMap {
      case docDetailsWithFDModel: FinancialDetailsWithDocumentDetailsModel
        if docDetailsWithFDModel.documentDetails.head.paymentLot.isEmpty &&
          docDetailsWithFDModel.documentDetails.head.paymentLotItem.isEmpty =>
        Future.successful(Right(PaymentAllocationViewModel(docDetailsWithFDModel, Seq.empty)))
      case documentDetailsWithFinancialDetailsModel: FinancialDetailsWithDocumentDetailsModel =>
        financialDetailsConnector.getPaymentAllocations(nino,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLot.get,
          documentDetailsWithFinancialDetailsModel.documentDetails.head.paymentLotItem.get) flatMap {
          case paymentAllocations: PaymentAllocations =>
            handlePaymentAllocations(paymentAllocations, documentDetailsWithFinancialDetailsModel)
          case _ =>
            Logger("application").error(s"$functionName Could not retrieve payment allocations with document details")
            Future.successful(Left(PaymentAllocationError()))
        }
      case paymentAllocation: FinancialDetailsWithDocumentDetailsErrorModel if paymentAllocation.code == 404 =>
        Logger("application").error(s"$functionName payment allocation could not be found")
        Future.successful(Left(PaymentAllocationError(Some(paymentAllocation.code))))
      case _ =>
        Logger("application").error(s"$functionName Could not retrieve document with financial details for payment charge model")
        Future.successful(Left(PaymentAllocationError()))
    }
  }

  private def handlePaymentAllocations(paymentAllocations: PaymentAllocations,
                                       documentDetailsWithFinancialDetailsModel: FinancialDetailsWithDocumentDetailsModel)
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_]):
  Future[Either[PaymentAllocationError, PaymentAllocationViewModel]] = {
    if (paymentAllocations.allocations.exists(_.mainType.get.contains("Late Payment Interest"))) {
      createPaymentAllocationForLpi(paymentAllocations, documentDetailsWithFinancialDetailsModel) map { lpiPaymentAllocationDetails =>
        lpiPaymentAllocationDetails.map(lpiPaymentAllocationDetails =>
          Right(PaymentAllocationViewModel(paymentAllocationChargeModel = documentDetailsWithFinancialDetailsModel,
            latePaymentInterestPaymentAllocationDetails = Some(lpiPaymentAllocationDetails), isLpiPayment = true))).getOrElse(Left(PaymentAllocationError()))
      }
    } else {
      val paymentAllocationWithClearingDate = paymentAllocations.allocations map { allocation =>
        AllocationDetailWithClearingDate(Some(allocation), paymentAllocations.transactionDate)
      }
      Future.successful(Right(PaymentAllocationViewModel(documentDetailsWithFinancialDetailsModel,
        paymentAllocationWithClearingDate)))
    }
  }

  private def createPaymentAllocationForLpi(paymentCharge: PaymentAllocations,
                                            documentDetailsWithFinancialDetails: FinancialDetailsWithDocumentDetailsModel)
                                           (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Option[LatePaymentInterestPaymentAllocationDetails]] = {
    financialDetailsService.getAllFinancialDetails.map { financialDetailsWithTaxYear =>
      financialDetailsWithTaxYear.flatMap {
        case (_, financialDetails: FinancialDetailsModel) =>
          financialDetails.documentDetailsWithLpiId(paymentCharge.allocations.head.chargeReference).map {
            documentDetailsWithLpiId =>
              LatePaymentInterestPaymentAllocationDetails(documentDetailsWithLpiId,
                documentDetailsWithFinancialDetails.documentDetails.head.originalAmount)
          }
        case _ => None
      }.headOption
    }
  }
}
