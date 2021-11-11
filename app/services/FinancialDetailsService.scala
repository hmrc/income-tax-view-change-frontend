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

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.{CodingOut, FeatureSwitching}
import connectors.IncomeTaxViewChangeConnector
import play.api.http.Status.NOT_FOUND
import models.chargeHistory.{ChargeHistoryModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.financialDetails.{DocumentDetail, DocumentDetailWithDueDate, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import play.api.Logger
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsService @Inject()(val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
                                       (implicit val appConfig: FrontendAppConfig, ec: ExecutionContext) extends FeatureSwitching {

  def getFinancialDetails(taxYear: Int, nino: String)(implicit hc: HeaderCarrier): Future[FinancialDetailsResponseModel] = {
    incomeTaxViewChangeConnector.getFinancialDetails(taxYear, nino)
  }

  def getChargeDueDates(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Option[Either[(LocalDate, Boolean), Int]]] = {
    val orderedTaxYear: List[Int] = user.incomeSources.orderedTaxYearsByYearOfMigration

    Future.sequence(orderedTaxYear.map(item =>
      getFinancialDetails(item, user.nino)
    )) map { financialDetails =>
      val chargeDueDates: List[LocalDate] = financialDetails.flatMap {
        case fdm: FinancialDetailsModel => fdm.documentDetails.filterNot(_.isPaid) flatMap { documentDetail =>
          fdm.getDueDateFor(documentDetail)
        }
        case FinancialDetailsErrorModel(NOT_FOUND, _) => List.empty[LocalDate]
        case _ => throw new InternalServerException(s"[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details")
      }.sortWith(_ isBefore _)

      val overdueDates: List[LocalDate] = chargeDueDates.filter(_ isBefore LocalDate.now)
      val nextDueDates: List[LocalDate] = chargeDueDates.diff(overdueDates)

      (overdueDates, nextDueDates) match {
        case (Nil, Nil) => None
        case (Nil, nextDueDate :: _) => Some(Left((nextDueDate, false)))
        case (overdueDate :: Nil, _) => Some(Left((overdueDate, true)))
        case _ => Some(Right(overdueDates.size))
      }
    }
  }

  def getChargeHistoryDetails(mtdBsa: String, docNumber: String)
                             (implicit hc: HeaderCarrier): Future[Option[List[ChargeHistoryModel]]] = {
    incomeTaxViewChangeConnector.getChargeHistory(mtdBsa, docNumber) flatMap {
      case ok: ChargesHistoryModel => Future.successful(ok.chargeHistoryDetails)

      case error: ChargesHistoryErrorModel =>
        Logger("application").error(s"[FinancialDetailsService][getChargeHistoryDetails] $error")
        Future.failed(new InternalServerException("[FinancialDetailsService][getChargeHistoryDetails] - Failed to retrieve successful charge history"))
    }
  }

  def getAllFinancialDetails(implicit user: MtdItUser[_],
                             hc: HeaderCarrier, ec: ExecutionContext): Future[List[(Int, FinancialDetailsResponseModel)]] = {
    Logger("application").debug(
      s"[IncomeSourceDetailsService][getAllFinancialDetails] - Requesting Financial Details for all periods for mtditid: ${user.mtditid}")

    Future.sequence(user.incomeSources.orderedTaxYearsByYearOfMigration.map {
      taxYear =>
        incomeTaxViewChangeConnector.getFinancialDetails(taxYear, user.nino).map {
          case financialDetails: FinancialDetailsModel => Some((taxYear, financialDetails))
          case error: FinancialDetailsErrorModel if error.code != NOT_FOUND => Some((taxYear, error))
          case _ => None
        }
    }
    ).map(_.flatten)
  }

  def getAllUnpaidFinancialDetails(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[List[FinancialDetailsResponseModel]] = {
    getAllFinancialDetails.map { chargesWithYears =>
      chargesWithYears.flatMap {
        case (_, errorModel: FinancialDetailsErrorModel) => Some(errorModel)
        case (_, financialDetails: FinancialDetailsModel) =>
          val unpaidDocumentDetails: List[DocumentDetail] = financialDetails.documentDetails.collect {
            case documentDetail: DocumentDetail if isEnabled(CodingOut) && documentDetail.isCodingOut => documentDetail
            case documentDetail: DocumentDetail if documentDetail.latePaymentInterestAmount.isDefined && !documentDetail.interestIsPaid => documentDetail
            case documentDetail: DocumentDetail if !documentDetail.isPaid => documentDetail
          }
          if (unpaidDocumentDetails.nonEmpty) Some(financialDetails.copy(documentDetails = unpaidDocumentDetails)) else None
      }
    }
  }
}
