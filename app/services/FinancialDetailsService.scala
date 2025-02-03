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
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                        implicit val dateService: DateServiceInterface)
                                       (implicit val appConfig: FrontendAppConfig, ec: ExecutionContext) {

  def getFinancialDetailsSingleYear(taxYear: TaxYear, nino: String)
                                   (implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetails(taxYear, taxYear, nino)
  }

  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FinancialDetailsResponseModel]] = {
    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    val yearsOfMigration = user.incomeSources.orderedTaxYearsByYearOfMigration
    if (yearsOfMigration.isEmpty)
      Future.successful(None)
    else {
      val maxYears = 5
      val listOfCalls = yearsOfMigration.grouped(maxYears).toList

      Future.sequence(listOfCalls.map { years =>
        val (from, to) = (years.min, years.max)
        Logger("application").debug(s"Getting financial details for TaxYears: ${from} - ${to}")

        for {
          response <- financialDetailsConnector.getFinancialDetails(TaxYear.forYearEnd(from), TaxYear.forYearEnd(to), user.nino)
        } yield response match {
          case financialDetails: FinancialDetailsModel => Some(financialDetails)
          case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Some(error)
          case _ => None
        }
      }).map(x => {
        x.foldLeft[Option[FinancialDetailsResponseModel]](None) { (acc, next) =>
          Some(combineTwoResponses(acc, next))
        }
      })
    }
  }

  def getAllUnpaidFinancialDetails()(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FinancialDetailsResponseModel]] = {
    getAllFinancialDetails.map {
      case Some(errorModel: FinancialDetailsErrorModel) => Some(errorModel)
      case Some(financialDetails: FinancialDetailsModel) =>
        val unpaidDocDetails: List[DocumentDetail] = financialDetails.unpaidDocumentDetails()
        if (unpaidDocDetails.nonEmpty) Some(financialDetails.copy(documentDetails = unpaidDocDetails)) else None
      case _ => None
    }
  }

  def combineTwoResponses(response1: Option[FinancialDetailsResponseModel],
                                  response2: Option[FinancialDetailsResponseModel]): FinancialDetailsResponseModel = {
    (response1, response2) match {
      case (Some(validModel1: FinancialDetailsModel), Some(validModel2: FinancialDetailsModel)) => validModel1.mergeLists(validModel2)
      case (Some(validModel: FinancialDetailsModel), _) => validModel
      case (_, Some(validModel: FinancialDetailsModel)) => validModel
      case (Some(errorModel1: FinancialDetailsErrorModel), Some(errorModel2: FinancialDetailsErrorModel)) =>
        FinancialDetailsErrorModel(errorModel1.code, s"Multiple errors returned when retrieving financial details: $errorModel1 + $errorModel2")
      case (Some(errorModel: FinancialDetailsErrorModel), _) => errorModel
      case (_, Some(errorModel: FinancialDetailsErrorModel)) => errorModel
      case _ => FinancialDetailsErrorModel(INTERNAL_SERVER_ERROR, "Error handling response for financial details")
    }
  }
}