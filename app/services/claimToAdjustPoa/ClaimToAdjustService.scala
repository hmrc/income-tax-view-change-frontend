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

import auth.MtdItUser
import cats.data.EitherT
import connectors.{CalculationListConnector, ChargeHistoryConnector, FinancialDetailsConnector}
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.Nino
import models.financialDetails.{ChargeItem, FinancialDetailsErrorModel, FinancialDetailsModel, PoaOneDebit, PoaTwoDebit}
import models.incomeSourceDetails.TaxYear
import play.api.http.Status.NOT_FOUND
import services.claimToAdjustPoa.ClaimToAdjustHelper
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                     val chargeHistoryConnector: ChargeHistoryConnector,
                                     val calculationListConnector: CalculationListConnector,
                                     implicit val dateService: DateServiceInterface)
                                    (implicit ec: ExecutionContext) extends ClaimToAdjustHelper {

  def getPoaTaxYearForEntryPoint(nino: Nino)
                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[TaxYear]]] = {
    {
      for {
        chargeItems <- EitherT(getNonCrystallisedFinancialDetails(nino))
        maybeTaxYear <- EitherT.right[Throwable](Future.successful {
          chargeItems.find( chargeItem => List(PoaOneDebit, PoaTwoDebit).contains(chargeItem.transactionType) )
            .map(_.taxYear)
        })
      } yield maybeTaxYear
    }.value
  }

  def getPoaForNonCrystallisedTaxYear(nino: Nino)
                                     (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[PaymentOnAccountViewModel]]] = {
    {
      for {
        chargeItems <- EitherT(getNonCrystallisedFinancialDetails(nino))
        paymentOnAccountViewModelMaybe <- EitherT(
          Future.successful({
            val sortedCharges = sortByTaxYearC(chargeItems)
            getPaymentOnAccountModel(sortedCharges)
          }
//            chargeItems
//            .map { chargeItem =>
//              val charges = sortByTaxYearC( chargeItem )
//              getPaymentOnAccountModel(charges)
//            } match {
//              case Some(x) => x
//              case None => Left(new Exception("Unable to extract getPaymentOnAccountModel"))
//          }
          ))
      } yield paymentOnAccountViewModelMaybe
    }.value
  }

  def getPoaViewModelWithAdjustmentReason(nino: Nino)
                                         (implicit hc: HeaderCarrier,
                                          user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, PaymentOnAccountViewModel]] = {
    for {
      financialAndPoaModelMaybe <- getPoaModelAndFinancialDetailsForNonCrystallised(nino)
      adjustmentReasonMaybe <- getPoaAdjustmentReason(financialAndPoaModelMaybe)
    } yield (adjustmentReasonMaybe, financialAndPoaModelMaybe) match {
      case (Right(reason), Right(FinancialDetailsAndPoaModel(_, Some(model)))) =>
        Right(
          model.copy(previouslyAdjusted = Some(reason.isDefined))
        )
      case (Left(ex), _) => Left(ex)
      case (_, Left(ex)) => Left(ex)
      case _ => Left(new Exception("Unexpected error when creating Enter PoA Amount view model"))
    }
  }

  def getAmendablePoaViewModel(nino: Nino)
                              (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, PaymentOnAccountViewModel]] = {
    {
      for {
        chargeItems <- EitherT(getNonCrystallisedFinancialDetails(nino))
        fdAndChargeMaybe <- EitherT(Future.successful(getFinancialDetailAndChargeRefModel(chargeItems)))
        haveBeenAdjusted <- EitherT(isSubsequentAdjustment(chargeHistoryConnector, fdAndChargeMaybe.chargeReference))
        paymentOnAccountViewModel <- EitherT(
          Future.successful(getAmendablePoaViewModel(sortByTaxYearC(fdAndChargeMaybe.chargeItems), haveBeenAdjusted)))
      } yield paymentOnAccountViewModel
    }.value
  }

  // TODO: move private functions into Helper?
  private def getPoaAdjustmentReason(financialPoaDetails: Either[Throwable, FinancialDetailsAndPoaModel])
                                    (implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    {
      for {
        someChargeItem <- EitherT(Future.successful(toFinancialDetail(financialPoaDetails)))
        chargeHistoryModelMaybe <- EitherT(getChargeHistory(chargeHistoryConnector, someChargeItem.flatMap(_.chargeReference)))
      } yield chargeHistoryModelMaybe.flatMap(_.poaAdjustmentReason)
    }.value
  }


  //TODO: Merge the two functions below, lots of code duplication
  private def getNonCrystallisedFinancialDetails(nino: Nino)
                                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, List[ChargeItem]]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(List.empty))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel =>
          val chargeItems = financialDetails.toChargeItem()
          Right(chargeItems)
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Left(new Exception("There was an error whilst fetching financial details data"))
        case _ => Right(List.empty)
      }
    }
  }

  private def getPoaModelAndFinancialDetailsForNonCrystallised(nino: Nino)
                                                              (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, FinancialDetailsAndPoaModel]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(FinancialDetailsAndPoaModel(List(), None)))
      case Some(taxYear: TaxYear) =>
        financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
          case financialDetails: FinancialDetailsModel =>
            val charges = sortByTaxYearC(financialDetails.toChargeItem())
            getPaymentOnAccountModel(charges) match {
              case Right(x) =>
                Right(FinancialDetailsAndPoaModel(charges, x))
              case Left(ex) =>
                Left(ex)
            }
          case error: FinancialDetailsErrorModel if error.code != NOT_FOUND =>
            Left(new Exception("There was an error whilst fetching financial details data"))
          case _ =>
            Right(FinancialDetailsAndPoaModel(List.empty, None))
        }
    }
  }


}

