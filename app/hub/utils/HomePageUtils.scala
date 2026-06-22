/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.utils

import common.auth.MtdItUser
import common.config.featureswitch.FeatureSwitching
import common.models.admin.{FilterCodedOutPoas, PenaltiesAndAppeals}
import common.services.DateServiceInterface
import controllers.Execution.trampoline
import financials.models.*
import financials.models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import financials.services.WhatYouOweService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

trait HomePageUtils extends FeatureSwitching {
  val whatYouOweService: WhatYouOweService
  val dateService: DateServiceInterface

  def getOutstandingChargesModel(unpaidCharges: List[FinancialDetailsResponseModel])
                                        (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[List[OutstandingChargeModel]] =
    whatYouOweService.getWhatYouOweChargesList(
      unpaidCharges,
      isFilterCodedOutPoasEnabled = isEnabled(FilterCodedOutPoas),
      isPenaltiesEnabled = isEnabled(PenaltiesAndAppeals),
      mainChargeIsNotPaidFilter
    ) map {
      case WhatYouOweChargesList(_, _, Some(OutstandingChargesModel(outstandingCharges)), _) =>
        outstandingCharges.filter(_.isBalancingChargeDebit)
          .filter(_.relevantDueDate.isDefined)
      case _ => Nil
    }

  def calculateOverduePaymentsCount(paymentsDue: List[LocalDate], outstandingChargesModel: List[OutstandingChargeModel]): Int = {
    val overduePaymentsCountFromDate = paymentsDue.count(_.isBefore(dateService.getCurrentDate))
    val overdueChargesCount = outstandingChargesModel.flatMap(_.relevantDueDate).count(_.isBefore(dateService.getCurrentDate))

    overduePaymentsCountFromDate + overdueChargesCount
  }

  def getDueDates(unpaidCharges: List[FinancialDetailsResponseModel], isFilterOutCodedPoasEnabled: Boolean, penaltiesEnabled: Boolean): List[LocalDate] = {

    val chargesList =
      unpaidCharges.collect {
        case fdm: FinancialDetailsModel => fdm
      }

    val dueDatesFromChargeItems =
      whatYouOweService.getFilteredChargesList(
        financialDetailsList = chargesList,
        isFilterCodedOutPoasEnabled = isFilterOutCodedPoasEnabled,
        isPenaltiesEnabled = penaltiesEnabled,
        remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot = mainChargeIsNotPaidFilter
      ).flatMap(_.dueDate)

    val dueDatesFromUnpaidDocumentDetails =
      chargesList
        .flatMap(_.unpaidDocumentDetails())
        .filter(_.originalAmount > 0)
        .flatMap(_.getDueDate)

    val dueDates =
      if (dueDatesFromChargeItems.nonEmpty) dueDatesFromChargeItems
      else dueDatesFromUnpaidDocumentDetails

    dueDates
      .sortWith(_ isBefore _)
      .sortBy(_.toEpochDay())
  }

  def getRelevantDates(outstandingCharges: List[OutstandingChargeModel]): List[LocalDate] =
    outstandingCharges
      .collect { case OutstandingChargeModel(_, relevantDate, _, _) => relevantDate }
      .flatten

  def mergePaymentsDue(paymentsDue: List[LocalDate], outstandingChargesDueDate: List[LocalDate]): Option[LocalDate] =
    (paymentsDue ::: outstandingChargesDueDate)
      .sortWith(_ isBefore _)
      .headOption

  def yourTasksUrl(origin: Option[String] = None, isAgent: Boolean): String = if (isAgent) hub.controllers.newHomePage.routes.HandleYourTasksController.showAgent().url else hub.controllers.newHomePage.routes.HandleYourTasksController.show().url
  def recentActivityUrl(origin: Option[String] = None, isAgent: Boolean): String = hub.controllers.newHomePage.routes.RecentActivityController.show(isAgent, origin).url
  def overviewUrl(origin: Option[String] = None, isAgent: Boolean): String = hub.controllers.routes.HomeController.handleOverview(origin, isAgent).url
  def helpUrl(origin: Option[String] = None, isAgent: Boolean): String = hub.controllers.routes.HomeController.handleHelp(origin, isAgent).url

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid => x
  }
}
