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
import play.api.Logger
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val financialDetailsConnector: FinancialDetailsConnector,
                                        implicit val dateService: DateServiceInterface)
                                       (implicit val appConfig: FrontendAppConfig, ec: ExecutionContext) {

  def getFinancialDetails(taxYear: Int, nino: String)(implicit hc: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {
    financialDetailsConnector.getFinancialDetails(taxYear, nino)
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
}
