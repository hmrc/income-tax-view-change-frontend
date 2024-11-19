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
import connectors.FinancialDetailsConnector
import models.admin.FilterCodedOutPoas
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouOweService @Inject()(val financialDetailsService: FinancialDetailsService,
                                  val financialDetailsConnector: FinancialDetailsConnector,
                                  implicit val dateService: DateServiceInterface)
                                 (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig)
  extends TransactionUtils with FeatureSwitching {

  implicit lazy val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val validChargeTypeCondition: ChargeItem => Boolean = chargeItem => {
    (chargeItem.transactionType, chargeItem.subTransactionType) match {
      case (_, Some(Nics2)) => true
      case (PaymentOnAccountOne | PaymentOnAccountTwo | PaymentOnAccountOneReviewAndReconcile
            | PaymentOnAccountTwoReviewAndReconcile | BalancingCharge | MfaDebitCharge, _) => true
      case (_, _) => false
    }
  }

  def getWhatYouOweChargesList(isCodingOutEnabled: Boolean, isReviewAndReconcile: Boolean, isFilterCodedOutPoasEnabled: Boolean)
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {
    {
      for {
        unpaidChanges <- financialDetailsService.getAllUnpaidFinancialDetails(isCodingOutEnabled)
      } yield getWhatYouOweChargesList(unpaidChanges, isCodingOutEnabled, isReviewAndReconcile, isFilterCodedOutPoasEnabled)
    }.flatten
  }

  def getWhatYouOweChargesList(unpaidCharges: List[FinancialDetailsResponseModel],
                               isCodingOutEnabled: Boolean,
                               isReviewAndReconciledEnabled: Boolean,
                               isFilterCodedOutPoasEnabled: Boolean)
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {

    unpaidCharges match {
      case financialDetails: List[FinancialDetailsResponseModel] if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("Error response while getting Unpaid financial details")
      case financialDetails =>
        val financialDetailsModelList = financialDetails.asInstanceOf[List[FinancialDetailsModel]]
        val balanceDetails = financialDetailsModelList.headOption
          .map(_.balanceDetails).getOrElse(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))
        val codedOutChargeItem = if (isCodingOutEnabled) {
          financialDetailsModelList.flatMap(_.toChargeItem(isCodingOutEnabled, isReviewAndReconciledEnabled))
            .filter(_.subTransactionType.contains(Accepted))
            .find(_.taxYear.endYear == (dateService.getCurrentTaxYearEnd - 1))
        } else None

        val whatYouOweChargesList = WhatYouOweChargesList(
          balanceDetails = balanceDetails,
          chargesList = getFilteredChargesList(financialDetailsModelList, isCodingOutEnabled, isReviewAndReconciledEnabled, isFilterCodedOutPoasEnabled),
          codedOutDocumentDetail = codedOutChargeItem)

        callOutstandingCharges(mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration, dateService.getCurrentTaxYearEnd).map {
          case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
          case _ => whatYouOweChargesList
        }
    }
  }

  private def callOutstandingCharges(saUtr: Option[String], yearOfMigration: Option[String], currentTaxYear: Int)
                                    (implicit headerCarrier: HeaderCarrier): Future[Option[OutstandingChargesModel]] = {
    if (saUtr.isDefined && yearOfMigration.isDefined && yearOfMigration.get.toInt >= currentTaxYear - 1) {
      val saPreviousYear = yearOfMigration.get.toInt - 1
      financialDetailsConnector.getOutstandingCharges("utr", saUtr.get, saPreviousYear.toString) map {
        case outstandingChargesModel: OutstandingChargesModel => Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == 404 => None
        case _ => throw new Exception("Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  private def getFilteredChargesList(financialDetailsList: List[FinancialDetailsModel],
                                     isCodingOutEnabled: Boolean, isReviewAndReconciled: Boolean, isFilterCodedOutPoasEnabled: Boolean)
                                    (implicit user: MtdItUser[_]): List[ChargeItem] = {

    def getChargeItem(financialDetails: List[FinancialDetail]): DocumentDetail => Option[ChargeItem] =
      getChargeItemOpt(
        codingOutEnabled = isCodingOutEnabled,
        reviewAndReconcileEnabled = isReviewAndReconciled
      )(financialDetails)

    financialDetailsList
      .flatMap(financialDetails =>  {
        financialDetails.getAllDocumentDetailsWithDueDates(isCodingOutEnabled)
          .flatMap(dd => getChargeItem(financialDetails.financialDetails)(dd.documentDetail))})
      .filter(validChargeTypeCondition)
      .filterNot(_.subTransactionType.contains(Accepted))
      .filter(_.remainingToPayByChargeOrInterest > 0)
      .filter(_.notCodedOutPoa(isFilterCodedOutPoasEnabled))
      .sortBy(_.dueDate.get)
  }
}
