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

package assets

import java.time.LocalDate

import assets.FinancialDetailsIntegrationTestConstants.testFinancialDetailsModel
import models.financialDetails.WhatYouOweChargesList
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}

object ChargeListIntegrationTestConstants {

  def outstandingChargesModel(dueDate: String) = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  val financialDetailsDueInMoreThan30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val financialDetailsDueIn30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val financialDetailsOverdueData = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val outstandingCharges = outstandingChargesModel(LocalDate.now().minusMonths(13).toString)

  val whatYouOweAllData = WhatYouOweChargesList(dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    overduePaymentList = financialDetailsOverdueData.financialDetails,
    outstandingChargesModel = Some(outstandingCharges))

  val whatYouOweFinancialDataWithoutOutstandingCharges = WhatYouOweChargesList(dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    overduePaymentList = financialDetailsOverdueData.financialDetails)
}
