package testConstants

import java.time.LocalDate

import BaseIntegrationTestConstants.{otherTestSelfEmploymentId, testSelfEmploymentId}
import models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import models.incomeSourceDetails.BusinessDetailsModel


object PaymentHistoryTestConstraints {

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if(currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6)))LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val b1CessationDate = LocalDate.of(2017,12,31)
  val b1CessationReason = "It really, really was a bad idea"
  val b1TradingStart = "2017-01-01"
  val b1TradingName = "business"
  val b1AccountingStart = LocalDate.of(2017, 1, 1)
  val b1AccountingEnd = LocalDate.of(2017,12,31)
  val b1AddressLine1 = "64 Zoo Lane"
  val b1AddressLine2 = "Happy Place"
  val b1AddressLine3 = "Magical Land"
  val b1AddressLine4 = "England"
  val b1AddressLine5 = "ZL1 064"
  val b1CountryCode = "UK"

  val b2CessationDate = LocalDate.of(2018,12,31)
  val b2CessationReason = "It really, really was a bad idea"
  val b2TradingStart = "2018-01-01"
  val b2TradingName = "secondBusiness"
  val b2AccountingStart = LocalDate.of(2018,1,1)
  val b2AccountingEnd = LocalDate.of(2018,12,31)
  val b2AddressLine1 = "742 Evergreen Terrace"
  val b2AddressLine2 = "Springfield"
  val b2AddressLine3 = "Oregon"
  val b2AddressLine4 = "USA"
  val b2AddressLine5 = "51MP 50N5"
  val b2CountryCode = "USA"
  val testMtdItId = "XIAT0000000000A"

  val oldBusiness1 = BusinessDetailsModel(
    incomeSourceId = testSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(
      start = b1AccountingStart,
      end = b1AccountingEnd
    ),
    tradingName = Some(b1TradingName),
    firstAccountingPeriodEndDate = Some(getCurrentTaxYearEnd.minusYears(1))
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = otherTestSelfEmploymentId,
    accountingPeriod = AccountingPeriodModel(
      start = b2AccountingStart,
      end = b2AccountingEnd
    ),
    tradingName = Some(b2TradingName),
    firstAccountingPeriodEndDate = Some(b2AccountingEnd)
  )

}
