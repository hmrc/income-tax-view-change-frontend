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

package views.agent

import assets.PaymentAllocationsTestConstants._
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import org.jsoup.select.Elements
import testUtils.ViewSpec
import views.html.agent.PaymentAllocation

import scala.collection.JavaConverters._

class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val paymentAllocation: PaymentAllocation = app.injector.instanceOf[PaymentAllocation]

  lazy val backUrl: String = controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url


  object paymentAllocationMessages {
    val title = "Payment made to HMRC - Your client’s Income Tax details - GOV.UK"
    val heading = "Payment made to HMRC"
    val backLink = "Back"
    val date = "31 January 2021"
    val amount = "£300.00"
    val info = "Any payments made will automatically be allocated towards penalties and earlier tax years before current and future tax years."
    val allocationsTableHeading = "Payment allocations"
    val allocationsTableHeaders = Seq("Payment allocation", "Date allocated", "Amount")
    val allocationsTableHeadersText: String = allocationsTableHeaders.mkString(" ")
    val creditOnAccount = "Credit on account"
    val creditOnAccountAmount = "£200.00"
  }

  class PaymentAllocationSetup(viewModel: PaymentAllocationViewModel = paymentAllocationViewModel) extends Setup(
    paymentAllocation(viewModel, backUrl)
  )

  "Payment Allocation Page" should {
    s"have the title: ${paymentAllocationMessages.title}" in new PaymentAllocationSetup {
      document.title() shouldBe paymentAllocationMessages.title
    }

    s"have the heading: ${paymentAllocationMessages.heading}" in new PaymentAllocationSetup {
      document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
    }

    "have a back link" in new PaymentAllocationSetup {
      document.backLink.text shouldBe paymentAllocationMessages.backLink
      document.hasBackLinkTo(controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url)
    }
    s"have the correct date of ${paymentAllocationMessages.date}" in new PaymentAllocationSetup {
      document.selectById("payment-allocation-charge-table")
        .getElementsByTag("td").eq(1).text shouldBe paymentAllocationMessages.date
    }

    s"have the correct Amount of ${paymentAllocationMessages.amount}" in new PaymentAllocationSetup {
      document.selectById("payment-allocation-charge-table")
        .getElementsByTag("td").last.text shouldBe paymentAllocationMessages.amount
    }

    "have info text" in new PaymentAllocationSetup {
      document.getElementById("payments-allocation-info").text shouldBe paymentAllocationMessages.info
    }

    "have Payment allocations table" when {

      def allocationDetail(sapDocNumber: String, taxPeriodEndDate: String,
                           mainType: String, chargeType: String, amount: BigDecimal): AllocationDetail =
        AllocationDetail(transactionId = Some(sapDocNumber), Some("2017-03-23"), to = Some(taxPeriodEndDate),
          chargeType = Some(chargeType), mainType = Some(mainType), amount = Some(amount), Some(1.23), Some("chargeReference1"))

      def viewModel(allocationDetailWithClearingDate: (AllocationDetail, String)*): PaymentAllocationViewModel = {
        val paymentAllocations = testValidPaymentAllocationsModel.copy(allocations = allocationDetailWithClearingDate.map(_._1))

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
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2018, "poa1_1").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2018, "poa1_2").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa1_3").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa1_4").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa1_5").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa1_6").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa1_7").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2020, "poa1_8").url
        )

        content.h2.text() shouldBe paymentAllocationMessages.allocationsTableHeading
        content.selectById("payment-allocation-table").text() shouldBe
          s"""
             |${paymentAllocationMessages.allocationsTableHeadersText}
             |Income Tax for payment on account 1 of 2 Tax year 2017 to 2018 27 Jun 2019 £1,234.56
             |Income Tax for payment on account 1 of 2 Tax year 2017 to 2018 28 Jun 2019 £2,345.67
             |Income Tax for payment on account 1 of 2 Tax year 2018 to 2019 29 Jun 2019 £3,456.78
             |Income Tax for payment on account 1 of 2 Tax year 2018 to 2019 30 Jun 2019 £4,567.89
             |Class 4 National Insurance for payment on account 1 of 2 Tax year 2018 to 2019 27 Aug 2019 £9,876.54
             |Class 4 National Insurance for payment on account 1 of 2 Tax year 2018 to 2019 28 Aug 2019 £8,765.43
             |Class 4 National Insurance for payment on account 1 of 2 Tax year 2018 to 2019 29 Aug 2019 £7,654.32
             |Class 4 National Insurance for payment on account 1 of 2 Tax year 2019 to 2020 30 Aug 2019 £6,543.21
             |Credit on account £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        content.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
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
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2018, "poa2_1").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2018, "poa2_2").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa2_3").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa2_4").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa2_5").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa2_6").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "poa2_7").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2020, "poa2_8").url
        )

        content.h2.text() shouldBe paymentAllocationMessages.allocationsTableHeading
        content.selectById("payment-allocation-table").text() shouldBe
          s"""
             |${paymentAllocationMessages.allocationsTableHeadersText}
             |Income Tax for payment on account 2 of 2 Tax year 2017 to 2018 27 Jun 2019 £1,234.56
             |Income Tax for payment on account 2 of 2 Tax year 2017 to 2018 28 Jun 2019 £2,345.67
             |Income Tax for payment on account 2 of 2 Tax year 2018 to 2019 29 Jun 2019 £3,456.78
             |Income Tax for payment on account 2 of 2 Tax year 2018 to 2019 30 Jun 2019 £4,567.89
             |Class 4 National Insurance for payment on account 2 of 2 Tax year 2018 to 2019 27 Aug 2019 £9,876.54
             |Class 4 National Insurance for payment on account 2 of 2 Tax year 2018 to 2019 28 Aug 2019 £8,765.43
             |Class 4 National Insurance for payment on account 2 of 2 Tax year 2018 to 2019 29 Aug 2019 £7,654.32
             |Class 4 National Insurance for payment on account 2 of 2 Tax year 2019 to 2020 30 Aug 2019 £6,543.21
             |Credit on account £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        content.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
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
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2018, "bcd_1").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2018, "bcd_2").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "bcd_3").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "bcd_4").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2019, "bcd_5").url,
          controllers.agent.routes.ChargeSummaryController.showChargeSummary(2020, "bcd_6").url
        )

        content.h2.text() shouldBe paymentAllocationMessages.allocationsTableHeading
        content.selectById("payment-allocation-table").text() shouldBe
          s"""
             |${paymentAllocationMessages.allocationsTableHeadersText}
             |Income Tax for remaining balance Tax year 2017 to 2018 27 Jun 2019 £1,234.56
             |Class 4 National Insurance for remaining balance Tax year 2017 to 2018 28 Jun 2019 £2,345.67
             |Class 2 National Insurance for remaining balance Tax year 2018 to 2019 29 Jun 2019 £3,456.78
             |Capital Gains Tax for remaining balance Tax year 2018 to 2019 27 Aug 2019 £9,876.54
             |Student Loans for remaining balance Tax year 2018 to 2019 28 Aug 2019 £8,765.43
             |Voluntary Class 2 National Insurance for remaining balance Tax year 2019 to 2020 29 Aug 2019 £7,654.32
             |Credit on account £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        content.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }
    }

    "have a Credit on account row within payment details" in new PaymentAllocationSetup() {
      val allTableData: Elements =  document.getElementById("credit-on-account").getElementsByTag("td")
      "getting payment allocation information"
      allTableData.get(0).text() shouldBe paymentAllocationMessages.creditOnAccount
      "getting payment allocation Amount"
      allTableData.get(2).text() shouldBe paymentAllocationMessages.creditOnAccountAmount
    }

    "not have Credit on account row within payment details" in new PaymentAllocationSetup(paymentAllocationViewModel.copy(
      paymentAllocationChargeModel = singleTestPaymentAllocationChargeWithOutstandingAmountZero)) {
      document.getElementById("credit-on-account") shouldBe null
    }

  }

}
