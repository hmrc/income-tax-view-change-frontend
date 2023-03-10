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
            (financialDetail.isMFACredit, document.credit.isDefined, financialDetail.isBalancingChargeCredit) match {
              case (true, true, false) =>
                Some(CreditDetailModel(date = document.documentDate, document, MfaCreditType, Some(financialDetailsModel.balanceDetails)))
              case (false, true, true) =>
                Some(CreditDetailModel(date = document.documentDate, document, BalancingChargeCreditType, Some(financialDetailsModel.balanceDetails)))
              case (false, true, false) =>
                // if we didn't find CutOverCredit dueDate then we "lost" this document
                financialDetailsModel.getDueDateForFinancialDetail(financialDetail)
                  .map(dueDate => CreditDetailModel(date = dueDate, document, CutOverCreditType, Some(financialDetailsModel.balanceDetails)))
              case (_, _, _) => None
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

  def getTaxYearAsInt(taxYear: String): Int =
    Try(taxYear.toInt) match {
      case Success(taxYearValue) => taxYearValue
      case Failure(_) => throw MissingFieldException("Tax Year field should be a numeric value in a format of YYYY")
    }

  def getCreditsHistory(calendarYear: Int, nino: String)
                       (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    for {
      creditModelForTaxYear <- getCreditsByTaxYear(calendarYear, nino)
      creditModelForTaxYearPlusOne <- getCreditsByTaxYear(calendarYear + 1, nino)
    } yield (creditModelForTaxYear, creditModelForTaxYearPlusOne) match {
      case (Right(creditModelTY), Right(creditModelTYandOne)) =>
        Right((creditModelTY ++ creditModelTYandOne).filter(creditDetailModel => getTaxYearAsInt(creditDetailModel.documentDetail.taxYear) == calendarYear))
      case (Right(creditModelTY), Left(_)) =>
        Right(creditModelTY.filter(creditDetailModel => getTaxYearAsInt(creditDetailModel.documentDetail.taxYear) == calendarYear))
      case (Left(_), Right(creditModelTYandOne)) =>
        Right(creditModelTYandOne.filter(creditDetailModel => getTaxYearAsInt(creditDetailModel.documentDetail.taxYear) == calendarYear))
      case (_, _) =>
        Left(CreditHistoryError)
    }
  }
}

object CreditHistoryService {
  case object CreditHistoryError
}