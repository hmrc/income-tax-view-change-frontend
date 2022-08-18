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
import models.CreditDetailModel
import models.core.Nino
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, Payments, PaymentsError}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import services.CreditHistoryService.CreditHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditHistoryService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                     val appConfig: FrontendAppConfig)
                                    (implicit ec: ExecutionContext) {

  // A: list of documentNumbers for CutOver Credits for the given tax year
  private def getCutOverDocumentNumbersByTaxYear(taxYear: Int)
                                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[String]]] = {
    for {
      paymentsResponse <- incomeTaxViewChangeConnector.getPayments(taxYear)
      payments = paymentsResponse match {
        case Payments(payments) => Right(Payments(payments))
        case PaymentsError(status, _) if status == 404 => Left(CreditHistoryError)
        case PaymentsError(_, _) => Left(CreditHistoryError)
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
  private def getAllCutOverCreditsByTaxYear(taxYear: Int, nino: String)
                                           (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    import CreditDetailModel._
    getCutOverDocumentNumbersByTaxYear(taxYear).flatMap { result =>
      result match {
        case Right(documentIds) =>
           val futureCreditModels = Future.sequence(
            for {
              creditModel <- documentIds.map { documentNumber =>
                incomeTaxViewChangeConnector
                  .getFinancialDetailsByDocumentId(Nino(nino), documentNumber)
                  .map {
                    case document: FinancialDetailsWithDocumentDetailsModel =>
                      val creditDetailsModel: List[CreditDetailModel] = document
                      creditDetailsModel
                    case _ =>
                      throw new Exception("CreditHistoryService::ERROR::CutOverCredits")
                  }
              }
            } yield creditModel
          )
          futureCreditModels.flatMap( e => Future { Right(e.flatten) } )
        case Left(error) =>
          Future {
            Left(error)
          }
      }
    }
  }

  // C: Get all credits (MFA + CutOver one) by tax year (and given Nino)
  def getCreditsHistory(taxYear: Int, nino: String)
                       (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear, nino).map {
      case financialDetails: FinancialDetailsModel =>
        for {
          x <- getAllCutOverCreditsByTaxYear(taxYear, nino).flatMap { result =>
            result match {
              case Right(creditModels) => Future {
                val mfaCredits: List[CreditDetailModel] = financialDetails
                // merge cutOver credits with MFA credits
                Right(creditModels ++ mfaCredits)
              }
              case e@Left(_) => Future {
                e
              }
            }
          }
        } yield x
      case _: FinancialDetailsErrorModel =>
        Future(Left(CreditHistoryError))
    }.flatten
  }

}

object CreditHistoryService {
  case object CreditHistoryError
}