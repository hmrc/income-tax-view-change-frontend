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
import connectors.{CalculationListConnector, ChargeHistoryConnector, FinancialDetailsConnector}
import models.claimToAdjustPoa.{AmendablePoaViewModel, PaymentOnAccountViewModel, PoAAmountViewModel}
import models.core.Nino
import models.financialDetails.{FinancialDetail, FinancialDetailsErrorModel, FinancialDetailsModel}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
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

  def getPoaTaxYearForEntryPoint(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[TaxYear]]] = {
    for {
      res <- getNonCrystallisedFinancialDetails(nino)
    } yield res match {
      case Right(Some(financialDetails)) =>
        val x = arePoAPaymentsPresent(financialDetails.documentDetails)
        Right(x)
      case Right(None) => Right(None)
      case Left(ex) =>
        Logger("application").error(s"There was an error getting FinancialDetailsModel" +
          s" < cause: ${ex.getCause} message: ${ex.getMessage} >")
        Left(ex)
    }
  }

  def getPoaForNonCrystallisedTaxYear(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[PaymentOnAccountViewModel]]] = {
    for {
      res <- getNonCrystallisedFinancialDetails(nino)
    } yield res match {
      case Right(Some(financialDetails)) =>
        val x = getPaymentOnAccountModel(sortByTaxYear(financialDetails.documentDetails))
        Right(x)
      case Right(None) => Right(None)
      case Left(ex) => Left(ex)
    }
  }

  private def getPoaAdjustmentReason(financialPoaDetails: Either[Throwable, FinancialDetailsAndPoAModel])(implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, Option[String]]] = {
    financialPoaDetails match {
      case Right(FinancialDetailsAndPoAModel(Some(finDetails), _)) =>
        finDetails.financialDetails.headOption match {
          case Some(detail) => getChargeHistory(chargeHistoryConnector, detail.chargeReference) map {
            case Right(Some(chargeHistory)) => Right(chargeHistory.poaAdjustmentReason)
            case Right(None) => Right(None)
            case Left(ex) => Left(ex)
          }
          case None => Future.successful(Left(new Exception("No financial details found for this charge")))
        }
      case Right(_) => Future.successful(Right(None))
      case Left(ex) => Future.successful(Left(ex))
    }
  }

  def getEnterPoAAmountViewModel(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, PoAAmountViewModel]] = {
    for {
      finanicalAndPoaModelMaybe <- getPoaModelAndFinancialDetailsForNonCrystallised(nino)
      adjustmentReasonMaybe     <- getPoaAdjustmentReason(finanicalAndPoaModelMaybe)
    } yield (adjustmentReasonMaybe, finanicalAndPoaModelMaybe) match {
      case (Right(reason), Right(FinancialDetailsAndPoAModel(Some(fdm), Some(model)))) =>
        Right(
          PoAAmountViewModel(
            partiallyOrFullyPaidPoaExists = hasPartiallyOrFullyPaidPoas(fdm.documentDetails),
            poaPreviouslyAdjusted = reason.isDefined,
            taxYear = model.taxYear,
            relevantAmountOne = model.poARelevantAmountOne,
            relevantAmountTwo = model.poARelevantAmountTwo,
            totalAmountOne = model.paymentOnAccountOne,
            totalAmountTwo = model.paymentOnAccountTwo
          )
        )
      case (Left(ex), _) => Left(ex)
      case (_, Left(ex)) => Left(ex)
      case _ => Left(new Exception("Unexpected error when creating Enter PoA Amount view model"))
    }
  }

  //TODO: Merge the two functions below, lots of code duplication
  private def getNonCrystallisedFinancialDetails(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[FinancialDetailsModel]]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(None))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel => Right(Some(financialDetails))
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Left(new Exception("There was an error whilst fetching financial details data"))
        case _ => Right(None)
      }
    }
  }

  private def getPoaModelAndFinancialDetailsForNonCrystallised(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, FinancialDetailsAndPoAModel]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears)(hc, dateService, calculationListConnector, ec).flatMap {
      case None => Future.successful(Right(FinancialDetailsAndPoAModel(None, None)))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel => Right(FinancialDetailsAndPoAModel(Some(financialDetails), getPaymentOnAccountModel(sortByTaxYear(financialDetails.documentDetails))))
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Left(new Exception("There was an error whilst fetching financial details data"))
        case _ => Right(FinancialDetailsAndPoAModel(None, None))
      }
    }
  }

  def getAmendablePoaViewModel(nino: Nino)
                              (implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, AmendablePoaViewModel]] = {
    getNonCrystallisedFinancialDetails(nino)
      .flatMap  {
        case Right(Some(FinancialDetailsModel(_, documentDetails, FinancialDetail(_, _, _, _, _, chargeReference, _, _, _, _, _, _, _, _) :: _))) =>
          isSubsequentAdjustment(chargeHistoryConnector, chargeReference)
            .map {
              case Right(haveBeenAdjusted) => getAmendablePoaViewModel(sortByTaxYear(documentDetails), haveBeenAdjusted)
              case Left(ex)                => Left(ex)
            }
        case Right(_) => Future.successful(Left(new Exception("Failed to retrieve non-crystallised financial details")))
        case Left(ex) => Future.successful(Left(ex))
      }
  }
}

private case class FinancialDetailsAndPoAModel(financialDetails: Option[FinancialDetailsModel],
                                        poaModel: Option[PaymentOnAccountViewModel])