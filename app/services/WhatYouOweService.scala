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
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
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

  def getWhatYouOweChargesList(unpaidCharges: List[FinancialDetailsResponseModel],
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
        val codedOutChargeItem = {
          financialDetailsModelList.flatMap(_.toChargeItem())
            .filter(_.subTransactionType.contains(Accepted))
            .find(_.taxYear.endYear == (dateService.getCurrentTaxYearEnd - 1))
        }

        val whatYouOweChargesList = WhatYouOweChargesList(
          balanceDetails = balanceDetails,
          chargesList = getFilteredChargesList(financialDetailsModelList, isReviewAndReconciledEnabled, isFilterCodedOutPoasEnabled),
          codedOutDocumentDetail = codedOutChargeItem)

        (mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration) match {
          case (Some(_), Some(year)) =>
            callOutstandingCharges(dateService.getCurrentTaxYear, year).map {
              case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
              case _ => whatYouOweChargesList
            }
          case _ => Future.successful(whatYouOweChargesList)
        }
    }
  }

  private def callOutstandingCharges(currentTaxYear: TaxYear, yearOfMigration: String)
                                    (implicit headerCarrier: HeaderCarrier,mtdUser: MtdItUser[_]):Future[Option[OutstandingChargesModel]] = {

    if (yearOfMigration.toInt >= currentTaxYear.startYear) {
      val saPreviousYear = mtdUser.incomeSources.yearOfMigration.get.toInt - 1
      outstandingChargesConnector.getOutstandingCharges("utr", mtdUser.saUtr.get, saPreviousYear.toString) map {
        case outstandingChargesModel: OutstandingChargesModel => Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == 404 => None
        case _ => throw new Exception("Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  private def getFilteredChargesList(financialDetailsList: List[FinancialDetailsModel],
                                     isReviewAndReconcileEnabled: Boolean,
                                     isFilterCodedOutPoasEnabled: Boolean)
                                    (implicit user: MtdItUser[_]): List[ChargeItem] = {

    def getChargeItem(financialDetails: List[FinancialDetail]): DocumentDetail => Option[ChargeItem] =
      getChargeItemOpt(financialDetails)

    financialDetailsList
      .flatMap(financialDetails =>  {
        financialDetails
          .getAllDocumentDetailsWithDueDates()
          .flatMap(dd => getChargeItem(financialDetails.financialDetails)(dd.documentDetail))})
      .filter(validChargeTypeCondition)
      .filterNot(_.subTransactionType.contains(Accepted))
      .filterNot(_.isReviewAndReconcileCharge && !isReviewAndReconcileEnabled)
      .filter(_.remainingToPayByChargeOrInterest > 0)
      .filter(_.notCodedOutPoa(isFilterCodedOutPoasEnabled))
      .sortBy(_.dueDate.get)
  }
}
