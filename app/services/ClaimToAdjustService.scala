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
import connectors.{CalculationListConnector, FinancialDetailsConnector}
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.core.Nino
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                     val calculationListConnector: CalculationListConnector,
                                     implicit val dateService: DateServiceInterface)
                                    (implicit ec: ExecutionContext) {

  def getPoATaxYear(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Throwable, Option[TaxYear]]] = {
    {
      getAllFinancialDetails map {
        item =>
          item.collect {
            case (_, model: FinancialDetailsModel) => model.documentDetails
          }
      }
    }.map(result => getPoAPayments(result.flatten))
  }


  private def getPoAPayments(documentDetails: List[DocumentDetail]): Either[Throwable, Option[TaxYear]] = {
    {
      for {
        poa1 <- documentDetails.filter(_.documentDescription.exists(_.equals("ITSA- POA 1")))
          .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
        poa2 <- documentDetails.filter(_.documentDescription.exists(_.equals("ITSA - POA 2")))
          .sortBy(_.taxYear).reverse.headOption.map(doc => makeTaxYearWithEndYear(doc.taxYear))
      } yield {
        if (poa1 == poa2) { // TODO: what about scenario when both are None? this is not expect to be an error
          Right(Some(poa1))
        } else {
          Logger("application").error(s"[ClaimToAdjustService][getPoAPayments] " +
            s"PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not. < PoA1 TaxYear: $poa1, PoA2 TaxYear: $poa2 >")
          Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not."))
        }
      }
    }.getOrElse {
      // TODO: tidy up relevant unit tests, as this is a separate error, see log details;
      Logger("application").error(s"[ClaimToAdjustService][getPoAPayments] " +
        s"Unable to find required POA records")
      Left(new Exception("PoA 1 & 2 most recent documents were expected to be from the same tax year. They are not."))
    }
  }

  // TODO: this method need to be optimised
  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[List[(Int, FinancialDetailsResponseModel)]] = {
    Logger("application").debug(
      s"[ClaimToAdjustService][getAllFinancialDetails] - Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYearsByYearOfMigration.map {
      taxYear =>
        Logger("application").debug(s"[ClaimToAdjustService][getAllFinancialDetails] - Getting financial details for TaxYear: ${taxYear}")
        financialDetailsConnector.getFinancialDetails(taxYear, user.nino).map {
          case financialDetails: FinancialDetailsModel => Some((taxYear, financialDetails))
          case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Some((taxYear, error))
          case _ => None
        }
    }).map(_.flatten)
  }

  def getPoaTaxYearForEntryPoint(nino: Nino)(implicit hc: HeaderCarrier): Future[Either[Throwable, Option[TaxYear]]] = {
    checkCrystallisation(nino, getPoaAdjustableTaxYears).flatMap {
      case None => Future.successful(Right(None))
      case Some(taxYear: TaxYear) => financialDetailsConnector.getFinancialDetails(taxYear.endYear, nino.value).map {
        case financialDetails: FinancialDetailsModel => Right(arePoAPaymentsPresent(financialDetails.documentDetails))
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
    println("XXXXXX" + taxYearList)
    taxYearList match {
      case ::(head, next) => isTaxYearNonCrystallised(head, nino).flatMap {
        case true =>
          println("AAAAAAAAAA")
          Future.successful(Some(head))
        case false =>
          println("BBBBBBBBBB")
          checkCrystallisation(nino, next)
      }
      case Nil =>
        println("CCCCCCCCC")
        Future.successful(None)
    }
  }

  // Next 2 methods taken from Calculation List Service (with small changes), but I don't like the code duplication. Is there any solution to this?
  private def isTaxYearNonCrystallised(taxYear: TaxYear, nino: Nino)(implicit hc: HeaderCarrier): Future[Boolean] = {

    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd
    val futureTaxYear = taxYear.endYear >= currentTaxYearEnd
    if (futureTaxYear) {
      println("DDDDDDDDDDD")
      Future.successful(true)
    } else {
      isTYSCrystallised(nino, taxYear.endYear).map {
        case true =>
          println("EEEEEEEE")
          false
        case false =>
          println("FFFFFFFFFF")
          true
      }
    }
  }

  private def isTYSCrystallised(nino: Nino, taxYear: Int)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val taxYearRange = s"${(taxYear - 1).toString.substring(2)}-${taxYear.toString.substring(2)}"
    calculationListConnector.getCalculationList(nino, taxYearRange).flatMap {
      case res: CalculationListModel =>
        println("ZZZZZZZZZZZZZ" + res.crystallised)
        Future.successful(res.crystallised)
      case err: CalculationListErrorModel if err.code == 204 =>
        println("YYYYYYYYYYY")
        Future.successful(Some(false))
      case err: CalculationListErrorModel => Future.failed(new InternalServerException(err.message))
    } map {
      case Some(true) =>
        println("MMMMMMMM")
        true
      case _ =>
        println("NNNNNNNNNN")
        false
    }
  }

}
