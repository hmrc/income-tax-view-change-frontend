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
import models.incomeSourceDetails.TaxYear
import services.CreditHistoryService.CreditHistoryError
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CreditHistoryService @Inject() (
    financialDetailsConnector: FinancialDetailsConnector,
    val appConfig:             FrontendAppConfig
  )(
    implicit ec: ExecutionContext) {

  // This logic is based on the findings in => RepaymentHistoryUtils.combinePaymentHistoryData method
  // Problem: we need to get list of credits (MFA + CutOver) and filter it out by calendar year
  // MFA credits are driven by taxYear
  // CutOver credit by dueDate found in financialDetails related to the corresponding documentDetail (see getDueDateFor)
  @deprecated("Use getCreditsByTaxYearV2 instead", "MISUV-8845")
  private def getCreditsByTaxYear(
      taxYear: Int,
      nino:    String
    )(
      implicit hc: HeaderCarrier,
      user:        MtdItUser[_]
    ): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    financialDetailsConnector.getFinancialDetails(taxYear, nino).flatMap {
      case financialDetailsModel: FinancialDetailsModel =>
        val fdRes = financialDetailsModel.getPairedDocumentDetails().flatMap {
          case (document: DocumentDetail, financialDetail: FinancialDetail) =>
            (financialDetail.getCreditType, document.credit.isDefined) match {
              case (Some(CutOverCreditType), true) =>
                // if we didn't find CutOverCredit dueDate then we "lost" this document
                financialDetailsModel
                  .getDueDateForFinancialDetail(financialDetail)
                  .map(dueDate =>
                    CreditDetailModel(
                      date = dueDate,
                      document,
                      CutOverCreditType,
                      Some(financialDetailsModel.balanceDetails)
                    )
                  )
              case (Some(creditType), true) =>
                Some(
                  CreditDetailModel(
                    date = document.documentDate,
                    document,
                    creditType,
                    Some(financialDetailsModel.balanceDetails)
                  )
                )
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

  private def getCreditsByTaxYearV2(
      taxYearFrom: TaxYear,
      taxYearTo:   TaxYear,
      nino:        String
    )(
      implicit hc: HeaderCarrier,
      user:        MtdItUser[_]
    ): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {
    financialDetailsConnector.getFinancialDetails(taxYearFrom, taxYearTo, nino).flatMap {
      case financialDetailsModel: FinancialDetailsModel =>
        val fdRes = financialDetailsModel.getPairedDocumentDetails().flatMap {
          case (document: DocumentDetail, financialDetail: FinancialDetail) =>
            (financialDetail.getCreditType, document.credit.isDefined) match {
              case (Some(CutOverCreditType), true) =>
                // if we didn't find CutOverCredit dueDate then we "lost" this document
                financialDetailsModel
                  .getDueDateForFinancialDetail(financialDetail)
                  .map(dueDate =>
                    CreditDetailModel(
                      date = dueDate,
                      document,
                      CutOverCreditType,
                      Some(financialDetailsModel.balanceDetails)
                    )
                  )
              case (Some(creditType), true) =>
                Some(
                  CreditDetailModel(
                    date = document.documentDate,
                    document,
                    creditType,
                    Some(financialDetailsModel.balanceDetails)
                  )
                )
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

  @deprecated("Use getCreditsHistoryV2 instead", "MISUV-8845")
  def getCreditsHistory(
      calendarYear: Int,
      nino:         String
    )(
      implicit hc: HeaderCarrier,
      user:        MtdItUser[_]
    ): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {

    for {
      creditModelForTaxYear        <- getCreditsByTaxYear(calendarYear, nino)
      creditModelForTaxYearPlusOne <- getCreditsByTaxYear(calendarYear + 1, nino)
    } yield (creditModelForTaxYear, creditModelForTaxYearPlusOne) match {
      case (Right(creditModelTY), Right(creditModelTYandOne)) =>
        val creditsForTaxYearAndPlusOne =
          (creditModelTY ++ creditModelTYandOne).filter(creditDetailModel =>
            creditDetailModel.documentDetail.taxYear == calendarYear
          )
        Right(creditsForTaxYearAndPlusOne)
      case (Right(creditModelTY), Left(_)) =>
        val creditsForTaxYear =
          creditModelTY.filter(creditDetailModel => creditDetailModel.documentDetail.taxYear == calendarYear)
        Right(creditsForTaxYear)
      case (Left(_), Right(creditModelTYandOne)) =>
        val creditsForTaxYearPlusOne =
          creditModelTYandOne.filter(creditDetailModel => creditDetailModel.documentDetail.taxYear == calendarYear)
        Right(creditsForTaxYearPlusOne)
      case (_, _) =>
        Left(CreditHistoryError)
    }
  }

  def getCreditsHistoryV2(
      calendarYear: Int,
      nino:         String
    )(
      implicit hc: HeaderCarrier,
      user:        MtdItUser[_]
    ): Future[Either[CreditHistoryError.type, List[CreditDetailModel]]] = {

    for {
      creditModelsForBothYears <-
        getCreditsByTaxYearV2(TaxYear(calendarYear - 1, calendarYear), TaxYear(calendarYear, calendarYear + 1), nino)
    } yield creditModelsForBothYears match {
      case Right(creditModel) =>
        val creditsForTaxYearAndPlusOne =
          creditModel.filter(creditDetailModel => creditDetailModel.documentDetail.taxYear == calendarYear)
        Right(creditsForTaxYearAndPlusOne)
      case _ =>
        Left(CreditHistoryError)
    }
  }
}

object CreditHistoryService {
  case object CreditHistoryError
}
