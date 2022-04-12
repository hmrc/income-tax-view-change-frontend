/*
 * Copyright 2022 HM Revenue & Customs
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

package views

import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testConstants.PaymentAllocationsTestConstants.{singleTestPaymentAllocationChargeWithOutstandingAmountZero, _}
import auth.MtdItUser
import config.FrontendAppConfig
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import models.financialDetails.DocumentDetail
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.PaymentAllocation
import testConstants.MessagesLookUp.{PaymentAllocation => paymentAllocationMessages}

import scala.collection.JavaConverters._


class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val paymentAllocationView = app.injector.instanceOf[PaymentAllocation]

  lazy val backUrl: String = controllers.routes.PaymentHistoryController.viewPaymentHistory().url

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    btaNavPartial = None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  val singleTestPaymentAllocationChargeWithOutstandingAmountZero: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail.copy(outstandingAmount = Some(0))),
    List(financialDetail)
  )

  class PaymentAllocationSetup(viewModel: PaymentAllocationViewModel = paymentAllocationViewModel) extends Setup(
    paymentAllocationView(viewModel, backUrl)) {
    paymentAllocationViewModel.originalPaymentAllocationWithClearingDate(0).allocationDetail.get.chargeType.get
  }

  "Payment Allocation Page for non LPI" should {
    "check that the first section information is present" when {
      "checking the heading" in new PaymentAllocationSetup() {
        document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
      }

      s"have the correct date of ${paymentAllocationMessages.date}" in new PaymentAllocationSetup {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").eq(0).text shouldBe paymentAllocationMessages.date
      }

      s"have the correct Amount of ${paymentAllocationMessages.amount}" in new PaymentAllocationSetup {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").last.text shouldBe paymentAllocationMessages.amount
      }

      "checking there is the info text" in new PaymentAllocationSetup() {
        document.getElementsByClass("govuk-inset-text").text shouldBe paymentAllocationMessages.info
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in new PaymentAllocationSetup() {
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationMessages.paymentAllocationHeading


      }

      "has table headers" in new PaymentAllocationSetup() {
        val allTableHeadings = document.selectHead("thead")
        allTableHeadings.selectNth("th", 1).text() shouldBe paymentAllocationMessages.tableHeadings(0)
        allTableHeadings.selectNth("th", 2).text() shouldBe paymentAllocationMessages.tableHeadings(1)
        allTableHeadings.selectNth("th", 3).text() shouldBe paymentAllocationMessages.tableHeadings(2)

      }

      "has a payment within the table" in new PaymentAllocationSetup() {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        allTableData.selectNth("td", 1).text() shouldBe paymentAllocationMessages.tableDataPaymentAllocation
        allTableData.selectNth("td", 2).text() shouldBe paymentAllocationMessages.tableDataDateAllocated
        allTableData.selectNth("td", 3).text() shouldBe paymentAllocationMessages.tableDataAmount

      }

      "has a Credit on account row within payment details" in new PaymentAllocationSetup() {
        val allTableData = document.getElementById("credit-on-account").getElementsByTag("td")
        allTableData.get(0).text() shouldBe paymentAllocationMessages.creditOnAccount
        allTableData.get(2).text() shouldBe paymentAllocationMessages.creditOnAccountAmount

      }

      "should not have Credit on account row within payment details" in new PaymentAllocationSetup(paymentAllocationViewModel.copy(
        paymentAllocationChargeModel = singleTestPaymentAllocationChargeWithOutstandingAmountZero)) {
        document.getElementById("credit-on-account") shouldBe null
      }
    }
  }
  "Payment Allocation Page for LPI" should {
    "check that the first section information is present" when {
      "checking the heading" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
      }
      s"have the correct date of ${paymentAllocationMessages.date}" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").eq(0).text shouldBe paymentAllocationMessages.date
      }

      s"have the correct Amount of ${paymentAllocationMessages.amount}" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").last.text shouldBe paymentAllocationMessages.amount
      }

      "checking there is the info text" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.getElementsByClass("govuk-inset-text").text shouldBe paymentAllocationMessages.info
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationMessages.paymentAllocationHeading
      }

      "has table headers" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        val allTableHeadings = document.selectHead("thead")
        allTableHeadings.selectNth("th", 1).text() shouldBe paymentAllocationMessages.tableHeadings(0)
        allTableHeadings.selectNth("th", 2).text() shouldBe paymentAllocationMessages.tableHeadings(1)
        allTableHeadings.selectNth("th", 3).text() shouldBe paymentAllocationMessages.tableHeadings(2)

      }

      "has a payment within the table" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        allTableData.selectNth("td", 1).text() shouldBe paymentAllocationMessages.tableDataPaymentAllocationLpi
        allTableData.selectNth("td", 2).text() shouldBe paymentAllocationMessages.tableDataDateAllocatedLpi
        allTableData.selectNth("td", 3).text() shouldBe paymentAllocationMessages.tableDataAmountLpi

      }
    }
  }

  "Payment Allocation Page" should {
    s"have the title: ${paymentAllocationMessages.title}" in new PaymentAllocationSetup {
      document.title() shouldBe paymentAllocationMessages.title
    }

    s"have the heading: ${paymentAllocationMessages.heading}" in new PaymentAllocationSetup {
      document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
    }

    "have a back link" in new PaymentAllocationSetup {
      document.backLink.text shouldBe paymentAllocationMessages.backLink
      document.hasBackLinkTo(controllers.routes.PaymentHistoryController.viewPaymentHistory().url)
    }

    "have Payment allocations table" when {

      def allocationDetail(sapDocNumber: String, taxPeriodEndDate: String,
                           mainType: String, chargeType: String, amount: BigDecimal): AllocationDetail =
        AllocationDetail(transactionId = Some(sapDocNumber), Some("2017-03-23"), to = Some(taxPeriodEndDate),
          chargeType = Some(chargeType), mainType = Some(mainType), amount = Some(amount), Some(1.23), Some("chargeReference1"))

      def viewModel(allocationDetailWithClearingDate: (AllocationDetail, String)*): PaymentAllocationViewModel = {

        PaymentAllocationViewModel(paymentAllocationChargesModel,
          originalPaymentAllocationWithClearingDate = allocationDetailWithClearingDate.map {
            case (allocationDetail, clearingDate) => AllocationDetailWithClearingDate(Some(allocationDetail), Some(clearingDate))
          }
        )
      }

      val POA1 = "SA Payment on Account 1"
      val POA2 = "SA Payment on Account 2"
      val BAL_CHARGE = "SA Balancing Charge"

      "a payment on account 1 of 2" in new PaymentAllocationSetup(viewModel(
        allocationDetail("poa1_1", "2018-03-15", POA1, "ITSA England & NI", 1234.56) -> "2019-06-27",
        allocationDetail("poa1_2", "2018-04-05", POA1, "ITSA NI", 2345.67) -> "2019-06-28",
        allocationDetail("poa1_3", "2018-04-06", POA1, "ITSA Scotland", 3456.78) -> "2019-06-29",
        allocationDetail("poa1_4", "2018-06-23", POA1, "ITSA Wales", 4567.89) -> "2019-06-30",
        allocationDetail("poa1_5", "2018-12-31", POA1, "NIC4-GB", 9876.54) -> "2019-08-27",
        allocationDetail("poa1_6", "2019-01-01", POA1, "NIC4 Scotland", 8765.43) -> "2019-08-28",
        allocationDetail("poa1_7", "2019-04-05", POA1, "NIC4 Wales", 7654.32) -> "2019-08-29",
        allocationDetail("poa1_8", "2019-04-06", POA1, "NIC4-NI", 6543.21) -> "2019-08-30")) {

        val expectedLinkUrls = Seq(
          controllers.routes.ChargeSummaryController.show(2018, "poa1_1").url,
          controllers.routes.ChargeSummaryController.show(2018, "poa1_2").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_3").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_4").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_5").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_6").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_7").url,
          controllers.routes.ChargeSummaryController.show(2020, "poa1_8").url
        )

        layoutContent.h2.text() shouldBe paymentAllocationMessages.allocationsTableHeading
        layoutContent.selectById("payment-allocation-table").text() shouldBe
          s"""
             |${paymentAllocationMessages.allocationsTableHeadersText}
             |Income Tax for payment on account 1 of 2 2018 Tax year 2017 to 2018 27 Jun 2019 £1,234.56
             |Income Tax for payment on account 1 of 2 2018 Tax year 2017 to 2018 28 Jun 2019 £2,345.67
             |Income Tax for payment on account 1 of 2 2019 Tax year 2018 to 2019 29 Jun 2019 £3,456.78
             |Income Tax for payment on account 1 of 2 2019 Tax year 2018 to 2019 30 Jun 2019 £4,567.89
             |Class 4 National Insurance for payment on account 1 of 2 2019 Tax year 2018 to 2019 27 Aug 2019 £9,876.54
             |Class 4 National Insurance for payment on account 1 of 2 2019 Tax year 2018 to 2019 28 Aug 2019 £8,765.43
             |Class 4 National Insurance for payment on account 1 of 2 2019 Tax year 2018 to 2019 29 Aug 2019 £7,654.32
             |Class 4 National Insurance for payment on account 1 of 2 2020 Tax year 2019 to 2020 30 Aug 2019 £6,543.21
             |Credit on account £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        layoutContent.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }

      "a payment on account 2 of 2" in new PaymentAllocationSetup(viewModel(
        allocationDetail("poa2_1", "2018-03-15", POA2, "ITSA England & NI", 1234.56) -> "2019-06-27",
        allocationDetail("poa2_2", "2018-04-05", POA2, "ITSA NI", 2345.67) -> "2019-06-28",
        allocationDetail("poa2_3", "2018-04-06", POA2, "ITSA Scotland", 3456.78) -> "2019-06-29",
        allocationDetail("poa2_4", "2018-06-23", POA2, "ITSA Wales", 4567.89) -> "2019-06-30",
        allocationDetail("poa2_5", "2018-12-31", POA2, "NIC4-GB", 9876.54) -> "2019-08-27",
        allocationDetail("poa2_6", "2019-01-01", POA2, "NIC4 Scotland", 8765.43) -> "2019-08-28",
        allocationDetail("poa2_7", "2019-04-05", POA2, "NIC4 Wales", 7654.32) -> "2019-08-29",
        allocationDetail("poa2_8", "2019-04-06", POA2, "NIC4-NI", 6543.21) -> "2019-08-30")) {

        val expectedLinkUrls = Seq(
          controllers.routes.ChargeSummaryController.show(2018, "poa2_1").url,
          controllers.routes.ChargeSummaryController.show(2018, "poa2_2").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_3").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_4").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_5").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_6").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_7").url,
          controllers.routes.ChargeSummaryController.show(2020, "poa2_8").url
        )

        layoutContent.h2.text() shouldBe paymentAllocationMessages.allocationsTableHeading
        layoutContent.selectById("payment-allocation-table").text() shouldBe
          s"""
             |${paymentAllocationMessages.allocationsTableHeadersText}
             |Income Tax for payment on account 2 of 2 2018 Tax year 2017 to 2018 27 Jun 2019 £1,234.56
             |Income Tax for payment on account 2 of 2 2018 Tax year 2017 to 2018 28 Jun 2019 £2,345.67
             |Income Tax for payment on account 2 of 2 2019 Tax year 2018 to 2019 29 Jun 2019 £3,456.78
             |Income Tax for payment on account 2 of 2 2019 Tax year 2018 to 2019 30 Jun 2019 £4,567.89
             |Class 4 National Insurance for payment on account 2 of 2 2019 Tax year 2018 to 2019 27 Aug 2019 £9,876.54
             |Class 4 National Insurance for payment on account 2 of 2 2019 Tax year 2018 to 2019 28 Aug 2019 £8,765.43
             |Class 4 National Insurance for payment on account 2 of 2 2019 Tax year 2018 to 2019 29 Aug 2019 £7,654.32
             |Class 4 National Insurance for payment on account 2 of 2 2020 Tax year 2019 to 2020 30 Aug 2019 £6,543.21
             |Credit on account £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        layoutContent.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }

      "a balancing charge" in new PaymentAllocationSetup(viewModel(
        allocationDetail("bcd_1", "2018-03-15", BAL_CHARGE, "ITSA Scotland", 1234.56) -> "2019-06-27",
        allocationDetail("bcd_2", "2018-04-05", BAL_CHARGE, "NIC4 Wales", 2345.67) -> "2019-06-28",
        allocationDetail("bcd_3", "2018-04-06", BAL_CHARGE, "NIC2-GB", 3456.78) -> "2019-06-29",
        allocationDetail("bcd_4", "2019-01-01", BAL_CHARGE, "CGT", 9876.54) -> "2019-08-27",
        allocationDetail("bcd_5", "2019-04-05", BAL_CHARGE, "SL", 8765.43) -> "2019-08-28",
        allocationDetail("bcd_6", "2019-04-06", BAL_CHARGE, "Voluntary NIC2-NI", 7654.32) -> "2019-08-29")) {

        val expectedLinkUrls = Seq(
          controllers.routes.ChargeSummaryController.show(2018, "bcd_1").url,
          controllers.routes.ChargeSummaryController.show(2018, "bcd_2").url,
          controllers.routes.ChargeSummaryController.show(2019, "bcd_3").url,
          controllers.routes.ChargeSummaryController.show(2019, "bcd_4").url,
          controllers.routes.ChargeSummaryController.show(2019, "bcd_5").url,
          controllers.routes.ChargeSummaryController.show(2020, "bcd_6").url
        )

        layoutContent.h2.text() shouldBe paymentAllocationMessages.allocationsTableHeading
        layoutContent.selectById("payment-allocation-table").text() shouldBe
          s"""
             |${paymentAllocationMessages.allocationsTableHeadersText}
             |Income Tax for Balancing payment 2018 Tax year 2017 to 2018 27 Jun 2019 £1,234.56
             |Class 4 National Insurance for Balancing payment 2018 Tax year 2017 to 2018 28 Jun 2019 £2,345.67
             |Class 2 National Insurance for Balancing payment 2019 Tax year 2018 to 2019 29 Jun 2019 £3,456.78
             |Capital Gains Tax for Balancing payment 2019 Tax year 2018 to 2019 27 Aug 2019 £9,876.54
             |Student Loans for Balancing payment 2019 Tax year 2018 to 2019 28 Aug 2019 £8,765.43
             |Voluntary Class 2 National Insurance for Balancing payment 2020 Tax year 2019 to 2020 29 Aug 2019 £7,654.32
             |Credit on account £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        layoutContent.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }
    }

    "have a Credit on account row within payment details" in new PaymentAllocationSetup() {
      val allTableData: Elements = document.getElementById("credit-on-account").getElementsByTag("td")
      allTableData.get(0).text() shouldBe paymentAllocationMessages.creditOnAccount
      allTableData.get(2).text() shouldBe paymentAllocationMessages.creditOnAccountAmount

    }

    "not have Credit on account row within payment details" in new PaymentAllocationSetup(paymentAllocationViewModel.copy(
      paymentAllocationChargeModel = singleTestPaymentAllocationChargeWithOutstandingAmountZero)) {
      document.getElementById("credit-on-account") shouldBe null
    }

    "The payments allocation view has NO payment allocation amount" should {
      "throw a MissingFieldException" in {
        val thrownException = intercept[MissingFieldException] {
          paymentAllocationView(paymentAllocationViewModelWithNoOriginalAmount, backUrl)
        }
        thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Payment Allocation Amount"
      }
    }

    "The payments allocation view has Allocation Detail but no clearing date" should {
      "throw a MissingFieldException" in {
        val thrownException = intercept[MissingFieldException] {
          paymentAllocationView(paymentAllocationViewModelWithNoClearingAmount, backUrl)
        }
        thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Payment Clearing Date"
      }
    }
  }
}
