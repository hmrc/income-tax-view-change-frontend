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
import models.creditDetailModel._
import models.financialDetails._
import services.CreditHistoryService.CreditHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditHistoryService @Inject()(financialDetailsConnector: FinancialDetailsConnector,
                                     val appConfig: FrontendAppConfig)
                                    (implicit ec: ExecutionContext) {

  // This logic is based on the findings in => RepaymentHistoryUtils.combinePaymentHistoryData method
  // Problem: we need to get list of credits (MFA + CutOver) and filter it out by calendar year
  // MFA credits are driven by taxYear
  // CutOver credit by dueDate found in financialDetails related to the corresponding documentDetail (see getDueDateFor)
  private def getCreditsByTaxYear(taxYear: Int, nino: String, claimARefundR18Enabled: Boolean)
                                 (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    financialDetailsConnector.getFinancialDetails(taxYear, nino).flatMap {
      case financialDetailsModel: FinancialDetailsModel =>
        val fdRes = financialDetailsModel.asChargeItems.flatMap {
          // Apply rewiring to use ChargeItem instead of DocumentDetails here
          case (chargeItem: ChargeItem) =>
            (chargeItem.transactionType, chargeItem.credit.isDefined) match {
              case (CutOverCreditType, true) =>
                Some(
                  CreditDetailModel(
                    date = chargeItem.dueDateForFinancialDetail.get,
                    charge = chargeItem,
                    CutOverCreditType,
                    availableCredit =
                      if (claimARefundR18Enabled) financialDetailsModel.balanceDetails.totalCredit
                      else financialDetailsModel.balanceDetails.totalCreditAvailableForRepayment)
                )

              case (creditTypeV, true) =>
                Some(
                  CreditDetailModel(
                    date = chargeItem.documentDate,
                    chargeItem,
                    creditType = creditTypeV.asInstanceOf[CreditType], // TODO: use safe type conversion instead
                    availableCredit =
                      if (claimARefundR18Enabled) financialDetailsModel.balanceDetails.totalCredit
                      else financialDetailsModel.balanceDetails.totalCreditAvailableForRepayment)
                )
              case (_, _) =>
                None
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

  def getCreditsHistory(calendarYear: Int, nino: String, claimARefundR18Enabled: Boolean)
                       (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {

    for {
      creditModelForTaxYear <- getCreditsByTaxYear(calendarYear, nino, claimARefundR18Enabled)
      creditModelForTaxYearPlusOne <- getCreditsByTaxYear(calendarYear + 1, nino, claimARefundR18Enabled)
    } yield (creditModelForTaxYear, creditModelForTaxYearPlusOne) match {
      case (Right(creditModelTY), Right(creditModelTYandOne)) =>
        val creditsForTaxYearAndPlusOne =
          (creditModelTY ++ creditModelTYandOne).filter(creditDetailModel => creditDetailModel.charge.taxYear.endYear == calendarYear)
        Right(creditsForTaxYearAndPlusOne)
      case (Right(creditModelTY), Left(_)) =>
        val creditsForTaxYear =
          creditModelTY.filter(creditDetailModel => creditDetailModel.charge.taxYear.endYear == calendarYear)
        Right(creditsForTaxYear)
      case (Left(_), Right(creditModelTYandOne)) =>
        val creditsForTaxYearPlusOne =
          creditModelTYandOne.filter(creditDetailModel => creditDetailModel.charge.taxYear.endYear == calendarYear)
        Right(creditsForTaxYearPlusOne)
      case (_, _) =>
        Left(CreditHistoryError)
    }
  }


}

object CreditHistoryService {
  case object CreditHistoryError
}