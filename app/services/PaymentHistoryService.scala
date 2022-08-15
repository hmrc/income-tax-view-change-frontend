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
import models.CreditDetailsModel
import models.core.Nino

import javax.inject.Inject
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, Payment, Payments, PaymentsError}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import models.repaymentHistory.{RepaymentHistory, RepaymentHistoryErrorModel, RepaymentHistoryModel}
import services.PaymentHistoryService.PaymentHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class PaymentHistoryService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector, val appConfig: FrontendAppConfig)
                                     (implicit ec: ExecutionContext) {

  def getPaymentHistory(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, List[Payment]]] = {

    val orderedTaxYears: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration.reverse.take(appConfig.paymentHistoryLimit)

    Future.sequence(orderedTaxYears.map(incomeTaxViewChangeConnector.getPayments)) map { paymentResponses =>
      val paymentsContainsFailure: Boolean = paymentResponses.exists {
        case Payments(_) => false
        case PaymentsError(status, _) if status == 404 => false
        case PaymentsError(_, _) => true
      }
      if (paymentsContainsFailure) {
        Left(PaymentHistoryError)
      } else {
        Right(paymentResponses.collect {
          case Payments(payments) => payments
        }.flatten.distinct)
      }
    }
  }

  def getRepaymentHistory(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[RepaymentHistoryErrorModel.type, List[RepaymentHistory]]] = {

    incomeTaxViewChangeConnector.getRepaymentHistoryByNino(Nino(user.nino)).map {
      case RepaymentHistoryModel(repaymentsViewerDetails) => Right(repaymentsViewerDetails)
      case RepaymentHistoryErrorModel(status, _) if status == 404 => Right(List())
      case RepaymentHistoryErrorModel(_, _) => Left(RepaymentHistoryErrorModel)
    }
  }

  // A: list of documentNumbers for CutOver Credits for the given tax year
  private def getCutOverDocumentNumbersByTaxYear(taxYear: Int)
                                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, List[String]]] = {
    for {
      paymentsResponse <- incomeTaxViewChangeConnector.getPayments(taxYear)
      payments = paymentsResponse match {
        case Payments(payments) => Right(Payments(payments))
        case PaymentsError(status, _) if status == 404 => Left(PaymentHistoryError)
        case PaymentsError(_, _) => Left(PaymentHistoryError)
      }
    } yield payments match {
      case Right(payments) =>
        val documentNumbers: List[String] = payments
          .payments
          .filter(_.credit.isDefined)
          .map(_.transactionId)
          .filter(_.isDefined)
          .map(_.get)
          .toList
        Right(documentNumbers)
      case Left(errors) => Left(errors)
    }
  }

  // B: Get cutOver credits models for the given tax year and Nino
  def getAllCutOverCreditsByTaxYear(taxYear: Int, nino: String)
                                   (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, List[CreditDetailsModel]]] = {
    import CreditDetailsModel._
    getCutOverDocumentNumbersByTaxYear(taxYear).flatMap { result =>
      result match {
        case Right(documentIds) =>
          Future.sequence(
            for {
              creditModel <- documentIds.map { documentNumber =>
                incomeTaxViewChangeConnector
                  .getFinancialDetailsByDocumentId(Nino(nino), documentNumber)
                  .map {
                    case document: FinancialDetailsWithDocumentDetailsModel =>
                      val creditDetailsModel: CreditDetailsModel = document
                      creditDetailsModel
                    case _ =>
                      throw new Exception("Something else")
                  }
              }
            } yield creditModel
          ).map(creditModels => Right(creditModels))
        case Left(error) =>
          Future {
            Left(error)
          }
      }
    }
  }

  // C: Get all credits (MFA + CutOver one) by tax year (and given Nino)
  def getAllCredits(taxYear: Int, nino: String)
                   (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[PaymentHistoryError.type, List[CreditDetailsModel]]] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear, nino).map {
      case financialDetails: FinancialDetailsModel =>
        for {
          x <- getAllCutOverCreditsByTaxYear(taxYear, nino).flatMap { result =>
            result match {
              case Right(creditModels) => Future {
                val mfaCredits: CreditDetailsModel = financialDetails
                // merge cutOver credits with MFA credits
                Right(creditModels :+ mfaCredits)
              }
              case e@Left(_) => Future { e }
            }
          }
        } yield x
      case _: FinancialDetailsErrorModel =>
        Future(Left(PaymentHistoryError))
    }.flatten
  }
}

object PaymentHistoryService {
  case object PaymentHistoryError
}
