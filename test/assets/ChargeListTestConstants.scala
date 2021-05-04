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

import assets.BaseTestConstants.taxYear
import assets.FinancialDetailsTestConstants.testFinancialDetailsModel
import models.financialDetails.{Charge, FinancialDetailsModel, SubItem, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}

object ChargeListTestConstants {

  val outstandingAmount1: BigDecimal = 50
  val outstandingAmount2: BigDecimal = 75

  def outstandingChargesModel(dueDate: String) = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  val financialDetailsDueInMoreThan30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusDays(45).toString), Some(LocalDate.now().plusDays(50).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val financialDetailsDueInMoreThan30DaysPartial = FinancialDetailsModel(List(
    Charge(taxYear, "1040000124", Some("2019-05-16"), Some("POA1"), Some(43.21), Some(10.34), Some(outstandingAmount1),Some(10.34),
      Some("POA1"), Some("Payment on account 1 of 2"),
      Some(Seq(
        SubItem(Some("003"), Some(110), Some("2019-05-17"), Some("03"), Some("C"), Some("C"), Some(5000),
          Some(LocalDate.now().plusDays(45).toString), Some("C"), Some("081203010026-000003"))
      )))
  ))

  val financialDetailsDueIn30Days = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().toString), Some(LocalDate.now().plusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val financialDetailsDueIn30DaysPartial = FinancialDetailsModel(List(
    Charge(taxYear, "1040000124", Some("2019-05-16"), Some("POA1"), Some(43.21), Some(10.34), Some(outstandingAmount2),Some(10.34),
      Some("POA1"), Some("Payment on account 2 of 2"),
      Some(Seq(
        SubItem(Some("003"), Some(110), Some("2019-05-17"), Some("03"), Some("C"), Some("C"), Some(5000),
          Some(LocalDate.now().plusDays(1).toString), Some("C"), Some("081203010026-000003"))
      )))
  ))

  val financialDetailsOverdueData = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val financialDetailsOverdueDataPartial = FinancialDetailsModel(List(
    Charge(taxYear, "1040000124", Some("2019-05-16"), Some("POA1"), Some(43.21), Some(10.34), Some(outstandingAmount1),Some(10.34),
      Some("POA1"), Some("Payment on account 1 of 2"),
      Some(Seq(
        SubItem(Some("003"), Some(110), Some("2019-05-17"), Some("03"), Some("C"), Some("C"), Some(5000),
          Some(LocalDate.now().minusDays(10).toString), Some("C"), Some("081203010026-000003"))
      )))
  ))

  val outstandingCharges = outstandingChargesModel(LocalDate.now().minusMonths(13).toString)

  val whatYouOweAllData = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueData.financialDetails,
    dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    outstandingChargesModel = Some(outstandingCharges))

  val whatYouOwePartialData = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueDataPartial.financialDetails,
    dueInThirtyDaysList = financialDetailsDueIn30DaysPartial.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30DaysPartial.financialDetails,
    outstandingChargesModel = Some(outstandingCharges))

  val whatYouOweFinancialDataWithoutOutstandingCharges = WhatYouOweChargesList(
    overduePaymentList = financialDetailsOverdueData.financialDetails,
    dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30Days.financialDetails)
}
