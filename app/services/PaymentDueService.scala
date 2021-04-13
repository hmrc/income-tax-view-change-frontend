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
import connectors.IncomeTaxViewChangeConnector
import models.financialDetails.{Charge, FinancialDetailsErrorModel, FinancialDetailsModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import uk.gov.hmrc.http.HeaderCarrier
import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class PaymentDueService @Inject()(val financialDetailsService: FinancialDetailsService,
                                  val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector)
                                 (implicit ec: ExecutionContext, appConfig: FrontendAppConfig) {

  implicit lazy val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  def getWhatYouOweChargesList()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {
    financialDetailsService.getAllUnpaidFinancialDetails flatMap {
      case financialDetails if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("[PaymentDueService][getWhatYouOweChargesList] Error response while getting Unpaid financial details")
      case financialDetails: List[FinancialDetailsModel] =>
        callOutstandingCharges(mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration, mtdUser.incomeSources.getCurrentTaxEndYear).map {
          case Some(outstandingChargesModel) => WhatYouOweChargesList(overduePaymentList = getOverduePaymentsList(financialDetails),
            dueInThirtyDaysList = getDueWithinThirtyDaysList(financialDetails), futurePayments = getFuturePaymentsList(financialDetails),
            outstandingChargesModel = Some(outstandingChargesModel))
          case _ => WhatYouOweChargesList(overduePaymentList = getOverduePaymentsList(financialDetails),
            dueInThirtyDaysList = getDueWithinThirtyDaysList(financialDetails), futurePayments = getFuturePaymentsList(financialDetails))
        }
    }
  }

  private def callOutstandingCharges(saUtr: Option[String], yearOfMigration: Option[String], currentTaxYear: Int)
                            (implicit headerCarrier: HeaderCarrier): Future[Option[OutstandingChargesModel]] = {
    if(saUtr.isDefined && yearOfMigration.isDefined && yearOfMigration.get.toInt >= currentTaxYear - 1) {
			val saPreviousYear = yearOfMigration.get.toInt - 1
			incomeTaxViewChangeConnector.getOutstandingCharges("utr", saUtr.get.toLong, saPreviousYear.toString) map {
        case outstandingChargesModel: OutstandingChargesModel => Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == 404 => None
        case _ => throw new Exception("[PaymentDueService][callOutstandingCharges] Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  private def whatYourOwePageDataExists(charge: Charge): Boolean = charge.mainType.isDefined && charge.due.isDefined && charge.outstandingAmount.isDefined

  private def getDueWithinThirtyDaysList(financialDetailsList: List[FinancialDetailsModel]): List[Charge] = financialDetailsList.flatMap(financialDetails =>
		financialDetails.financialDetails.filter(charge => whatYourOwePageDataExists(charge)
			&& (charge.mainType.get == "SA Payment on Account 1" || charge.mainType.get == "SA Payment on Account 2")
			&& charge.outstandingAmount.get > 0
			&& LocalDate.now().isAfter(charge.due.get.minusDays(31))
			&& LocalDate.now().isBefore(charge.due.get.plusDays(1)))).sortBy(_.due.get)

  private def getFuturePaymentsList(financialDetailsList: List[FinancialDetailsModel]): List[Charge] = financialDetailsList.flatMap(financialDetails =>
		financialDetails.financialDetails.filter(charge => whatYourOwePageDataExists(charge)
			&& (charge.mainType.get == "SA Payment on Account 1" || charge.mainType.get == "SA Payment on Account 2")
			&& charge.outstandingAmount.get > 0
			&& LocalDate.now().isBefore(charge.due.get.minusDays(30)))).sortBy(_.due.get)

  private def getOverduePaymentsList(financialDetailsList: List[FinancialDetailsModel]): List[Charge] = financialDetailsList.flatMap(financialDetails =>
		financialDetails.financialDetails.filter(charge => whatYourOwePageDataExists(charge)
			&& (charge.mainType.get == "SA Payment on Account 1" || charge.mainType.get == "SA Payment on Account 2")
			&& charge.outstandingAmount.get > 0
			&& charge.due.get.isBefore(LocalDate.now()))).sortBy(_.due.get)

}
