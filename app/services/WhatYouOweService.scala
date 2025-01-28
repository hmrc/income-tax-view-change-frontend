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
import config.featureswitch.FeatureSwitching
import connectors.{FinancialDetailsConnector, OutstandingChargesConnector}
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouOweService @Inject()(val financialDetailsService: FinancialDetailsService,
                                  val financialDetailsConnector: FinancialDetailsConnector,
                                  val outstandingChargesConnector: OutstandingChargesConnector,
                                  implicit val dateService: DateServiceInterface)
                                 (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig)
  extends TransactionUtils with FeatureSwitching {

  implicit lazy val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val validChargeTypeCondition: ChargeItem => Boolean = chargeItem => {
    (chargeItem.transactionType, chargeItem.subTransactionType) match {
      case (_, Some(Nics2)) => true
      case (PoaOneDebit | PoaTwoDebit | PoaOneReconciliationDebit
            | PoaTwoReconciliationDebit | BalancingCharge | MfaDebitCharge, _) => true
      case (_, _) => false
    }
  }

  def getWhatYouOweChargesList(isReviewAndReconcile: Boolean, isFilterCodedOutPoasEnabled: Boolean)
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {
    {
      for {
        unpaidChanges <- financialDetailsService.getAllUnpaidFinancialDetails()
      } yield getWhatYouOweChargesList(unpaidChanges, isReviewAndReconcile, isFilterCodedOutPoasEnabled)
    }.flatten
  }

  def getWhatYouOweChargesList(unpaidCharges: Option[FinancialDetailsResponseModel],
                               isReviewAndReconciledEnabled: Boolean,
                               isFilterCodedOutPoasEnabled: Boolean)
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {

    unpaidCharges match {
      case Some(fds: FinancialDetailsModel) =>
        val balanceDetails = fds.balanceDetails
        val codedOutChargeItem = {
          fds.toChargeItem()
            .filter(_.subTransactionType.contains(Accepted))
            .find(_.taxYear.endYear == (dateService.getCurrentTaxYearEnd - 1))
        }

        val whatYouOweChargesList = WhatYouOweChargesList(
          balanceDetails = balanceDetails,
          chargesList = getFilteredChargesList(fds, isReviewAndReconciledEnabled, isFilterCodedOutPoasEnabled),
          codedOutDocumentDetail = codedOutChargeItem)

        callOutstandingCharges(mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration, dateService.getCurrentTaxYearEnd).map {
          case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
          case _ => whatYouOweChargesList
        }
      case Some(error: FinancialDetailsErrorModel) =>
        Logger("application").error(s"error model in unpaid charges: $error")
        throw new Exception("Error response while getting Unpaid financial details")
      case None =>
        val whatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))
        callOutstandingCharges(mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration, dateService.getCurrentTaxYearEnd).map {
          case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
          case _ => whatYouOweChargesList
        }
      case _ =>
        throw new Exception("Error response while getting Unpaid financial details")
    }
  }

  private def callOutstandingCharges(saUtr: Option[String], yearOfMigration: Option[String], currentTaxYear: Int)
                                    (implicit headerCarrier: HeaderCarrier): Future[Option[OutstandingChargesModel]] = {
    if (saUtr.isDefined && yearOfMigration.isDefined && yearOfMigration.get.toInt >= currentTaxYear - 1) {
      val saPreviousYear = yearOfMigration.get.toInt - 1
      outstandingChargesConnector.getOutstandingCharges("utr", saUtr.get, saPreviousYear.toString) map {
        case outstandingChargesModel: OutstandingChargesModel => Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == 404 => None
        case _ => throw new Exception("Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  private def getFilteredChargesList(financialDetails: FinancialDetailsModel,
                                     isReviewAndReconcileEnabled: Boolean,
                                     isFilterCodedOutPoasEnabled: Boolean)
                                    (implicit user: MtdItUser[_]): List[ChargeItem] = {

    def getChargeItem(financialDetails: List[FinancialDetail]): DocumentDetail => Option[ChargeItem] =
      getChargeItemOpt(financialDetails)

    financialDetails
      .getAllDocumentDetailsWithDueDates()
      .flatMap(dd => getChargeItem(financialDetails.financialDetails)(dd.documentDetail))
      .filter(validChargeTypeCondition)
      .filterNot(_.subTransactionType.contains(Accepted))
      .filterNot(_.isReviewAndReconcileCharge && !isReviewAndReconcileEnabled)
      .filter(_.remainingToPayByChargeOrInterest > 0)
      .filter(_.notCodedOutPoa(isFilterCodedOutPoasEnabled))
      .sortBy(_.dueDate.get)
  }
}
