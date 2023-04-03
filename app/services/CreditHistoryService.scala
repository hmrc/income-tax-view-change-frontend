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
import connectors.IncomeTaxViewChangeConnector
import exceptions.MissingFieldException
import models.financialDetails.{DocumentDetail, FinancialDetail}
import models.creditDetailModel._
import models.financialDetails.FinancialDetailsModel
import services.CreditHistoryService.CreditHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CreditHistoryService @Inject()(incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                     val appConfig: FrontendAppConfig)
                                    (implicit ec: ExecutionContext) {

  // This logic is based on the findings in => RepaymentHistoryUtils.combinePaymentHistoryData method
  // Problem: we need to get list of credits (MFA + CutOver) and filter it out by calendar year
  // MFA credits are driven by taxYear
  // CutOver credit by dueDate found in financialDetails related to the corresponding documentDetail (see getDueDateFor)
  private def getCreditsByTaxYear(taxYear: Int, nino: String)
                                 (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear, nino).flatMap {
      case financialDetailsModel: FinancialDetailsModel =>
        val fdRes = financialDetailsModel.getPairedDocumentDetails().flatMap {
          case (document: DocumentDetail, financialDetail: FinancialDetail) =>
            (financialDetail.getCreditType, document.credit.isDefined) match {
              case (Some(MfaCreditType), true) =>
                Some(CreditDetailModel(date = document.documentDate, document, MfaCreditType, Some(financialDetailsModel.balanceDetails)))
              case (Some(BalancingChargeCreditType), true) =>
                Some(CreditDetailModel(date = document.documentDate, document, BalancingChargeCreditType, Some(financialDetailsModel.balanceDetails)))
              case (Some(CutOverCreditType), true) =>
                // if we didn't find CutOverCredit dueDate then we "lost" this document
                financialDetailsModel.getDueDateForFinancialDetail(financialDetail)
                  .map(dueDate => CreditDetailModel(date = dueDate, document, CutOverCreditType, Some(financialDetailsModel.balanceDetails)))
              case (_, _) => None
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

  def getCreditsHistory(calendarYear: Int, nino: String, isMFACreditsEnabled: Boolean, isCutoverCreditsEnabled: Boolean)
                       (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {

    for {
      creditModelForTaxYear <- getCreditsByTaxYear(calendarYear, nino)
      creditModelForTaxYearPlusOne <- getCreditsByTaxYear(calendarYear + 1, nino)
    } yield (creditModelForTaxYear, creditModelForTaxYearPlusOne) match {
      case (Right(creditModelTY), Right(creditModelTYandOne)) =>
        val creditsForTaxYearAndPlusOne =
          (creditModelTY ++ creditModelTYandOne).filter(creditDetailModel => creditDetailModel.documentDetail.taxYear == calendarYear)
        Right(filterExcludedCredits(creditsForTaxYearAndPlusOne, isMFACreditsEnabled, isCutoverCreditsEnabled))
      case (Right(creditModelTY), Left(_)) =>
        val creditsForTaxYear =
          creditModelTY.filter(creditDetailModel => creditDetailModel.documentDetail.taxYear == calendarYear)
        Right(filterExcludedCredits(creditsForTaxYear, isMFACreditsEnabled, isCutoverCreditsEnabled))
      case (Left(_), Right(creditModelTYandOne)) =>
        val creditsForTaxYearPlusOne =
          creditModelTYandOne.filter(creditDetailModel => creditDetailModel.documentDetail.taxYear == calendarYear)
        Right(filterExcludedCredits(creditsForTaxYearPlusOne, isMFACreditsEnabled, isCutoverCreditsEnabled))
      case (_, _) =>
        Left(CreditHistoryError)
    }
  }

  private def filterExcludedCredits(credits: List[CreditDetailModel], isMFACreditsEnabled: Boolean, isCutoverCreditsEnabled: Boolean): List[CreditDetailModel] = {
    (isMFACreditsEnabled, isCutoverCreditsEnabled) match {
      case (true, false) => credits.filterNot(_.creditType == CutOverCreditType)
      case (false, true) => credits.filterNot(_.creditType == MfaCreditType)
      case (false, false) => credits.filterNot(c => c.creditType == MfaCreditType || c.creditType == CutOverCreditType)
      case _ => credits
    }
  }
}

object CreditHistoryService {
  case object CreditHistoryError
}