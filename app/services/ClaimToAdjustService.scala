/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.{CalculationListConnector, FinancialDetailsConnector}
import exceptions.MissingFieldException
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import models.paymentOnAccount.PaymentOnAccount
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.{LocalDate, Month}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                     val calculationListConnector: CalculationListConnector,
                                     implicit val dateService: DateServiceInterface)
                                    (implicit ec: ExecutionContext) {

  def getPoaTaxYearForEntryPoint(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[TaxYear]]] = {
    for {
      res <- getPoaForNonCrystallisedFinancialDetails(nino)
    } yield res match {
      case Right(Some(financialDetails)) =>
        val x = arePoAPaymentsPresent(financialDetails.documentDetails)
        Right(x)
      case Right(None) => Right(None)
      case Left(ex) => Left(ex)
    }
  }

  def getPoaForNonCrystallisedTaxYear(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[PaymentOnAccount]]] = {
    for {
      res <- getPoaForNonCrystallisedFinancialDetails(nino)
    } yield res match {
      case Right(Some(financialDetails)) =>
        val x = getPaymentOnAccountModel(financialDetails.documentDetails)
        Right(x)
      case Right(None) => Right(None)
      case Left(ex) => Left(ex)
    }
  }

  private def getPoaForNonCrystallisedFinancialDetails(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[FinancialDetailsModel]]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears).flatMap {
      case None => Future.successful(Right(None))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel => Right(Some(financialDetails))
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Left(new Exception("There was an error whilst fetching financial details data"))
        case _ => Right(None)
      }
    }
  }

  private def arePoAPaymentsPresent(documentDetails: List[DocumentDetail]): Option[TaxYear] = {
    documentDetails.filter(_.documentDescription.exists(description => description.equals("ITSA- POA 1") || description.equals("ITSA - POA 2")))
      .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
  }

  private def getPoaAdjustableTaxYears: List[TaxYear] = {
    if (dateService.isAfterTaxReturnDeadlineButBeforeTaxYearEnd) {
      List(
        TaxYear.makeTaxYearWithEndYear(dateService.getCurrentTaxYearEnd)
      )
    } else {
      List(
        TaxYear.makeTaxYearWithEndYear(dateService.getCurrentTaxYearEnd).addYears(-1),
        TaxYear.makeTaxYearWithEndYear(dateService.getCurrentTaxYearEnd)
      ).sortBy(_.endYear)
    }
  }

  private def checkCrystallisation(nino: Nino, taxYearList: List[TaxYear])(implicit hc: HeaderCarrier): Future[Option[TaxYear]] = {
    taxYearList match {
      case ::(head, next) => isTaxYearNonCrystallised(head, nino).flatMap {
        case true => Future.successful(Some(head))
        case false => checkCrystallisation(nino, next)
      }
      case Nil => Future.successful(None)
    }
  }

  // Next 2 methods taken from Calculation List Service (with small changes), but I don't like the code duplication. Is there any solution to this?
  private def isTaxYearNonCrystallised(taxYear: TaxYear, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] = {

    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd
    val futureTaxYear = taxYear.endYear >= currentTaxYearEnd
    if (futureTaxYear) {
      Future.successful(true)
    } else {
      isTYSCrystallised(nino, taxYear.endYear).map {
        case true => false
        case false => true
      }
    }
  }

  private def isTYSCrystallised(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val taxYearRange = s"${(taxYear - 1).toString.substring(2)}-${taxYear.toString.substring(2)}"
    calculationListConnector.getCalculationList(nino, taxYearRange).flatMap {
      case res: CalculationListModel => Future.successful(res.crystallised)
      case err: CalculationListErrorModel if err.code == 204 => Future.successful(Some(false))
      case err: CalculationListErrorModel => Future.failed(new InternalServerException(err.message))
    } map {
      case Some(true) => true
      case _ => false
    }
  }

  private def getPaymentOnAccountModel(documentDetails: List[DocumentDetail]): Option[PaymentOnAccount] = {
    for {
      poaOneDocDetail          <- documentDetails.filter(isUnpaidPoAOne).sortBy(_.taxYear).reverse.headOption
      poaTwoDocDetail          <- documentDetails.filter(isUnpaidPoATwo).sortBy(_.taxYear).reverse.headOption
      latestDocumentDetail     <- documentDetails.filter(isUnpaidPaymentOnAccount).sortBy(_.taxYear).reverse.headOption
      poasAreBeforeTaxDeadline <- arePoAsBeforeTaxReturnDeadline(poaTwoDocDetail.documentDueDate)
    } yield {

      Logger("application").debug(s"PoA 1 - dueDate: ${poaOneDocDetail.documentDueDate}, outstandingAmount: ${poaOneDocDetail.outstandingAmount}")
      Logger("application").debug(s"PoA 2 - dueDate: ${poaTwoDocDetail.documentDueDate}, outstandingAmount: ${poaTwoDocDetail.outstandingAmount}")
      Logger("application").debug(s"PoA 1 & 2 are before Tax return deadline: $poasAreBeforeTaxDeadline")

      PaymentOnAccount(
        poaOneTransactionId = poaOneDocDetail.transactionId,
        poaTwoTransactionId = poaTwoDocDetail.transactionId,
        taxYear             = makeTaxYearWithEndYear(latestDocumentDetail.taxYear),
        paymentOnAccountOne = poaOneDocDetail.originalAmount.getOrElse(throw MissingFieldException("DocumentDetail.totalAmount")), // TODO: Change field to mandatory MISUV-7556
        paymentOnAccountTwo = poaOneDocDetail.originalAmount.getOrElse(throw MissingFieldException("DocumentDetail.totalAmount"))
      )
    }
  }

  private def arePoAsBeforeTaxReturnDeadline(poaTwoDate: Option[LocalDate]): Option[Boolean] = {
    for {
      poaTwoDeadline             <- taxReturnDeadlineOf(poaTwoDate)
      poaTwoDateIsBeforeDeadline <- poaTwoDate.map(_.isBefore(poaTwoDeadline))
    } yield {
      Logger("application").debug(s"PoA 1 - documentDueDate: $poaTwoDate, TaxReturnDeadline: $poaTwoDeadline")
      poaTwoDateIsBeforeDeadline
    }
  }

  private val isUnpaidPoAOne: DocumentDetail => Boolean = documentDetail =>
    documentDetail.documentDescription.contains("ITSA- POA 1") &&
    !documentDetail.outstandingAmount.contains(BigDecimal(0))

  private val isUnpaidPoATwo: DocumentDetail => Boolean = documentDetail =>
    documentDetail.documentDescription.contains("ITSA - POA 2") &&
    !documentDetail.outstandingAmount.contains(BigDecimal(0))

  private val isUnpaidPaymentOnAccount: DocumentDetail => Boolean = documentDetail =>
    isUnpaidPoAOne(documentDetail) || isUnpaidPoATwo(documentDetail)

  private val LAST_DAY_OF_JANUARY: Int = 31

  private val taxReturnDeadlineOf: Option[LocalDate] => Option[LocalDate] = dueDate => {
    dueDate.map(d => LocalDate.of(d.getYear, Month.JANUARY, LAST_DAY_OF_JANUARY).plusYears(1))
  }

}
