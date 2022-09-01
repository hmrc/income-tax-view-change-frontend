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
import models.{CreditDetailModel, CutOverCreditType, MfaCreditType}
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel, Payments, PaymentsError}
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import services.CreditHistoryService.CreditHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditHistoryService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                     val appConfig: FrontendAppConfig)
                                    (implicit ec: ExecutionContext) {

  private def getFinancialDetails(document: DocumentDetail, financialDetails: List[FinancialDetail]): Unit = {

  }
  // This logic is based on the findings in => RepaymentHistoryUtils.combinePaymentHistoryData method
  // Problem: we need to get list of credits (MFA + CutOver) and filter it out by calendar year
  // MFA credits are driven by documentDate
  // CutOver credit by dueDate found in financialDetails related to the corresponding documentDetail (see getDueDateFor)
  private def getCreditsByTaxYear(taxYear: Int, nino: String)
                                 (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear, nino).flatMap {
      case financialDetailsModel: FinancialDetailsModel =>
        val fdRes = financialDetailsModel.getPairedDocumentDetails().flatMap {
          case (document: DocumentDetail, financialDetail: FinancialDetail) => {
            (financialDetail.validMFACreditType(), document.credit.isDefined) match {
              case (true, true) =>
                Some(CreditDetailModel(date = document.documentDate, document, MfaCreditType))
              case (false, true) =>
                // if we didn't find CutOverCredit dueDate then we "lost" this document
                financialDetailsModel.getDueDateForFinancialDetail(financialDetail)
                  .map(dueDate => CreditDetailModel(date = dueDate, document, CutOverCreditType))
              case (_, _) => None
            }
          }
        }
        Future {
          Right(fdRes)
        }
      case _ =>
        Future {
          Left(CreditHistoryError)
        }
    }
  }



  def getCreditsHistory(calendarYear: Int, nino: String)
                       (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    for {
      creditModelForTaxYear <- getCreditsByTaxYear(calendarYear, nino)
      creditModelForTaxYearPlusOne <- getCreditsByTaxYear(calendarYear + 1, nino)
    } yield (creditModelForTaxYear, creditModelForTaxYearPlusOne) match {
      case (Right(creditModelTY), Right(creditModelTYandOne)) =>
        Right((creditModelTY ++ creditModelTYandOne).filter(_.date.getYear == calendarYear))
      case (Right(creditModelTY), Left(_)) =>
        Right(creditModelTY.filter(_.date.getYear == calendarYear))
      case (Left(_), Right(creditModelTYandOne)) =>
        Right(creditModelTYandOne.filter(_.date.getYear == calendarYear))
      case (_, _) =>
        Left(CreditHistoryError)
    }
  }
}

object CreditHistoryService {
  case object CreditHistoryError
}