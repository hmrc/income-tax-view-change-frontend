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

import auth.MtdItUser
import config.FrontendAppConfig
import enums.ChargeType._
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import models.paymentAllocationCharges.{AllocationDetailWithClearingDate, FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import models.paymentAllocations.AllocationDetail
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testConstants.PaymentAllocationsTestConstants._
import testUtils.ViewSpec
import views.html.PaymentAllocation

import java.time.LocalDate
import scala.collection.JavaConverters._


class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val paymentAllocationView = app.injector.instanceOf[PaymentAllocation]

  lazy val backUrl: String = controllers.routes.PaymentHistoryController.show().url

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    btaNavPartial = None, Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  val singleTestPaymentAllocationChargeWithOutstandingAmountZero: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail.copy(outstandingAmount = Some(0))),
    List(financialDetail)
  )

  val singleTestPaymentAllocationChargeWithOutstandingAmountZeroCredit: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetailNoPaymentCredit.copy(outstandingAmount = Some(0))),
    List(financialDetailNoPaymentCredit)
  )

  val heading: String = messages("paymentAllocation.heading")
  val date: String = "31 January 2021"
  val amount: String = "£300.00"
  val paymentAllocationHeading: String = messages("paymentAllocation.tableSection.heading")
  val tableHeadings: Seq[String] = Seq(messages("paymentAllocation.tableHead.allocation"), messages("paymentAllocation.tableHead.allocated-date"), messages("paymentAllocation.tableHead.amount"))
  val moneyOnAccount: String = messages("paymentAllocation.moneyOnAccount")
  val moneyOnAccountAmount: String = "£200.00"
  val allocationsTableHeading: String = messages("paymentAllocation.tableSection.heading")
  val allocationsTableHeadersText: String = s"$paymentAllocationHeading ${tableHeadings.mkString(" ")}"
  val paymentAllocationsPoa1IncomeTax: String = messages("paymentAllocation.paymentAllocations.poa1.incomeTax")
  val paymentAllocationsPoa2IncomeTax: String = messages("paymentAllocation.paymentAllocations.poa2.incomeTax")
  val paymentAllocationsPoa1Nic4: String = messages("paymentAllocation.paymentAllocations.poa1.nic4")
  val paymentAllocationsPoa2Nic4: String = messages("paymentAllocation.paymentAllocations.poa2.nic4")
  val moneyOnAccountNA: String = s"${messages("paymentAllocation.moneyOnAccount")} ${messages("paymentAllocation.na")}"
  val moneyOnAccountMessage: String = s"${messages("paymentAllocation.moneyOnAccount")}"
  val dueDate = "31 Jan 2021"
  val paymentAllocationTaxYearFrom2017to2018: String = messages("paymentAllocation.taxYear", "2017", "2018")
  val paymentAllocationTaxYearFrom2018to2019: String = messages("paymentAllocation.taxYear", "2018", "2019")
  val paymentAllocationTaxYearFrom2019to2020: String = messages("paymentAllocation.taxYear", "2019", "2020")
  val paymentAllocationTaxYearFrom2021to2022: String = messages("paymentAllocation.taxYear", "2021", "2022")
  val paymentAllocationsHmrcAdjustment: String = messages("paymentAllocation.paymentAllocations.hmrcAdjustment.text")
  val paymentAllocationViewModelDueDate: Option[LocalDate] = paymentAllocationViewModel.paymentAllocationChargeModel
    .financialDetails.headOption.flatMap(_.items.flatMap(_.headOption.flatMap(_.dueDate)))
  val paymentAllocationViewModelOutstandingAmount: Option[BigDecimal] = paymentAllocationViewModel
    .paymentAllocationChargeModel.documentDetails.headOption.flatMap(_.outstandingAmount)
  val paymentAllocationViewModelWithCreditZeroOutstandingDueDate: Option[LocalDate] =
    paymentAllocationViewModelWithCreditZeroOutstanding.paymentAllocationChargeModel
      .financialDetails.headOption.flatMap(_.items.flatMap(_.headOption.flatMap(_.dueDate)))
  val paymentAllocationViewModelWithCreditZeroOutstandingOutstandingAmount: Option[BigDecimal] =
    paymentAllocationViewModelWithCreditZeroOutstanding
      .paymentAllocationChargeModel.documentDetails.headOption.flatMap(_.outstandingAmount)

  class PaymentAllocationSetup(viewModel: PaymentAllocationViewModel = paymentAllocationViewModel,
                               CutOverCreditsEnabled: Boolean = false, saUtr: Option[String] = None,
                               creditsRefundsRepayEnabled: Boolean = true,
                               dueDate: Option[LocalDate] = paymentAllocationViewModelDueDate,
                               outstandingAmount: Option[BigDecimal] = paymentAllocationViewModelOutstandingAmount) extends Setup(
    paymentAllocationView(viewModel, backUrl, saUtr = saUtr, CutOverCreditsEnabled = CutOverCreditsEnabled,
      creditsRefundsRepayEnabled = creditsRefundsRepayEnabled, dueDate = dueDate,
      outstandingAmount = outstandingAmount)) {
    paymentAllocationViewModel.originalPaymentAllocationWithClearingDate(0).allocationDetail.get.chargeType.get
  }

  class PaymentAllocationSetupCreditZeroOutstanding(viewModel: PaymentAllocationViewModel = paymentAllocationViewModelWithCreditZeroOutstanding,
                                                    dueDate: Option[LocalDate] =
                                                    paymentAllocationViewModelWithCreditZeroOutstandingDueDate,
                                                    outstandingAmount: Option[BigDecimal] =
                                                    paymentAllocationViewModelWithCreditZeroOutstandingOutstandingAmount) extends Setup(
    paymentAllocationView(viewModel, backUrl, saUtr = Some("1234567890"), CutOverCreditsEnabled = true,
      dueDate = dueDate,
      outstandingAmount = outstandingAmount)) {
    paymentAllocationViewModelWithCreditZeroOutstanding.originalPaymentAllocationWithClearingDate(0).allocationDetail.get.chargeType.get
  }


  "Payment Allocation Page for non LPI" should {
    "check that the first section information is present" when {
      "checking the heading" in new PaymentAllocationSetup() {
        document.getElementsByTag("h1").text shouldBe heading
      }

      s"have the correct date of $date" in new PaymentAllocationSetup {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").eq(0).text shouldBe date
      }

      s"have the correct Amount of $amount" in new PaymentAllocationSetup {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").last.text shouldBe amount
      }

      "checking there is the info text" in new PaymentAllocationSetup() {
        document.getElementsByClass("govuk-inset-text").text shouldBe messages("paymentAllocation.info")
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in new PaymentAllocationSetup() {
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationHeading
      }

      "check that the heading section is not present when credit is defined but outstandingAmount is 0" in
        new PaymentAllocationSetupCreditZeroOutstanding() {
          document.getElementsByClass("govuk-heading-m").eq(0).text() shouldBe ""
        }

      "check that the allocation table section is not present when credit is defined but outstandingAmount is 0" in
        new PaymentAllocationSetupCreditZeroOutstanding() {
          document.getElementsByClass("govuk-table").eq(0).text() shouldBe ""
        }

      "has table headers" in new PaymentAllocationSetup() {
        val allTableHeadings = document.selectHead("thead")
        allTableHeadings.selectNth("th", 1).text() shouldBe tableHeadings(0)
        allTableHeadings.selectNth("th", 2).text() shouldBe tableHeadings(1)
        allTableHeadings.selectNth("th", 3).text() shouldBe tableHeadings(2)
      }

      "has a payment within the table" in new PaymentAllocationSetup() {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        allTableData.selectNth("td", 1).text() shouldBe s"${messages("paymentAllocation.paymentAllocations.poa1.nic4")} 2020 ${messages("paymentAllocation.taxYear", "2019", "2020")}"
        allTableData.selectNth("td", 2).text() shouldBe "31 Jan 2021"
        allTableData.selectNth("td", 3).text() shouldBe "£10.10"
      }

      "has a Credit on account link row within payment details when refunds page FS enabled" in new PaymentAllocationSetup() {
        val allTableData = document.getElementById("money-on-account").getElementsByTag("td")
        document.select("a#money-on-account-link").size() shouldBe 1
        allTableData.get(0).text() shouldBe moneyOnAccount
        allTableData.get(2).text() shouldBe moneyOnAccountAmount
      }

      "has a Credit on account text row within payment details when refunds page FS disabled" in
        new PaymentAllocationSetup(creditsRefundsRepayEnabled = false) {
          val allTableData = document.getElementById("money-on-account").getElementsByTag("td")
          document.select("a#money-on-account-link").size() shouldBe 0
          allTableData.get(0).text() shouldBe moneyOnAccount
          allTableData.get(2).text() shouldBe moneyOnAccountAmount
        }

      "should not have Credit on account row within payment details" in new PaymentAllocationSetup(outstandingAmount =
        paymentAllocationViewModelWithCreditZeroOutstandingOutstandingAmount) {
        document.getElementById("money-on-account") shouldBe null
      }

      "checking the earlier tax year page when the cutOverCredit FS enabled with no payment items" in
        new PaymentAllocationSetup(paymentAllocationViewModelNoPayment, true, saUtr = Some("1234567890")) {
          document.getElementsByTag("h1").text shouldBe messages("paymentAllocation.earlyTaxYear.heading")
          document.getElementById("sa-note-migrated").text shouldBe s"${messages("paymentAllocation.sa.info")} ${messages("taxYears.oldSa.content.link")}${messages("pagehelp.opensInNewTabText")}."
          val moneyOnAccountData: Elements = document.getElementById("money-on-account").getElementsByTag("td")
          moneyOnAccountData.get(0).text() shouldBe moneyOnAccount
          moneyOnAccountData.get(1).text() shouldBe "31 Jan 2021"
          moneyOnAccountData.get(2).text() shouldBe moneyOnAccountAmount
        }

      "has a payment within the table for HMRC Adjustments with link back to charge view" in new PaymentAllocationSetup(viewModel = paymentAllocationViewModelHmrcAdjustment) {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        val chargePageLink = document.selectHead("tbody").link.attr("href")
        val taxYear = "2022"
        val chargePageLinkTrue = s"/report-quarterly/income-and-expenses/view/tax-years/$taxYear/charge?id=chargeReference3"

        allTableData.selectNth("td", 1).text() shouldBe s"$paymentAllocationsHmrcAdjustment $taxYear $paymentAllocationTaxYearFrom2021to2022"
        allTableData.selectNth("td", 2).text() shouldBe "31 Jan 2021"
        allTableData.selectNth("td", 3).text() shouldBe amount
        chargePageLink shouldBe chargePageLinkTrue
      }

      "has a payment within the table right aligned" in new PaymentAllocationSetup() {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        allTableData.selectNth("td", 3).hasClass("govuk-table__cell--numeric")
      }

      "has a payment headers right aligned" in new PaymentAllocationSetup() {
        val allTableData = document.selectHead("thead").selectHead("tr")
        allTableData.selectNth("th", 3).hasClass("govuk-table__header--numeric")
      }

    }
  }
  "Payment Allocation Page for LPI" should {
    "check that the first section information is present" when {
      "checking the heading" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.getElementsByTag("h1").text shouldBe heading
      }
      s"have the correct date of $date" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").eq(0).text shouldBe date
      }

      s"have the correct Amount of $amount" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.selectById("payment-allocation-charge-table")
          .getElementsByTag("dd").last.text shouldBe amount
      }

      "checking there is the info text" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.getElementsByClass("govuk-inset-text").text shouldBe messages("paymentAllocation.info")
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationHeading
      }

      "has table headers" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        val allTableHeadings = document.selectHead("thead")
        allTableHeadings.selectNth("th", 1).text() shouldBe tableHeadings(0)
        allTableHeadings.selectNth("th", 2).text() shouldBe tableHeadings(1)
        allTableHeadings.selectNth("th", 3).text() shouldBe tableHeadings(2)

      }

      "has a payment within the table" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        allTableData.selectNth("td", 1).text() shouldBe s"${messages("paymentAllocation.paymentAllocations.balancingCharge.text")} 2020 ${messages("paymentAllocation.taxYear", "2019", "2020")}"
        allTableData.selectNth("td", 2).text() shouldBe messages("paymentAllocation.na")
        allTableData.selectNth("td", 3).text() shouldBe "£300.00"

      }

      "has a payment within the table right aligned" in new PaymentAllocationSetup(paymentAllocationViewModelLpi) {
        val allTableData = document.selectHead("tbody").selectHead("tr")
        allTableData.selectNth("td", 3).hasClass("govuk-table__cell--numeric")
      }
    }
  }

  "Payment Allocation Page" should {
    s"have the title: ${messages("htmlTitle", messages("paymentAllocation.heading"))}" in new PaymentAllocationSetup {
      document.title() shouldBe messages("htmlTitle", messages("paymentAllocation.heading"))
    }

    s"have the heading: $heading" in new PaymentAllocationSetup {
      document.getElementsByTag("h1").text shouldBe heading
    }

    "have a fallback link" in new PaymentAllocationSetup {
      document.hasFallbackBacklinkTo(controllers.routes.PaymentHistoryController.show().url)
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
        allocationDetail("poa1_1", "2018-03-15", POA1, ITSA_ENGLAND_AND_NI, 1234.56) -> "2019-06-27",
        allocationDetail("poa1_2", "2018-04-05", POA1, ITSA_NI, 2345.67) -> "2019-06-28",
        allocationDetail("poa1_3", "2018-04-06", POA1, ITSA_SCOTLAND, 3456.78) -> "2019-06-29",
        allocationDetail("poa1_4", "2018-06-23", POA1, ITSA_WALES, 4567.89) -> "2019-06-30",
        allocationDetail("poa1_5", "2018-12-31", POA1, NIC4_GB, 9876.54) -> "2019-08-27",
        allocationDetail("poa1_6", "2019-01-01", POA1, NIC4_SCOTLAND, 8765.43) -> "2019-08-28",
        allocationDetail("poa1_7", "2019-04-05", POA1, NIC4_WALES, 7654.32) -> "2019-08-29",
        allocationDetail("poa1_8", "2019-04-06", POA1, NIC4_NI, 6543.21) -> "2019-08-30")) {

        val expectedLinkUrls = Seq(
          controllers.routes.ChargeSummaryController.show(2018, "poa1_1").url,
          controllers.routes.ChargeSummaryController.show(2018, "poa1_2").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_3").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_4").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_5").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_6").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa1_7").url,
          controllers.routes.ChargeSummaryController.show(2020, "poa1_8").url,
          controllers.routes.CreditAndRefundController.show().url
        )

        layoutContent.h2.text() shouldBe paymentAllocationHeading
        layoutContent.selectById("payment-allocation-table").text() shouldBe
          s"""
             |$allocationsTableHeadersText
             |$paymentAllocationsPoa1IncomeTax 2018 $paymentAllocationTaxYearFrom2017to2018 27 Jun 2019 £1,234.56
             |$paymentAllocationsPoa1IncomeTax 2018 $paymentAllocationTaxYearFrom2017to2018 28 Jun 2019 £2,345.67
             |$paymentAllocationsPoa1IncomeTax 2019 $paymentAllocationTaxYearFrom2018to2019 29 Jun 2019 £3,456.78
             |$paymentAllocationsPoa1IncomeTax 2019 $paymentAllocationTaxYearFrom2018to2019 30 Jun 2019 £4,567.89
             |$paymentAllocationsPoa1Nic4 2019 $paymentAllocationTaxYearFrom2018to2019 27 Aug 2019 £9,876.54
             |$paymentAllocationsPoa1Nic4 2019 $paymentAllocationTaxYearFrom2018to2019 28 Aug 2019 £8,765.43
             |$paymentAllocationsPoa1Nic4 2019 $paymentAllocationTaxYearFrom2018to2019 29 Aug 2019 £7,654.32
             |$paymentAllocationsPoa1Nic4 2020 $paymentAllocationTaxYearFrom2019to2020 30 Aug 2019 £6,543.21
             |$moneyOnAccountMessage $dueDate £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        layoutContent.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }

      "a payment on account 2 of 2" in new PaymentAllocationSetup(viewModel(
        allocationDetail("poa2_1", "2018-03-15", POA2, ITSA_ENGLAND_AND_NI, 1234.56) -> "2019-06-27",
        allocationDetail("poa2_2", "2018-04-05", POA2, ITSA_NI, 2345.67) -> "2019-06-28",
        allocationDetail("poa2_3", "2018-04-06", POA2, ITSA_SCOTLAND, 3456.78) -> "2019-06-29",
        allocationDetail("poa2_4", "2018-06-23", POA2, ITSA_WALES, 4567.89) -> "2019-06-30",
        allocationDetail("poa2_5", "2018-12-31", POA2, NIC4_GB, 9876.54) -> "2019-08-27",
        allocationDetail("poa2_6", "2019-01-01", POA2, NIC4_SCOTLAND, 8765.43) -> "2019-08-28",
        allocationDetail("poa2_7", "2019-04-05", POA2, NIC4_WALES, 7654.32) -> "2019-08-29",
        allocationDetail("poa2_8", "2019-04-06", POA2, NIC4_NI, 6543.21) -> "2019-08-30")) {

        val expectedLinkUrls = Seq(
          controllers.routes.ChargeSummaryController.show(2018, "poa2_1").url,
          controllers.routes.ChargeSummaryController.show(2018, "poa2_2").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_3").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_4").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_5").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_6").url,
          controllers.routes.ChargeSummaryController.show(2019, "poa2_7").url,
          controllers.routes.ChargeSummaryController.show(2020, "poa2_8").url,
          controllers.routes.CreditAndRefundController.show().url
        )

        layoutContent.h2.text() shouldBe paymentAllocationHeading
        layoutContent.selectById("payment-allocation-table").text() shouldBe
          s"""
             |$allocationsTableHeadersText
             |$paymentAllocationsPoa2IncomeTax 2018 $paymentAllocationTaxYearFrom2017to2018 27 Jun 2019 £1,234.56
             |$paymentAllocationsPoa2IncomeTax 2018 $paymentAllocationTaxYearFrom2017to2018 28 Jun 2019 £2,345.67
             |$paymentAllocationsPoa2IncomeTax 2019 $paymentAllocationTaxYearFrom2018to2019 29 Jun 2019 £3,456.78
             |$paymentAllocationsPoa2IncomeTax 2019 $paymentAllocationTaxYearFrom2018to2019 30 Jun 2019 £4,567.89
             |$paymentAllocationsPoa2Nic4 2019 $paymentAllocationTaxYearFrom2018to2019 27 Aug 2019 £9,876.54
             |$paymentAllocationsPoa2Nic4 2019 $paymentAllocationTaxYearFrom2018to2019 28 Aug 2019 £8,765.43
             |$paymentAllocationsPoa2Nic4 2019 $paymentAllocationTaxYearFrom2018to2019 29 Aug 2019 £7,654.32
             |$paymentAllocationsPoa2Nic4 2020 $paymentAllocationTaxYearFrom2019to2020 30 Aug 2019 £6,543.21
             |$moneyOnAccountMessage $dueDate £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        layoutContent.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }

      "a balancing charge" in new PaymentAllocationSetup(viewModel(
        allocationDetail("bcd_1", "2018-03-15", BAL_CHARGE, ITSA_SCOTLAND, 1234.56) -> "2019-06-27",
        allocationDetail("bcd_2", "2018-04-05", BAL_CHARGE, NIC4_WALES, 2345.67) -> "2019-06-28",
        allocationDetail("bcd_3", "2018-04-06", BAL_CHARGE, NIC2_GB, 3456.78) -> "2019-06-29",
        allocationDetail("bcd_4", "2019-01-01", BAL_CHARGE, CGT, 9876.54) -> "2019-08-27",
        allocationDetail("bcd_5", "2019-04-05", BAL_CHARGE, "SL", 8765.43) -> "2019-08-28",
        allocationDetail("bcd_6", "2019-04-06", BAL_CHARGE, VOLUNTARY_NIC2_NI, 7654.32) -> "2019-08-29")) {

        val expectedLinkUrls = Seq(
          controllers.routes.ChargeSummaryController.show(2018, "bcd_1").url,
          controllers.routes.ChargeSummaryController.show(2018, "bcd_2").url,
          controllers.routes.ChargeSummaryController.show(2019, "bcd_3").url,
          controllers.routes.ChargeSummaryController.show(2019, "bcd_4").url,
          controllers.routes.ChargeSummaryController.show(2019, "bcd_5").url,
          controllers.routes.ChargeSummaryController.show(2020, "bcd_6").url,
          controllers.routes.CreditAndRefundController.show().url
        )

        layoutContent.h2.text() shouldBe paymentAllocationHeading
        layoutContent.selectById("payment-allocation-table").text() shouldBe
          s"""
             |$allocationsTableHeadersText
             |${messages("paymentAllocation.paymentAllocations.bcd.incomeTax")} 2018 $paymentAllocationTaxYearFrom2017to2018 27 Jun 2019 £1,234.56
             |${messages("paymentAllocation.paymentAllocations.bcd.nic4")} 2018 $paymentAllocationTaxYearFrom2017to2018 28 Jun 2019 £2,345.67
             |${messages("paymentAllocation.paymentAllocations.bcd.nic2")} 2019 $paymentAllocationTaxYearFrom2018to2019 29 Jun 2019 £3,456.78
             |${messages("paymentAllocation.paymentAllocations.bcd.cgt")} 2019 $paymentAllocationTaxYearFrom2018to2019 27 Aug 2019 £9,876.54
             |${messages("paymentAllocation.paymentAllocations.bcd.sl")} 2019 $paymentAllocationTaxYearFrom2018to2019 28 Aug 2019 £8,765.43
             |${messages("paymentAllocation.paymentAllocations.bcd.vcnic2")} 2020 $paymentAllocationTaxYearFrom2019to2020 29 Aug 2019 £7,654.32
             |$moneyOnAccountMessage $dueDate £200.00
             |""".stripMargin.trim.linesIterator.mkString(" ")

        layoutContent.selectById("payment-allocation-table").select(Selectors.tableRow).select(Selectors.link)
          .eachAttr("href").asScala shouldBe expectedLinkUrls
      }
    }

    "have a Credit on account row within payment details" in new PaymentAllocationSetup() {
      val allTableData: Elements = document.getElementById("money-on-account").getElementsByTag("td")
      allTableData.get(0).text() shouldBe moneyOnAccount
      allTableData.get(2).text() shouldBe moneyOnAccountAmount

    }

    "not have Credit on account row within payment details" in new PaymentAllocationSetup(outstandingAmount =
      paymentAllocationViewModelWithCreditZeroOutstandingOutstandingAmount) {
      document.getElementById("money-on-account") shouldBe null
    }

    "The payments allocation view has NO payment allocation amount" should {
      "throw a MissingFieldException" in {
        val thrownException = intercept[MissingFieldException] {
          paymentAllocationView(paymentAllocationViewModelWithNoOriginalAmount, backUrl, saUtr = None, CutOverCreditsEnabled = false, dueDate = None,
            outstandingAmount = None)
        }
        thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Payment Allocation Amount"
      }
    }

    "The payments allocation view has Allocation Detail but no clearing date" should {
      "throw a MissingFieldException" in {
        val thrownException = intercept[MissingFieldException] {
          paymentAllocationView(paymentAllocationViewModelWithNoClearingAmount, backUrl, saUtr = None, CutOverCreditsEnabled = false, dueDate = None,
            outstandingAmount = None)
        }
        thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Payment Clearing Date"
      }
    }
  }
}
