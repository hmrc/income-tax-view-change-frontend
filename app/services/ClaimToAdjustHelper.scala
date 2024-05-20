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

import connectors.CalculationListConnector
import exceptions.MissingFieldException
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.Nino
import models.financialDetails.DocumentDetail
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.{LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}

// TODO: This part of the logic expected to be moved within BE
// TODO: plain models like: TaxYear and PaymentOnAccountViewModel will be return via new connector
trait ClaimToAdjustHelper {

  private val POA1: String = "ITSA- POA 1"
  private val POA2: String = "ITSA - POA 2"

  private val LAST_DAY_OF_JANUARY: Int = 31

  protected val poaDocumentDescriptions: List[String] = List(POA1, POA2)

  private val isUnpaidPoAOne: DocumentDetail => Boolean = documentDetail =>
    documentDetail.documentDescription.contains(POA1) &&
      (documentDetail.outstandingAmount != 0)

  private val isUnpaidPoATwo: DocumentDetail => Boolean = documentDetail =>
    documentDetail.documentDescription.contains(POA2) &&
      (documentDetail.outstandingAmount != 0)

  private val taxReturnDeadlineOf: LocalDate => LocalDate = date =>
    LocalDate.of(date.getYear, Month.JANUARY, LAST_DAY_OF_JANUARY)
      .plusYears(1)

  val sortByTaxYear: List[DocumentDetail] => List[DocumentDetail] =
    _.sortBy(_.taxYear).reverse

  def getPaymentOnAccountModel(documentDetails: List[DocumentDetail]): Option[PaymentOnAccountViewModel] = {
    for {
      poaOneDocDetail           <- documentDetails.find(isUnpaidPoAOne)
      poaTwoDocDetail           <- documentDetails.find(isUnpaidPoATwo)
      latestDocumentDetail       = poaTwoDocDetail
      poaTwoDueDate             <- poaTwoDocDetail.documentDueDate
      poaDeadline                = taxReturnDeadlineOf(poaTwoDueDate)
      poasAreBeforeDeadline      = poaTwoDueDate isBefore poaDeadline
      if poasAreBeforeDeadline
    } yield {

      PaymentOnAccountViewModel(
        poaOneTransactionId  = poaOneDocDetail.transactionId,
        poaTwoTransactionId  = poaTwoDocDetail.transactionId,
        taxYear              = makeTaxYearWithEndYear(latestDocumentDetail.taxYear),
        paymentOnAccountOne  = poaOneDocDetail.originalAmount,
        paymentOnAccountTwo  = poaTwoDocDetail.originalAmount,
        poARelevantAmountOne = poaOneDocDetail.poaRelevantAmount.getOrElse(throw MissingFieldException("DocumentDetail.poaRelevantAmount")),
        poARelevantAmountTwo = poaTwoDocDetail.poaRelevantAmount.getOrElse(throw MissingFieldException("DocumentDetail.poaRelevantAmount"))
      )
    }
  }

  protected def isTaxYearNonCrystallised(taxYear: TaxYear, nino: Nino)
                                      (implicit hc: HeaderCarrier, dateService: DateServiceInterface,
                                       calculationListConnector: CalculationListConnector, ec: ExecutionContext): Future[Boolean] = {
    if (taxYear.isFutureTaxYear(dateService)) {
      Future.successful(true)
    } else {
      calculationListConnector.getCalculationList(nino, taxYear.formatTaxYearRange).flatMap {
        case res: CalculationListModel => Future.successful(res.crystallised.getOrElse(false))
        case err: CalculationListErrorModel if err.code == 204 => Future.successful(false)
        case err: CalculationListErrorModel => Future.failed(new InternalServerException(err.message))
      }.map(!_)
    }
  }

  protected def checkCrystallisation(nino: Nino, taxYearList: List[TaxYear])
                                    (implicit hc: HeaderCarrier, dateService: DateServiceInterface,
                                     calculationListConnector: CalculationListConnector, ec: ExecutionContext): Future[Option[TaxYear]] = {
    taxYearList.foldLeft(Future.successful(Option.empty[TaxYear])) { (acc, item) =>
      acc.flatMap {
        case Some(_) => acc
        case None => isTaxYearNonCrystallised(item, nino)(hc, dateService, calculationListConnector, ec) map {
          case true => Some(item)
          case false => None
        }
      }
    }
  }

  protected def getPoaAdjustableTaxYears(implicit dateService: DateServiceInterface): List[TaxYear] = {
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

  protected def arePoAPaymentsPresent(documentDetails: List[DocumentDetail]): Option[TaxYear] = {
    documentDetails.filter(_.documentDescription.exists(description => poaDocumentDescriptions.contains(description)))
      .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
  }

}
