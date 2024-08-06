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

package services.claimToAdjustPoa

import auth.MtdItUser
import connectors.{CalculationListConnector, ChargeHistoryConnector}
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.chargeHistory.{ChargeHistoryModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetail, FinancialDetailsModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.Logger
import services.DateServiceInterface
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.{LocalDate, Month}
import scala.concurrent.{ExecutionContext, Future}


trait ClaimToAdjustHelper {

  private val POA1: String = "ITSA- POA 1"
  private val POA2: String = "ITSA - POA 2"

  private val LAST_DAY_OF_JANUARY: Int = 31

  protected val poaDocumentDescriptions: List[String] = List(POA1, POA2)

  private val isPoAOne: DocumentDetail => Boolean = documentDetail =>
    documentDetail.documentDescription.contains(POA1)

  private val isPoATwo: DocumentDetail => Boolean = documentDetail =>
    documentDetail.documentDescription.contains(POA2)

  private val getTaxReturnDeadline: LocalDate => LocalDate = date =>
    LocalDate.of(date.getYear, Month.JANUARY, LAST_DAY_OF_JANUARY)
      .plusYears(1)

  val sortByTaxYear: List[DocumentDetail] => List[DocumentDetail] =
    _.sortBy(_.taxYear).reverse

  protected case class FinancialDetailsAndPoAModel(financialDetails: Option[FinancialDetailsModel],
                                                   poaModel: Option[PaymentOnAccountViewModel])

  protected case class FinancialDetailAndChargeRefMaybe(documentDetails: List[DocumentDetail],
                                                        chargeReference: Option[String])

  def getPaymentOnAccountModel(documentDetails: List[DocumentDetail],
                               poaPreviouslyAdjusted: Option[Boolean] = None): Either[Throwable, Option[PaymentOnAccountViewModel]] = {
    {
      for {
        poaOneDocDetail <- documentDetails.find(isPoAOne)
        poaTwoDocDetail <- documentDetails.find(isPoATwo)
        latestDocumentDetail = poaTwoDocDetail
        poaTwoDueDate <- poaTwoDocDetail.documentDueDate
        taxReturnDeadline = getTaxReturnDeadline(poaTwoDueDate)
        poasAreBeforeDeadline = poaTwoDueDate isBefore taxReturnDeadline
        if poasAreBeforeDeadline
      } yield {
        if (poaOneDocDetail.poaRelevantAmount.isDefined && poaTwoDocDetail.poaRelevantAmount.isDefined) {
          Right(
            Some(
              PaymentOnAccountViewModel(
                poaOneTransactionId = poaOneDocDetail.transactionId,
                poaTwoTransactionId = poaTwoDocDetail.transactionId,
                taxYear = makeTaxYearWithEndYear(latestDocumentDetail.taxYear),
                totalAmountOne = poaOneDocDetail.originalAmount,
                totalAmountTwo = poaTwoDocDetail.originalAmount,
                relevantAmountOne = poaOneDocDetail.poaRelevantAmount.get,
                relevantAmountTwo = poaTwoDocDetail.poaRelevantAmount.get,
                previouslyAdjusted = poaPreviouslyAdjusted,
                partiallyPaid = poaOneDocDetail.isPartPaid || poaTwoDocDetail.isPartPaid,
                fullyPaid = poaOneDocDetail.isPaid || poaTwoDocDetail.isPaid
              )
            )
          )
        } else {
          val errors: List[String] = List(
            poaOneDocDetail.poaRelevantAmount.map(_ => "").getOrElse("DocumentDetail.poaRelevantAmount::One"),
            poaTwoDocDetail.poaRelevantAmount.map(_ => "").getOrElse("DocumentDetail.poaRelevantAmount::Two")
          ).filterNot(_.isEmpty)
          Left(new Exception(errors.mkString("-")))
        }
      }
    } match {
      case Some(res) => res
      case None => Right(None)
    }
  }

  protected def getChargeHistory(chargeHistoryConnector: ChargeHistoryConnector, chargeReference: Option[String])
                                (implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Option[ChargeHistoryModel]]] = {
    chargeHistoryConnector.getChargeHistory(user.nino, chargeReference).map {
      case ChargesHistoryModel(_, _, _, chargeHistoryDetails) => chargeHistoryDetails match {
        case Some(detailsList) => Right(extractPoaChargeHistory(detailsList))
        case None => Right(None)
      }
      case ChargesHistoryErrorModel(code, message) =>
        Logger("application").error("chargeHistoryConnector.getChargeHistory returned a non-valid response")
        Left(new Exception(s"Error retrieving charge history code: $code message: $message"))
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
        case err: CalculationListErrorModel if err.code == 404 =>
          Logger("application").info("User had no calculations for this tax year, therefore is non-crystallised")
          Future.successful(false)
        case err: CalculationListErrorModel =>
          Logger("application").error("getCalculationList returned a non-valid response")
          Future.failed(new InternalServerException(err.message))
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

  def getAmendablePoaViewModel(documentDetails: List[DocumentDetail],
                               poasHaveBeenAdjustedPreviously: Boolean): Either[Throwable, PaymentOnAccountViewModel] = {
    val res  = for {
      poaOneDocDetail <- documentDetails.find(isPoAOne)
      poaTwoDocDetail <- documentDetails.find(isPoATwo)
      latestDocumentDetail = poaTwoDocDetail
      poaTwoDueDate <- poaTwoDocDetail.documentDueDate
      taxReturnDeadline = getTaxReturnDeadline(poaTwoDueDate)
      poasAreBeforeDeadline = poaTwoDueDate isBefore taxReturnDeadline
      if poasAreBeforeDeadline
  } yield {
      if (poaOneDocDetail.poaRelevantAmount.isDefined && poaTwoDocDetail.poaRelevantAmount.isDefined) {
        Right(
          PaymentOnAccountViewModel(
            poaOneTransactionId = poaOneDocDetail.transactionId,
            poaTwoTransactionId = poaTwoDocDetail.transactionId,
            taxYear = makeTaxYearWithEndYear(latestDocumentDetail.taxYear),
            totalAmountOne = poaOneDocDetail.originalAmount,
            totalAmountTwo = poaTwoDocDetail.originalAmount,
            relevantAmountOne = poaOneDocDetail.poaRelevantAmount.get,
            relevantAmountTwo = poaTwoDocDetail.poaRelevantAmount.get,
            partiallyPaid = poaOneDocDetail.isPartPaid || poaTwoDocDetail.isPartPaid,
            fullyPaid = poaOneDocDetail.isPaid || poaTwoDocDetail.isPaid,
            previouslyAdjusted = Some(poasHaveBeenAdjustedPreviously)
          )
        )
      } else {
        val errors: List[String] = List(
          poaOneDocDetail.poaRelevantAmount.map(_ => "").getOrElse("DocumentDetail.poaRelevantAmount::One"),
          poaTwoDocDetail.poaRelevantAmount.map(_ => "").getOrElse("DocumentDetail.poaRelevantAmount::Two")
        ).filterNot(_.isEmpty)
        Left(new Exception(errors.mkString("-")))
      }
    }
    res match {
      case Some(e) => e
      case _ => Left(new Exception("Unable to construct PaymentOnAccountViewModel"))
    }
  }

  protected def isSubsequentAdjustment(chargeHistoryConnector: ChargeHistoryConnector, chargeReference: Option[String])
                                      (implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Boolean]] = {
    chargeHistoryConnector.getChargeHistory(user.nino, chargeReference) map {
      case ChargesHistoryModel(_, _, _, Some(charges)) if charges.filter(_.isPoA).exists(_.poaAdjustmentReason.isDefined) => Right(true)
      case ChargesHistoryModel(_, _, _, _) => Right(false)
      case ChargesHistoryErrorModel(code, message) =>
        Logger("application").error("getChargeHistory returned a non-valid response")
        Left(new Exception(s"Error retrieving charge history code: $code message: $message"))
    }
  }

  private def extractPoaChargeHistory(chargeHistories: List[ChargeHistoryModel]): Option[ChargeHistoryModel] = {
    // We are not differentiating between POA 1 & 2 as records for both should match since they are always amended together
    chargeHistories.find(chargeHistoryModel => chargeHistoryModel.isPoA)
  }

  // TODO: re-write with the use of EitherT
  protected def toFinancialDetail(financialPoaDetails: Either[Throwable, FinancialDetailsAndPoAModel]): Either[Throwable, Option[FinancialDetail]] = {
    financialPoaDetails match {
      case Right(FinancialDetailsAndPoAModel(Some(finDetails), _)) =>
        finDetails.financialDetails.headOption match {
          case Some(detail) => Right(Some(detail))
          case None => Left(new Exception("No financial details found for this charge"))
        }
      case Right(_) => Right(None)
      case Left(ex) => Left(ex)
    }
  }

  protected def getFinancialDetailAndChargeRefModel(financialDetailModel: Option[FinancialDetailsModel]): Either[Throwable, FinancialDetailAndChargeRefMaybe] = {
    financialDetailModel match {
      case Some(
      FinancialDetailsModel(_, documentDetails, FinancialDetail(_, _, _, _, _, chargeReference, _, _, _, _, _, _, _, _) :: _)) =>
        Right(FinancialDetailAndChargeRefMaybe(documentDetails, chargeReference))
      case _ =>
        Left(new Exception("Failed to retrieve non-crystallised financial details"))
    }
  }

}
