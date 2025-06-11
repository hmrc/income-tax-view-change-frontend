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
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.{TaxYear, TaxYearRange}
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                        implicit val dateService: DateServiceInterface)
                                       (implicit val appConfig: FrontendAppConfig) {

  def getFinancialDetails(taxYear: Int, nino: String)(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetails(taxYear, nino)
  }

  def getFinancialDetailsV2(taxYearRange: TaxYearRange, nino: String)(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetailsByTaxYearRange(taxYearRange, nino)
  }

  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[List[(Int, FinancialDetailsResponseModel)]] = {
    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYearsByYearOfMigration.map {
      taxYear =>
        Logger("application").debug(s"Getting financial details for TaxYear: ${taxYear}")
        financialDetailsConnector.getFinancialDetails(taxYear, user.nino).map {
          case financialDetails: FinancialDetailsModel => Some((taxYear, financialDetails))
          case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Some((taxYear, error))
          case _ => None
        }
    }).map(_.flatten)
  }

  def getAllFinancialDetailsV2(implicit user: MtdItUser[_],
                               hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FinancialDetailsResponseModel]] = {
    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    val yearsOfMigration = user.incomeSources.orderedTaxYearsByYearOfMigration
    if (yearsOfMigration.isEmpty)
      Future.successful(None)
    else {
      val (from, to) = (yearsOfMigration.min, yearsOfMigration.max)

      for {
        response <- financialDetailsConnector.getFinancialDetailsByTaxYearRange(TaxYearRange(TaxYear.forYearEnd(from), TaxYear.forYearEnd(to)), user.nino)

      } yield response match {
        case financialDetails: FinancialDetailsModel => Some(financialDetails)
        case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Some(error)
        case _ => None
      }

    }
  }

  def getAllUnpaidFinancialDetails()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[FinancialDetailsResponseModel]] = {
    getAllFinancialDetails.map { chargesWithYears =>
      chargesWithYears.flatMap {
        case (_, errorModel: FinancialDetailsErrorModel) => Some(errorModel)
        case (_, financialDetails: FinancialDetailsModel) =>
          val unpaidDocDetails: List[DocumentDetail] = financialDetails.unpaidDocumentDetails()
          if (unpaidDocDetails.nonEmpty) Some(financialDetails.copy(documentDetails = unpaidDocDetails)) else None
      }
    }
  }

  def getAllUnpaidFinancialDetailsV2()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FinancialDetailsResponseModel]] = {
    getAllFinancialDetailsV2.map {
      case Some(errorModel: FinancialDetailsErrorModel) => Some(errorModel)
      case Some(financialDetails: FinancialDetailsModel) =>
        val unpaidDocDetails: List[DocumentDetail] = financialDetails.unpaidDocumentDetails()
        if (unpaidDocDetails.nonEmpty) Some(financialDetails.copy(documentDetails = unpaidDocDetails)) else None
      case _ => None
    }
  }
}