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
import models.financialDetails.{FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
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
        fdMaybe <- EitherT(getNonCrystallisedFinancialDetails(nino))
        maybeTaxYear <- EitherT.right[Throwable](Future.successful {
          fdMaybe.flatMap(x => arePoAPaymentsPresent(x.documentDetails))
        })
      } yield maybeTaxYear
    }.value
  }

  def getPoaForNonCrystallisedTaxYear(nino: Nino)
                                     (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[PaymentOnAccountViewModel]]] = {
    {
      for {
        financialDetailsMaybe <- EitherT(getNonCrystallisedFinancialDetails(nino))
        paymentOnAccountViewModelMaybe <- EitherT.right[Throwable](
          Future.successful(financialDetailsMaybe
            .flatMap(financialDetails =>
              getPaymentOnAccountModel(sortByTaxYear(financialDetails.documentDetails)))
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
      case (Right(reason), Right(FinancialDetailsAndPoAModel(_, Some(model)))) =>
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
        financialDetailsMaybe <- EitherT(getNonCrystallisedFinancialDetails(nino))
        fdAndChargeMaybe <- EitherT(Future.successful(getFinancialDetailAndChargeRefModel(financialDetailsMaybe)))
        haveBeenAdjusted <- EitherT(isSubsequentAdjustment(chargeHistoryConnector, fdAndChargeMaybe.chargeReference))
        paymentOnAccountViewModel <- EitherT(
          Future.successful(getAmendablePoaViewModel(sortByTaxYear(fdAndChargeMaybe.documentDetails), haveBeenAdjusted)))
      } yield paymentOnAccountViewModel
    }.value
  }

  // TODO: move private functions into Helper?
  private def getPoaAdjustmentReason(financialPoaDetails: Either[Throwable, FinancialDetailsAndPoAModel])
                                    (implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    {
      for {
        financialDetails <- EitherT(Future.successful(toFinancialDetail(financialPoaDetails)))
        chargeHistoryModelMaybe <- EitherT(getChargeHistory(chargeHistoryConnector, financialDetails.flatMap(_.chargeReference)))
      } yield chargeHistoryModelMaybe.flatMap(_.poaAdjustmentReason)
    }.value
  }


  //TODO: Merge the two functions below, lots of code duplication
  private def getNonCrystallisedFinancialDetails(nino: Nino)
                                                (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[FinancialDetailsModel]]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(None))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel => Right(Some(financialDetails))
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Left(new Exception("There was an error whilst fetching financial details data"))
        case _ => Right(None)
      }
    }
  }

  private def getPoaModelAndFinancialDetailsForNonCrystallised(nino: Nino)
                                                              (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, FinancialDetailsAndPoAModel]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(FinancialDetailsAndPoAModel(None, None)))
      case Some(taxYear: TaxYear) =>
        financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
          case financialDetails: FinancialDetailsModel =>
            Right(FinancialDetailsAndPoAModel(Some(financialDetails), getPaymentOnAccountModel(sortByTaxYear(financialDetails.documentDetails))))
          case error: FinancialDetailsErrorModel if error.code != NOT_FOUND =>
            Left(new Exception("There was an error whilst fetching financial details data"))
          case _ =>
            Right(FinancialDetailsAndPoAModel(None, None))
        }
    }
  }


}

