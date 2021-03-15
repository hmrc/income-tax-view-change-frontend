/*
 * Copyright 2021 HM Revenue & Customs
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

import java.time.LocalDate

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.{API5, FeatureSwitching}
import connectors.IncomeTaxViewChangeConnector
import controllers.Assets.NOT_FOUND
import javax.inject.{Inject, Singleton}
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
                                       (implicit val appConfig: FrontendAppConfig, ec: ExecutionContext) extends FeatureSwitching {

  def getFinancialDetails(taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[FinancialDetailsResponseModel] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear, nino)
  }

  def getChargeDueDates(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Option[Either[(LocalDate, Boolean), Int]]] = {
    val orderedTaxYear: List[Int] = user.incomeSources.orderedTaxYears(true)

    Future.sequence(orderedTaxYear.map(item =>
      getFinancialDetails(item, user.nino)
    )) map { financialDetails =>
      val chargeDueDates: List[LocalDate] = financialDetails.flatMap {
        case fdm: FinancialDetailsModel => fdm.financialDetails.filterNot(_.isPaid).flatMap(_.charges()).flatMap(_.dueDate).map(LocalDate.parse)
        case FinancialDetailsErrorModel(NOT_FOUND, _) => List.empty[LocalDate]
        case _ => throw new InternalServerException(s"[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details")
      }.sortWith(_ isBefore _)

      val overdueDates: List[LocalDate] = chargeDueDates.filter(_ isBefore LocalDate.now)
      val nextDueDates: List[LocalDate] = chargeDueDates.diff(overdueDates)

      (overdueDates, nextDueDates) match {
        case (Nil, Nil) => None
        case (Nil, nextDueDate :: _) => Some(Left(nextDueDate, false))
        case (overdueDate :: Nil, _) => Some(Left(overdueDate, true))
        case _ => Some(Right(overdueDates.size))
      }
    }
  }

  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[List[(Int, FinancialDetailsResponseModel)]] = {
    Logger.debug(
      s"[IncomeSourceDetailsService][getAllFinancialDetails] - Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYears(isEnabled(API5)).map {
      taxYear =>
        incomeTaxViewChangeConnector.getFinancialDetails(taxYear, user.nino).map {
          case financialDetails: FinancialDetailsModel => Some((taxYear, financialDetails))
          case error: FinancialDetailsErrorModel if error.code != 404 => Some((taxYear, error))
          case _ => None
        }
    }
    ).map(_.flatten)
  }

  def getAllUnpaidFinancialDetails(implicit user: MtdItUser[_],
                                   hc: HeaderCarrier, ec: ExecutionContext): Future[List[FinancialDetailsResponseModel]] = {
    getAllFinancialDetails.map { chargesWithYears =>
      chargesWithYears.collect {
        case (_, errorModel: FinancialDetailsErrorModel) => errorModel
        case (_, financialDetails: FinancialDetailsModel) if !financialDetails.isAllPaid =>
          financialDetails.copy(
            financialDetails = financialDetails.financialDetails.filterNot(
              charge => charge.originalAmount.exists(_ <= 0) || charge.outstandingAmount.isEmpty)
          )
      }
    }
  }
}
