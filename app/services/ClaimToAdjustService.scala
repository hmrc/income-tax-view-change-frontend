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
import connectors.FinancialDetailsConnector
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.TaxYear.makeTaxYearWithEndYear
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ClaimToAdjustService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
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
        if (poa1 == poa2) {
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

}
