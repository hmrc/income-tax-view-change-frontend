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

import assets.BaseIntegrationTestConstants.taxYear
import assets.FinancialDetailsIntegrationTestConstants.testFinancialDetailsModel
import models.financialDetails.{Charge, FinancialDetailsModel, SubItem, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}

object ChargeListIntegrationTestConstants {

  val outstandingAmount1: BigDecimal = 50
  val outstandingAmount2: BigDecimal = 75

  def outstandingChargesModel(dueDate: String): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("BCD", Some(dueDate), 123456789012345.67, 1234), OutstandingChargeModel("ACI", None, 12.67, 1234)))

  def outstandingChargesEmptyBCDModel(dueDate: String): OutstandingChargesModel = OutstandingChargesModel(
    List(OutstandingChargeModel("LATE", Some(dueDate), 123456789012345.67, 1234)))

  val financialDetailsDueInMoreThan30Days: FinancialDetailsModel = testFinancialDetailsModel(List(Some("SA Payment on Account 1"),
    Some("SA Payment on Account 2")), List(Some(LocalDate.now().plusDays(45).toString),
    Some(LocalDate.now().plusDays(50).toString)), List(Some(50), Some(75)), LocalDate.now().getYear.toString)

  val financialDetailsDueInMoreThan30DaysPartial: FinancialDetailsModel = FinancialDetailsModel(List(
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

  val financialDetailsOneZeroOutstandingAmountValue = FinancialDetailsModel(List(
    Charge(taxYear, "1040000124", Some("2019-05-16"), Some("POA1"), Some(43.21), Some(10.34), Some(100),Some(10.34),
      Some("POA1"), Some("Payment on account 1 of 2"),
      Some(Seq(
        SubItem(Some("003"), Some(110), Some("2019-05-17"), Some("03"), Some("C"), Some("C"), Some(5000),
          Some(LocalDate.now().plusDays(1).toString), Some("C"), Some("081203010026-000003"))
      )))
  ))

  val financialDetailsMultiChargePast = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().minusDays(15).toString), Some(LocalDate.now().minusDays(15).toString)),
    List(Some(2000), Some(2000)), LocalDate.now().getYear.toString)

  val financialDetailsMultiCharge = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().toString), Some(LocalDate.now().toString)),
    List(Some(2000), Some(2000)), LocalDate.now().getYear.toString)

  val financialDetailsMultiChargeFuture = testFinancialDetailsModel(List(Some("SA Payment on Account 1"), Some("SA Payment on Account 2")),
    List(Some(LocalDate.now().plusYears(1).toString), Some(LocalDate.now().plusYears(1).toString)),
    List(Some(2000), Some(2000)), LocalDate.now().getYear.toString)

  val outstandingCharges: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusMonths(13).toString)

  val outstandingChargesEmptyBCD: OutstandingChargesModel = outstandingChargesEmptyBCDModel(LocalDate.now().minusMonths(13).toString)

  val whatYouOweAllData: WhatYouOweChargesList = WhatYouOweChargesList(dueInThirtyDaysList = List.empty,
    futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    overduePaymentList = financialDetailsOverdueData.financialDetails,
    outstandingChargesModel = Some(outstandingCharges))

  val whatYouOweFinancialDataWithoutOutstandingCharges: WhatYouOweChargesList =
    WhatYouOweChargesList(dueInThirtyDaysList = financialDetailsDueIn30Days.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30Days.financialDetails,
    overduePaymentList = financialDetailsOverdueData.financialDetails)

  val whatYouOwePartialData: WhatYouOweChargesList = WhatYouOweChargesList(dueInThirtyDaysList = financialDetailsDueIn30DaysPartial.financialDetails,
    futurePayments = financialDetailsDueInMoreThan30DaysPartial.financialDetails,
    overduePaymentList = financialDetailsOverdueDataPartial.financialDetails,
    outstandingChargesModel = Some(outstandingCharges))

  val whatYouOweNoChargeList: WhatYouOweChargesList = WhatYouOweChargesList(List.empty, List.empty, List.empty)

  val whatYouOweFinancialDetailsEmptyBCDCharge: WhatYouOweChargesList = WhatYouOweChargesList(
    List.empty,
    financialDetailsMultiChargeFuture.financialDetails,
    List.empty,
    outstandingChargesModel = Some(outstandingChargesEmptyBCD))

  val whatYouOweOutstandingChargesOnly: WhatYouOweChargesList = WhatYouOweChargesList(outstandingChargesModel = Some(outstandingCharges))

  val whatYouOweMultiFinancialDetailsBCDACI: WhatYouOweChargesList = WhatYouOweChargesList(
    List.empty,
    financialDetailsMultiCharge.financialDetails,
    List.empty,
    outstandingChargesModel = Some(outstandingCharges))

  val whatYouOweMultiFinancialDetails: WhatYouOweChargesList = WhatYouOweChargesList(
    List.empty,
    financialDetailsMultiChargePast.financialDetails,
    List.empty)

  val financialDetailsOneZeroOutstandingAmount: WhatYouOweChargesList =
    WhatYouOweChargesList(
      financialDetailsOneZeroOutstandingAmountValue.financialDetails,
      List.empty,
      List.empty,
      outstandingChargesModel = Some(outstandingCharges))

  //Agents


}
