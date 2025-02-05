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

  def getFinancialDetails(taxYearFrom: TaxYear, taxYearTo: TaxYear, nino: String)
                         (implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetails(taxYearFrom, taxYearTo, nino)
  }

  def getFinancialDetailsSingleYear(taxYear: TaxYear, nino: String)
                                   (implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetails(taxYear, taxYear, nino)
  }

  def getChargeDueDates(financialDetails: List[FinancialDetailsResponseModel]): Option[Either[(LocalDate, Boolean), Int]] = {
    val chargeDueDates: List[LocalDate] = financialDetails.flatMap {
      case fdm: FinancialDetailsModel => fdm.validChargesWithRemainingToPay.getAllDueDates
      case _ => List.empty[LocalDate]
    }.sortWith(_ isBefore _)

    val overdueDates: List[LocalDate] = chargeDueDates.filter(_ isBefore dateService.getCurrentDate)
    val nextDueDates: List[LocalDate] = chargeDueDates.diff(overdueDates)

    (overdueDates, nextDueDates) match {
      case (Nil, Nil) => None
      case (Nil, nextDueDate :: _) => Some(Left((nextDueDate, false)))
      case (overdueDate :: Nil, _) => Some(Left((overdueDate, true)))
      case _ => Some(Right(overdueDates.size))
    }
  }

  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[Option[FinancialDetailsResponseModel]] = {
    Logger("application").debug(
      s"Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    val listOfCalls = user.incomeSources.orderedTaxYearsInWindows

    if (listOfCalls.isEmpty)
      Future.successful(None)
    else {
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
          combineTwoResponses(acc, next)
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
                                  response2: Option[FinancialDetailsResponseModel]): Option[FinancialDetailsResponseModel] = {
    (response1, response2) match {
      case (Some(validModel1: FinancialDetailsModel), Some(validModel2: FinancialDetailsModel)) => Some(validModel1.mergeLists(validModel2))
      case (Some(validModel: FinancialDetailsModel), _) => Some(validModel)
      case (_, Some(validModel: FinancialDetailsModel)) => Some(validModel)
      case (Some(errorModel1: FinancialDetailsErrorModel), Some(errorModel2: FinancialDetailsErrorModel)) =>
        Some(FinancialDetailsErrorModel(errorModel1.code, s"Multiple errors returned when retrieving financial details: $errorModel1 + $errorModel2"))
      case (Some(errorModel: FinancialDetailsErrorModel), _) if errorModel.code != NOT_FOUND => Some(errorModel)
      case (_, Some(errorModel: FinancialDetailsErrorModel)) if errorModel.code != NOT_FOUND => Some(errorModel)
      case _ => None
    }
  }
}