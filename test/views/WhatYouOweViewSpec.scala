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
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.outstandingCharges._
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testRetrievedUserName, testSaUtr, testUserTypeAgent, testUserTypeIndividual}
import testConstants.FinancialDetailsTestConstants._
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.retrieve.Name
import views.html.WhatYouOwe

import java.time.LocalDate

class WhatYouOweViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]

  val whatYouOweTitle: String = messages("titlePattern.serviceName.govUk", messages("whatYouOwe.heading"))
  val whatYouOweHeading: String = messages("whatYouOwe.heading")
  val noPaymentsDue: String = messages("whatYouOwe.no-payments-due")
  val saLink: String = s"${messages("whatYouOwe.sa-link")}${messages("pagehelp.opensInNewTabText")}"
  val saNote: String = s"${messages("whatYouOwe.sa-note")} $saLink."
  val osChargesNote: String = messages("whatYouOwe.outstanding-charges-note")
  val dropDownInfo: String = messages("whatYouOwe.dropdown.info")
  val paymentUnderReviewPara: String = s"${messages("whatYouOwe.dunningLock.text", s"${messages("whatYouOwe.dunningLock.link")}${messages("pagehelp.opensInNewTabText")}")}."
  val paymentType: String = messages("tax-year-summary.payments.payment-type")
  val taxYearSummary: String = messages("whatYouOwe.tableHead.tax-year-summary")
  val amountDue: String = messages("whatYouOwe.tableHead.amount-due")
  val paymentProcessingBullet1: String = s"${messages("whatYouOwe.payments-made-bullet-1.1")} ${messages("whatYouOwe.payments-made-bullet-1.2")}${messages("pagehelp.opensInNewTabText")} ${messages("whatYouOwe.payments-made-bullet-1.3")}"
  val paymentProcessingBullet2: String = messages("whatYouOwe.payments-made-bullet-2")
  val paymentsMade: String = messages("whatYouOwe.payments-made")
  val poa1Text: String = messages("whatYouOwe.paymentOnAccount1.text")
  val latePoa1Text: String = messages("whatYouOwe.lpi.paymentOnAccount1.text")
  val poa2Text: String = messages("whatYouOwe.paymentOnAccount2.text")
  val remainingBalance: String = messages("whatYouOwe.balancingCharge.text")
  val preMTDRemainingBalance: String = s"${messages("whatYouOwe.balancingCharge.text")} ${messages("whatYouOwe.pre-mtd-digital")}"
  val remainingBalanceLine1: String = messages("whatYouOwe.remaining-balance.line1")
  val paymentUnderReview: String = messages("whatYouOwe.paymentUnderReview")
  val poaHeading: String = messages("whatYouOwe.payment-on-account.heading")
  val poaLine1: String = messages("whatYouOwe.payment-on-account.line1")
  val lpiHeading: String = messages("whatYouOwe.late-payment-interest.heading")
  val lpiLine1: String = messages("whatYouOwe.late-payment-interest.line1")
  val overdueTag: String = messages("whatYouOwe.over-due")
  val poa1WithTaxYearAndUnderReview: String = s"$poa1Text $currentYear $paymentUnderReview"
  val poa1WithTaxYearOverdueAndUnderReview: String = s"$overdueTag $poa1Text $currentYear $paymentUnderReview"
  val dueDate: String = messages("whatYouOwe.tableHead.due-date")
  val payNow: String = messages("whatYouOwe.payNow")
  val saPaymentOnAccount1: String = "SA Payment on Account 1"
  val saPaymentOnAccount2: String = "SA Payment on Account 2"
  val hmrcAdjustment: String = messages("whatYouOwe.hmrcAdjustment.text")
  val hmrcAdjustmentHeading: String = messages("whatYouOwe.hmrcAdjustment.heading")
  val hmrcAdjustmentLine1: String = messages("whatYouOwe.hmrcAdjustment.line1")
  val itsaPOA1: String = "ITSA- POA 1"
  val itsaPOA2: String = "ITSA - POA 2"
  val cancelledPayeSelfAssessment: String = "Cancelled Self Assessment payment (through your PAYE tax code)"

  def interestFromToDate(from: String, to: String, rate: String) =
    s"${messages("whatYouOwe.over-due.interest.line1")} ${messages("whatYouOwe.over-due.interest.line2", from, to, rate)}"

  def taxYearSummaryText(from: String, to: String): String = s"${messages("whatYouOwe.tax-year-summary.taxYear", from, to)} ${messages("whatYouOwe.taxYear")}"

  def preMtdPayments(from: String, to: String) = s"${messages("whatYouOwe.pre-mtd-year", from, to)}"

  class Setup(creditCharges: List[DocumentDetail] = List(),
              charges: WhatYouOweChargesList,
              currentTaxYear: Int = LocalDate.now().getYear,
              hasLpiWithDunningBlock: Boolean = false,
              dunningLock: Boolean = false,
              whatYouOweCreditAmountEnabled: Boolean = false,
              migrationYear: Int = LocalDate.now().getYear - 1,
              codingOutEnabled: Boolean = true,
              mfaCreditsAndDebitsEnabled: Boolean = true
             ) {
    val individualUser: MtdItUser[_] = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(testRetrievedUserName),
      incomeSources = IncomeSourceDetailsModel("testMtdItId", Some(migrationYear.toString), List(), None),
      btaNavPartial = None,
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = Some(testUserTypeIndividual),
      arn = None
    )(FakeRequest())

    val html: HtmlFormat.Appendable = whatYouOweView(creditCharges, charges, hasLpiWithDunningBlock, currentTaxYear, "testBackURL",
      Some("1234567890"), None, dunningLock, codingOutEnabled, mfaCreditsAndDebitsEnabled, whatYouOweCreditAmountEnabled)(FakeRequest(), individualUser, implicitly)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def verifySelfAssessmentLink(): Unit = {
      val anchor: Element = pageDocument.getElementById("payments-due-note").selectFirst("a")
      anchor.text shouldBe saLink
      anchor.attr("href") shouldBe "http://localhost:8930/self-assessment/ind/1234567890/account"
      anchor.attr("target") shouldBe "_blank"
    }
  }

  class AgentSetup(creditCharges: List[DocumentDetail] = List(),
                   charges: WhatYouOweChargesList,
                   currentTaxYear: Int = LocalDate.now().getYear,
                   migrationYear: Int = LocalDate.now().getYear - 1,
                   codingOutEnabled: Boolean = true,
                   mfaCreditsAndDebitsEnabled: Boolean = true,
                   whatYouOweCreditAmountEnabled: Boolean = false,
                   dunningLock: Boolean = false,
                   hasLpiWithDunningBlock: Boolean = false) {

    val agentUser: MtdItUser[_] = MtdItUser(
      mtditid = "XAIT00000000015",
      nino = "AA111111A",
      userName = Some(Name(Some("Test"), Some("User"))),
      incomeSources = IncomeSourceDetailsModel("testMtdItId", Some(migrationYear.toString), List(), None),
      btaNavPartial = None,
      saUtr = Some("1234567890"),
      credId = Some(testCredId),
      userType = Some(testUserTypeAgent),
      arn = Some(testArn)
    )(FakeRequest())

    val whatYouOweView: WhatYouOwe = app.injector.instanceOf[WhatYouOwe]

    val html: HtmlFormat.Appendable = whatYouOweView(
      creditCharges = creditCharges,
      whatYouOweChargesList = charges,
      hasLpiWithDunningBlock = hasLpiWithDunningBlock,
      currentTaxYear = currentTaxYear,
      backUrl = "testBackURL",
      utr = Some("1234567890"),
      dunningLock = dunningLock,
      codingOutEnabled = codingOutEnabled,
      mfaCreditsAndDebitsEnabled = mfaCreditsAndDebitsEnabled,
      whatYouOweCreditAmountEnabled = whatYouOweCreditAmountEnabled,
      isAgent = true)(FakeRequest(), agentUser, implicitly)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  def financialDetailsOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): FinancialDetailsModel = testFinancialDetailsModelWithInterest(
    documentDescription = List(Some(itsaPOA1), Some(itsaPOA2)),
    mainType = List(Some(saPaymentOnAccount1), Some(saPaymentOnAccount2)),
    dueDate = dueDateOverdue,
    dunningLock = noDunningLocks,
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestOutstandingAmount = List(Some(42.50), Some(24.05)),
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
  )

  def financialDetailsOverdueWithLpi(latePaymentInterest: List[Option[BigDecimal]], dunningLock: List[Option[String]]): FinancialDetailsModel = testFinancialDetailsModelWithLPI(
    documentDescription = List(Some(itsaPOA1), Some(itsaPOA2)),
    mainType = List(Some(saPaymentOnAccount1), Some(saPaymentOnAccount2)),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    dunningLock = dunningLock,
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest
  )

  def financialDetailsOverdueWithLpiDunningBlock(latePaymentInterest: Option[BigDecimal], lpiWithDunningBlock: Option[BigDecimal]): FinancialDetailsModel = testFinancialDetailsModelWithLPIDunningLock(
    documentDescription = List(Some(itsaPOA1), Some(itsaPOA2)),
    mainType = List(Some(saPaymentOnAccount1), Some(saPaymentOnAccount2)),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest,
    lpiWithDunningBlock = lpiWithDunningBlock
  )

  def financialDetailsOverdueWithLpiDunningBlockZero(latePaymentInterest: Option[BigDecimal], lpiWithDunningBlock: Option[BigDecimal]): FinancialDetailsModel = testFinancialDetailsModelWithLpiDunningLockZero(
    documentDescription = List(Some(itsaPOA1), Some(itsaPOA2)),
    mainType = List(Some(saPaymentOnAccount1), Some(saPaymentOnAccount2)),
    dueDate = List(Some(LocalDate.now().minusDays(10).toString), Some(LocalDate.now().minusDays(1).toString)),
    outstandingAmount = List(Some(50), Some(75)),
    taxYear = LocalDate.now().getYear.toString,
    interestRate = List(Some(2.6), Some(6.2)),
    latePaymentInterestAmount = latePaymentInterest,
    lpiWithDunningBlock = lpiWithDunningBlock
  )

  def whatYouOweDataWithOverdueInterestData(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = financialDetailsOverdueInterestData(latePaymentInterest).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPI(latePaymentInterest: List[Option[BigDecimal]],
                                   dunningLock: List[Option[String]] = noDunningLocks): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpi(latePaymentInterest, dunningLock).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPIDunningBlock(latePaymentInterest: Option[BigDecimal],
                                               lpiWithDunningBlock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpiDunningBlock(latePaymentInterest, lpiWithDunningBlock).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueLPIDunningBlockZero(latePaymentInterest: Option[BigDecimal],
                                                   lpiWithDunningBlock: Option[BigDecimal]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = financialDetailsOverdueWithLpiDunningBlockZero(latePaymentInterest, lpiWithDunningBlock).getAllDocumentDetailsWithDueDates(),
    outstandingChargesModel = Some(outstandingChargesOverdueData)
  )

  def whatYouOweDataWithOverdueMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates()(1))
      ++ List(financialDetailsWithMixedData3.getAllDocumentDetailsWithDueDates().head),

  )

  def whatYouOweDataTestActiveWithMixedData2(latePaymentInterest: List[Option[BigDecimal]]): WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(financialDetailsOverdueWithLpi(latePaymentInterest, noDunningLocks).getAllDocumentDetailsWithDueDates()(1))
      ++ List(financialDetailsWithMixedData3.getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val codingOutAmount = 444.23
  val codingOutHeader: String = messages("whatYouOwe.codingOutHeading")
  val codingOutNotice = s"${messages("whatYouOwe.codingOut-1a")} £444.23 ${messages("whatYouOwe.codingOut-1b")} ${messages("whatYouOwe.codingOut-2", "2020", "2021")} ${messages("whatYouOwe.codingOut-3")}"

  val codedOutDocumentDetail: DocumentDetail = DocumentDetail(taxYear = "2021", transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some("Class 2 National Insurance"), outstandingAmount = Some(12.34),
    originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)

  val codedOutDocumentDetailPayeSA: DocumentDetail = DocumentDetail(taxYear = "2021", transactionId = "CODINGOUT02", documentDescription = Some("TRM New Charge"),
    documentText = Some("PAYE Self Assessment"), outstandingAmount = Some(0.00),
    originalAmount = Some(43.21), documentDate = LocalDate.of(2018, 3, 29),
    interestOutstandingAmount = None, interestRate = None,
    latePaymentInterestId = None, interestFromDate = Some(LocalDate.parse("2019-05-25")),
    interestEndDate = Some(LocalDate.parse("2019-06-25")), latePaymentInterestAmount = None)

  val outstandingChargesWithAciValueZeroAndOverdue: OutstandingChargesModel = outstandingChargesModel(LocalDate.now().minusDays(15).toString, 0.00)

  val whatYouOweDataWithWithAciValueZeroAndOverdue: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData3.getAllDocumentDetailsWithDueDates()(1))
      ++ List(financialDetailsWithMixedData3.getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithWithAciValueZeroAndFuturePayments: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates()(1))
      ++ List(financialDetailsWithMixedData1.getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = Some(outstandingChargesWithAciValueZeroAndOverdue)
  )

  val whatYouOweDataWithCodingOut: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(testFinancialDetailsModelWithCodingOut().getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(DocumentDetailWithCodingDetails(codedOutDocumentDetail,
      CodingDetails(taxYearReturn = "2021", amountCodedOut = codingOutAmount, taxYearCoding = "2020")))
  )

  val whatYouOweDataWithMFADebits: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(financialDetailsMFADebits.getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = None,
    codedOutDocumentDetail = None
  )

  val whatYouOweDataWithCodingOutFuture: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(testFinancialDetailsModelWithCodingOut().getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = None,
    codedOutDocumentDetail = Some(DocumentDetailWithCodingDetails(codedOutDocumentDetail,
      CodingDetails(taxYearReturn = "2021", amountCodedOut = codingOutAmount, taxYearCoding = "2020")))
  )

  val whatYouOweDataCodingOutWithoutAmountCodingOut: WhatYouOweChargesList = whatYouOweDataWithCodingOut.copy(codedOutDocumentDetail = None)

  val whatYouOweDataWithCancelledPayeSa: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
    chargesList = List(testFinancialDetailsModelWithCancelledPayeSa().getAllDocumentDetailsWithDueDates().head),
    outstandingChargesModel = None,
    codedOutDocumentDetail = None
  )

  val noChargesModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None))

  val whatYouOweDataWithPayeSA: WhatYouOweChargesList = WhatYouOweChargesList(
    balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None),
    chargesList = List(),
    codedOutDocumentDetail = Some(DocumentDetailWithCodingDetails(codedOutDocumentDetailPayeSA,
      CodingDetails(taxYearReturn = "2021", amountCodedOut = codingOutAmount, taxYearCoding = "2020")))
  )

  val noUtrModel: WhatYouOweChargesList = WhatYouOweChargesList(balanceDetails = BalanceDetails(0.00, 0.00, 0.00, None, None, None, None))

  "individual" when {
    "The What you owe view with financial details model" when {
      "the user has charges and access viewer before 30 days of due date" should {

        "have the Balancing Payment title " in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
          val remainingBalanceHeader: Element = pageDocument.select("tr").first()
          remainingBalanceHeader.select("th").first().text() shouldBe dueDate
          remainingBalanceHeader.select("th").get(1).text() shouldBe paymentType
          remainingBalanceHeader.select("th").get(2).text() shouldBe taxYearSummary
          remainingBalanceHeader.select("th").last().text() shouldBe amountDue
        }
        "Balancing Payment row data exists and should not contain hyperlink and overdue tag " in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {

          val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
          remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(35).toLongDateShort
          remainingBalanceTable.select("td").get(1).text() shouldBe preMTDRemainingBalance
          remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
            (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)

          remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

          pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
        }
        "have POA data in same table" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {

          val poa1Table: Element = pageDocument.select("tr").get(2)
          poa1Table.select("td").first().text() shouldBe LocalDate.now().plusDays(45).toLongDateShort
          poa1Table.select("td").get(1).text() shouldBe poa1Text + s" $currentYear"
          poa1Table.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)

          poa1Table.select("td").last().text() shouldBe "£50.00"

          val poa2Table: Element = pageDocument.select("tr").get(3)
          poa2Table.select("td").first().text() shouldBe LocalDate.now().plusDays(50).toLongDateShort
          poa2Table.select("td").get(1).text() shouldBe poa2Text + s" $currentYear"
          poa2Table.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)

          poa2Table.select("td").last().text() shouldBe "£75.00"

        }
        "payment type drop down and content exists" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
          pageDocument.select(".govuk-details__summary-text").text shouldBe dropDownInfo
          pageDocument.getElementById("payment-details-content-0").text shouldBe s"$remainingBalance $remainingBalanceLine1"
          pageDocument.getElementById("payment-details-content-1").text shouldBe s"$poaHeading $poaLine1"
          pageDocument.getElementById("payment-details-content-2").text shouldBe s"$lpiHeading $lpiLine1"

        }
        "should have payment processing bullets when payment due in more than 30 days" in new Setup(charges = whatYouOweDataWithDataDueInMoreThan30Days()) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote


        }

        "display the paragraph about payments under review and bullet points when there is a dunningLock" in new Setup(
          charges = whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

        "display bullets and not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          charges = whatYouOweDataWithDataDueInMoreThan30Days(twoDunningLocks)) {
          pageDocument.getElementById("payment-under-review-info") shouldBe null

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

      }

      "the user has charges and access viewer within 30 days of due date" should {
        s"have the Balancing Payment header and table data" in new Setup(charges = whatYouOweDataWithDataDueIn30Days()) {
          val remainingBalanceHeader: Element = pageDocument.select("tr").first()
          remainingBalanceHeader.select("th").first().text() shouldBe dueDate
          remainingBalanceHeader.select("th").get(1).text() shouldBe paymentType
          remainingBalanceHeader.select("th").get(2).text() shouldBe taxYearSummary
          remainingBalanceHeader.select("th").last().text() shouldBe amountDue

          val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
          remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
          remainingBalanceTable.select("td").get(1).text() shouldBe preMTDRemainingBalance
          remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
            (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
          remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        }
        "have POA data in same table as balancing payment " in new Setup(charges = whatYouOweDataWithDataDueIn30Days()) {

          val poa1Table: Element = pageDocument.select("tr").get(2)
          poa1Table.select("td").first().text() shouldBe LocalDate.now().toLongDateShort
          poa1Table.select("td").get(1).text() shouldBe poa1Text + s" $currentYear"
          poa1Table.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)

          poa1Table.select("td").last().text() shouldBe "£50.00"

          val poa2Table: Element = pageDocument.select("tr").get(3)
          poa2Table.select("td").first().text() shouldBe LocalDate.now().plusDays(1).toLongDateShort
          poa2Table.select("td").get(1).text() shouldBe poa2Text + s" $currentYear"
          poa2Table.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)

          poa2Table.select("td").last().text() shouldBe "£75.00"

        }
        "have payment type drop down details" in new Setup(charges = whatYouOweDataWithDataDueIn30Days()) {
          pageDocument.select(".govuk-details__summary-text").text shouldBe dropDownInfo
          pageDocument.getElementById("payment-details-content-0").text shouldBe s"$remainingBalance $remainingBalanceLine1"
          pageDocument.getElementById("payment-details-content-1").text shouldBe s"$poaHeading $poaLine1"
          pageDocument.getElementById("payment-details-content-2").text shouldBe s"$lpiHeading $lpiLine1"
          pageDocument.getElementById("payment-details-content-3").text shouldBe messages("whatYouOwe.class2-nic.heading") + " " + messages("whatYouOwe.class2-nic.line1")
          pageDocument.getElementById("payment-details-content-4").text shouldBe messages("whatYouOwe.cancelled-paye-sa.heading") + " " + messages("whatYouOwe.cancelled-paye-sa.line1")


          pageDocument.getElementById("balancing-charge-type-overdue") shouldBe null
        }

        "have Data for due within 30 days" in new Setup(charges = whatYouOweDataWithDataDueIn30Days()) {

          pageDocument.getElementById("due-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000124").url
          pageDocument.getElementById("due-0-overdue") shouldBe null
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            LocalDate.now().getYear).url
        }
        "have data with POA2 with hyperlink and no overdue" in new Setup(charges = whatYouOweDataWithDataDueIn30Days()) {

          pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000125").url
          pageDocument.getElementById("due-1-overdue") shouldBe null
        }

        "have payment details and should not contain future payments and overdue payment headers" in new Setup(charges =whatYouOweDataWithDataDueIn30Days()) {
          pageDocument.getElementById("payment-button").text shouldBe payNow

          pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(5000).url

        }

        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }
        "should have payment processing bullets when there is dunningLock" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks), dunningLock = true) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }
        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {
          pageDocument.getElementById("payment-under-review-info") shouldBe null
        }
        "should have payment processing bullets when there is no dunningLock" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }
        s"display $paymentUnderReview when there is a dunningLock against a single charge" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(oneDunningLock)) {
          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
          val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(3)

          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearAndUnderReview
          dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe s"$poa2Text $currentYear"
        }
        "should have payment processing bullets when there is a single charge" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(oneDunningLock)) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }


        s"display $paymentUnderReview when there is a dunningLock against multiple charges" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {
          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
          val dueWithInThirtyDaysTableRow2: Element = pageDocument.select("tr").get(3)

          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearAndUnderReview
          dueWithInThirtyDaysTableRow2.select("td").get(1).text() shouldBe s"$poa2Text $currentYear $paymentUnderReview"
        }
        "should have payment processing bullets when there is multiple charge" in new Setup(
          charges = whatYouOweDataWithDataDueIn30Days(twoDunningLocks)) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }
      }

      "the user has charges and access viewer after due date" should {

        "have the mtd payments header, table header and data with Balancing Payment data with no hyperlink but have overdue tag" in new Setup(
          charges = whatYouOweDataWithOverdueDataAndInterest()) {
          val remainingBalanceHeader: Element = pageDocument.select("tr").first()
          remainingBalanceHeader.select("th").first().text() shouldBe dueDate
          remainingBalanceHeader.select("th").get(1).text() shouldBe paymentType
          remainingBalanceHeader.select("th").get(2).text() shouldBe taxYearSummary
          remainingBalanceHeader.select("th").last().text() shouldBe amountDue

          val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
          remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(30).toLongDateShort
          remainingBalanceTable.select("td").get(1).text() shouldBe overdueTag + " " + preMTDRemainingBalance
          remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
            (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
          remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

          val interestTable: Element = pageDocument.select("tr").get(2)
          interestTable.select("td").first().text() shouldBe ""
          interestTable.select("td").get(1).text() shouldBe messages("whatYouOwe.balancingCharge.interest.line1.text") + " " +
            messages("whatYouOwe.balancingCharge.interest.line2.text",
                      LocalDate.now().minusDays(30).toLongDateShort, LocalDate.now().toLongDateShort)
          interestTable.select("td").get(2).text() shouldBe preMtdPayments(
            (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
          interestTable.select("td").last().text() shouldBe "£12.67"

          pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe overdueTag
        }
        "have payment type dropdown details and bullet point list" in new Setup(charges = whatYouOweDataWithOverdueDataAndInterest()) {
          pageDocument.select(".govuk-details__summary-text").text shouldBe dropDownInfo
          pageDocument.getElementById("payment-details-content-0").text shouldBe remainingBalance + " " + remainingBalanceLine1
          pageDocument.getElementById("payment-details-content-1").text shouldBe poaHeading + " " + poaLine1
          pageDocument.getElementById("payment-details-content-2").text shouldBe lpiHeading + " " + lpiLine1

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"

        }

        "have overdue payments header and data with POA1 charge type and show Late payment interest on payment on account 1 of 2" in
          new Setup(charges = whatYouOweDataWithOverdueLPI(List(Some(34.56), None))) {
            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe paymentType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe s"$overdueTag $latePoa1Text $currentYear"
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              LocalDate.now().getYear, "1040000124", latePaymentCharge = true).url
            pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              LocalDate.now().getYear).url
          }

        "should have payment processing bullets when there is POA1 charge and lpi on poa 1 of 2" in new Setup(charges = whatYouOweDataWithOverdueLPI(List(Some(34.56), None))) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        "have overdue payments header, bullet points and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - LPI Dunning Block" in
          new Setup(charges = whatYouOweDataWithOverdueLPIDunningBlock(Some(34.56), Some(1000))) {

            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe paymentType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
              latePoa1Text + s" $currentYear" + " " + paymentUnderReview
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              LocalDate.now().getYear, "1040000124", latePaymentCharge = true).url
            pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
            pageDocument.getElementById("LpiDunningBlock").text shouldBe "Payment under review"
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              LocalDate.now().getYear).url

            pageDocument.getElementById("payments-made").text shouldBe paymentsMade
            val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
            paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
            paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
            pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          }

        "have overdue payments header, bullet points and data with POA1 charge type and show Late payment interest on payment on account 1 of 2 - No LPI Dunning Block" in
          new Setup(charges = whatYouOweDataWithOverdueLPIDunningBlockZero(Some(34.56), Some(0))) {

            val overdueTableHeader: Element = pageDocument.select("tr").get(0)
            overdueTableHeader.select("th").first().text() shouldBe dueDate
            overdueTableHeader.select("th").get(1).text() shouldBe paymentType
            overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
            overdueTableHeader.select("th").last().text() shouldBe amountDue

            val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
            overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
              latePoa1Text + s" $currentYear"
            overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
            overduePaymentsTableRow1.select("td").last().text() shouldBe "£34.56"

            pageDocument.getElementById("due-0-late-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
              LocalDate.now().getYear, "1040000124", latePaymentCharge = true).url
            pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
            pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
              LocalDate.now().getYear).url

            pageDocument.getElementById("payments-made").text shouldBe paymentsMade
            val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
            paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
            paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
            pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"

          }

        "have overdue payments header, bullet points and data with POA1 charge type and No Late payment interest" in new Setup(charges = whatYouOweDataWithOverdueLPI(List(None, None))) {

          val overdueTableHeader: Element = pageDocument.select("tr").get(0)
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe paymentType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
            poa1Text + s" $currentYear"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000124").url
          pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            LocalDate.now().getYear).url

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

        "have overdue payments header, bullet points and data with POA1 charge type" in new Setup(charges = whatYouOweDataWithOverdueLPI(List(None, None))) {

          val overdueTableHeader: Element = pageDocument.select("tr").get(0)
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe paymentType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          /*
                overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(10).toLongDateShort
        */
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " +
            poa1Text + s" $currentYear"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000124").url
          pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            LocalDate.now().getYear).url

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }
        "have overdue payments with POA2 charge type with hyperlink and overdue tag" in new Setup(charges = whatYouOweDataWithOverdueLPI(List(None, None))) {
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)
          overduePaymentsTableRow2.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe overdueTag + " " + poa2Text + s" $currentYear"
          overduePaymentsTableRow2.select("td").last().text() shouldBe "£75.00"

          pageDocument.getElementById("due-1-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000125").url
          pageDocument.getElementById("due-1-overdue").text shouldBe overdueTag
        }

        "have accruing interest displayed below each overdue POA" in new Setup(charges = whatYouOweDataWithOverdueInterestData(List(None, None))) {
          def overduePaymentsInterestTableRow(index: String): Element = pageDocument.getElementById(s"charge-interest-$index")

          overduePaymentsInterestTableRow("0").select("td").get(1).text() shouldBe interestFromToDate("25 May 2019", "25 Jun 2019", "2.6")
          overduePaymentsInterestTableRow("0").select("td").last().text() shouldBe "£42.50"

          overduePaymentsInterestTableRow("1").select("td").get(1).text() shouldBe interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
          overduePaymentsInterestTableRow("1").select("td").last().text() shouldBe "£24.05"
        }
        "should have payment processing bullets when there is accruing interest" in new Setup(charges = whatYouOweDataWithOverdueInterestData(List(None, None))) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        "only show interest for POA when there is no late Payment Interest" in new Setup(charges = whatYouOweDataWithOverdueInterestData(List(Some(34.56), None))) {
          def overduePaymentsInterestTableRow(index: String): Element = pageDocument.getElementById(s"charge-interest-$index")

          overduePaymentsInterestTableRow("0") shouldBe null

          overduePaymentsInterestTableRow("1").select("td").get(1).text() shouldBe interestFromToDate("25 May 2019", "25 Jun 2019", "6.2")
          overduePaymentsInterestTableRow("1").select("td").last().text() shouldBe "£24.05"
        }

        "not have a paragraph explaining interest rates when there is no accruing interest" in new Setup(charges = whatYouOweDataWithOverdueData()) {
          pageDocument.select(".interest-rate").first() shouldBe null
        }

        "have payments data with button" in new Setup(charges = whatYouOweDataWithOverdueData()) {
          pageDocument.getElementById("payment-button").text shouldBe payNow

          pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

        }

        "display the paragraph about payments under review when there is a dunningLock" in new Setup(
          charges = whatYouOweDataWithOverdueData(twoDunningLocks), dunningLock = true) {
          val paymentUnderReviewParaLink: Element = pageDocument.getElementById("disagree-with-tax-appeal-link")

          pageDocument.getElementById("payment-under-review-info").text shouldBe paymentUnderReviewPara
          paymentUnderReviewParaLink.attr("href") shouldBe "https://www.gov.uk/tax-appeals"
          paymentUnderReviewParaLink.attr("target") shouldBe "_blank"
        }

        "not display the paragraph about payments under review when there are no dunningLock" in new Setup(
          charges = whatYouOweDataWithOverdueData(twoDunningLocks)) {
          pageDocument.getElementById("payment-under-review-info") shouldBe null
        }

        s"display $paymentUnderReview when there is a dunningLock against a single charge" in new Setup(
          charges = whatYouOweDataWithOverdueLPI(List(None, None), oneDunningLock)) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe s"$overdueTag $poa2Text $currentYear"
        }

        s"display $paymentUnderReview when there is a dunningLock against multiple charges" in new Setup(
          charges = whatYouOweDataWithOverdueLPI(List(None, None), twoDunningLocks)) {
          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(3)
          val overduePaymentsTableRow2: Element = pageDocument.select("tr").get(4)

          overduePaymentsTableRow1.select("td").get(1).text() shouldBe poa1WithTaxYearOverdueAndUnderReview
          overduePaymentsTableRow2.select("td").get(1).text() shouldBe s"$overdueTag $poa2Text $currentYear $paymentUnderReview"
        }
      }

      "the user has charges and access viewer with mixed dates" should {
        s"have the title $whatYouOweTitle and notes" in new Setup(charges = whatYouOweDataWithMixedData1) {
          pageDocument.title() shouldBe whatYouOweTitle
        }
        s"not have MTD payments heading" in new Setup(charges = whatYouOweDataWithMixedData1) {
          pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
        }

        "should have payment processing bullets when there is mixed dates" in new Setup(charges = whatYouOweDataWithMixedData1) {

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
          pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

        }

        s"have overdue table header, bullet points and data with hyperlink and overdue tag" in new Setup(charges = whatYouOweDataWithOverdueMixedData2(List(None, None, None))) {
          val overdueTableHeader: Element = pageDocument.select("tr").first()
          overdueTableHeader.select("th").first().text() shouldBe dueDate
          overdueTableHeader.select("th").get(1).text() shouldBe paymentType
          overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
          overdueTableHeader.select("th").last().text() shouldBe amountDue

          val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(1)
          overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
          overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " + poa2Text + s" $currentYear"
          overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
          overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

          val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(2)
          dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
          dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1Text + s" $currentYear"
          dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
          dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

          pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000125").url
          pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
          pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            LocalDate.now().getYear).url

          pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
            LocalDate.now().getYear, "1040000123").url
          pageDocument.getElementById("due-1-overdue") shouldBe null
          pageDocument.getElementById("taxYearSummary-link-1").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
            LocalDate.now().getYear).url

          pageDocument.getElementById("payments-made").text shouldBe paymentsMade
          val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
          paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
          paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
          pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        }

      }
      s"have payment data with button" in new Setup(charges = whatYouOweDataWithMixedData1) {

        pageDocument.getElementById("payment-button").text shouldBe payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(10000).url

        pageDocument.getElementById("pre-mtd-payments-heading") shouldBe null
      }
    }

    "the user has charges and access viewer with mixed dates and ACI value of zero" should {

      s"have the mtd payments, bullets and table header and data with Balancing Payment data with no hyperlink but have overdue tag" in new Setup(
        charges = whatYouOweDataWithWithAciValueZeroAndOverdue) {
        val remainingBalanceHeader: Element = pageDocument.select("tr").first()
        remainingBalanceHeader.select("th").first().text() shouldBe dueDate
        remainingBalanceHeader.select("th").get(1).text() shouldBe paymentType
        remainingBalanceHeader.select("th").get(2).text() shouldBe taxYearSummary
        remainingBalanceHeader.select("th").last().text() shouldBe amountDue

        val remainingBalanceTable: Element = pageDocument.select("tr").get(1)
        remainingBalanceTable.select("td").first().text() shouldBe LocalDate.now().minusDays(15).toLongDateShort
        remainingBalanceTable.select("td").get(1).text() shouldBe overdueTag + " " + preMTDRemainingBalance
        remainingBalanceTable.select("td").get(2).text() shouldBe preMtdPayments(
          (LocalDate.now().getYear - 2).toString, (LocalDate.now().getYear - 1).toString)
        remainingBalanceTable.select("td").last().text() shouldBe "£123,456.67"

        pageDocument.getElementById("balancing-charge-type-overdue").text shouldBe overdueTag
      }
      s"have overdue table header and data with hyperlink and overdue tag" in new Setup(charges = whatYouOweDataTestActiveWithMixedData2(List(None, None, None, None))) {
        val overdueTableHeader: Element = pageDocument.select("tr").get(0)
        overdueTableHeader.select("th").first().text() shouldBe dueDate
        overdueTableHeader.select("th").get(1).text() shouldBe paymentType
        overdueTableHeader.select("th").get(2).text() shouldBe taxYearSummary
        overdueTableHeader.select("th").last().text() shouldBe amountDue
        val overduePaymentsTableRow1: Element = pageDocument.select("tr").get(2)
        overduePaymentsTableRow1.select("td").first().text() shouldBe LocalDate.now().minusDays(1).toLongDateShort
        overduePaymentsTableRow1.select("td").get(1).text() shouldBe overdueTag + " " + poa2Text + s" $currentYear"
        overduePaymentsTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        overduePaymentsTableRow1.select("td").last().text() shouldBe "£75.00"

        val dueWithInThirtyDaysTableRow1: Element = pageDocument.select("tr").get(3)
        dueWithInThirtyDaysTableRow1.select("td").first().text() shouldBe LocalDate.now().plusDays(30).toLongDateShort
        dueWithInThirtyDaysTableRow1.select("td").get(1).text() shouldBe poa1Text + s" $currentYear"
        dueWithInThirtyDaysTableRow1.select("td").get(2).text() shouldBe taxYearSummaryText((LocalDate.now().getYear - 1).toString, LocalDate.now().getYear.toString)
        dueWithInThirtyDaysTableRow1.select("td").last().text() shouldBe "£50.00"

        pageDocument.getElementById("due-0-late-link2").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          LocalDate.now().getYear, "1040000125").url
        pageDocument.getElementById("due-0-overdue").text shouldBe overdueTag
        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
          LocalDate.now().getYear).url

        pageDocument.getElementById("due-1-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(
          LocalDate.now().getYear, "1040000123").url
        pageDocument.getElementById("due-1-overdue") shouldBe null
        pageDocument.getElementById("taxYearSummary-link-1").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(
          LocalDate.now().getYear).url

        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }

      s"have payment data with button" in new Setup(charges = whatYouOweDataWithWithAciValueZeroAndOverdue) {

        pageDocument.getElementById("payment-button").text shouldBe payNow

        pageDocument.getElementById("payment-button-link").attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(12345667).url

      }

    }

    "the user has no charges" should {

      s"have the title $whatYouOweTitle and page header and notes" in new Setup(charges = noChargesModel) {
        pageDocument.title() shouldBe whatYouOweTitle
        pageDocument.selectFirst("h1").text shouldBe whatYouOweHeading
        pageDocument.getElementById("no-payments-due").text shouldBe noPaymentsDue
        pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(saNote)
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote

      }

      "have the link to their previous Self Assessment online account in the sa-note" in new Setup(charges = noChargesModel) {
        verifySelfAssessmentLink()
      }

      "not have button Pay now" in new Setup(charges = noChargesModel) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }
      "should have payment processing bullets" in new Setup(charges = noChargesModel) {

        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

      }
    }

    "codingOut is enabled" should {
      "have coding out message displayed at the bottom of the page" in new Setup(charges = whatYouOweDataWithCodingOut, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-summary-link") should not be null
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/tax-years/2021/charge?id=CODINGOUT02"
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
      "have a class 2 Nics overdue entry" in new Setup(charges = whatYouOweDataWithCodingOut, codingOutEnabled = true) {
        pageDocument.getElementById("due-0") should not be null
        pageDocument.getElementById("due-0").text().contains("Class 2 National Insurance") shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
      }

      "should have payment processing bullets when there is coding out" in new Setup(charges = whatYouOweDataWithCodingOut) {

        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

      }

      "have a cancelled paye self assessment entry" in new Setup(charges = whatYouOweDataWithCancelledPayeSa, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-notice") shouldBe null
        pageDocument.getElementById("due-0").text().contains(cancelledPayeSelfAssessment) shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
        pageDocument.getElementById("coding-out-summary-link") shouldBe null
      }
    }

    "codingOut is disabled" should {
      "have no coding out message displayed" in new Setup(charges = whatYouOweDataWithCodingOut, codingOutEnabled = false) {
        pageDocument.getElementById("coding-out-notice") shouldBe null
      }
      "have a balancing charge overdue entry" in new Setup(charges = whatYouOweDataWithCodingOut, codingOutEnabled = false) {
        pageDocument.select("#due-0 a").get(0).text() shouldBe "Balancing payment 2021"
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
      }

      "have a cancelled paye self assessment entry" in new Setup(charges = whatYouOweDataWithCancelledPayeSa, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-notice") shouldBe null
        pageDocument.getElementById("due-0") should not be null
        pageDocument.getElementById("due-0").text().contains(cancelledPayeSelfAssessment) shouldBe true
        pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
        pageDocument.getElementById("coding-out-summary-link") shouldBe null
      }
      "show only PAYE SA, SA note and payment bullet points" in new Setup(charges = whatYouOweDataWithPayeSA, codingOutEnabled = true) {
        pageDocument.title() shouldBe whatYouOweTitle
        pageDocument.selectFirst("h1").text shouldBe whatYouOweHeading
        pageDocument.getElementById("coding-out-notice").text() shouldBe codingOutNotice
        pageDocument.getElementById("sa-note-migrated").text shouldBe saNote
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote
        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
      }

      "should have payment processing bullets" in new Setup(charges = whatYouOweDataWithCodingOut, codingOutEnabled = false) {

        pageDocument.getElementById("payments-made").text shouldBe paymentsMade
        val paymentProcessingBullet: Element = pageDocument.getElementById("payments-made-bullets")
        paymentProcessingBullet.select("li").get(0).text shouldBe paymentProcessingBullet1
        paymentProcessingBullet.select("li").get(1).text shouldBe paymentProcessingBullet2
        pageDocument.getElementById("sa-tax-bill").attr("href") shouldBe "https://www.gov.uk/pay-self-assessment-tax-bill"
        pageDocument.getElementById("sa-note-migrated").text shouldBe saNote

      }
    }

    "When codingOut is enabled - At crystallization, the user has the coding out requested amount fully collected" should {
      "only show coding out content under header" in new Setup(charges = whatYouOweDataWithCodingOut, codingOutEnabled = true) {
        pageDocument.getElementById("coding-out-notice").text() shouldBe codingOutNotice
        pageDocument.getElementById("coding-out-summary-link").attr("href") shouldBe
          "/report-quarterly/income-and-expenses/view/tax-years/2021/charge?id=CODINGOUT02"
        pageDocument.getElementById("coding-out-notice").text().contains(codingOutAmount.toString)
      }
      "show no payments due content when coding out is disabled" in new Setup(charges = noChargesModel, codingOutEnabled = false) {
        pageDocument.title() shouldBe whatYouOweTitle
        pageDocument.selectFirst("h1").text shouldBe whatYouOweHeading
        pageDocument.getElementById("no-payments-due").text shouldBe noPaymentsDue
        pageDocument.getElementById("payments-due-note").selectFirst("a").text.contains(saNote)
        pageDocument.getElementById("outstanding-charges-note-migrated").text shouldBe osChargesNote

      }
    }
  }

  "MFA Debits is enabled" should {
    "have an HMRC adjustment payment due" in new Setup(charges = whatYouOweDataWithMFADebits, mfaCreditsAndDebitsEnabled = true) {
      pageDocument.title() shouldBe whatYouOweTitle
      pageDocument.selectFirst("h1").text shouldBe whatYouOweHeading
      pageDocument.getElementById("due-0").text.contains(hmrcAdjustment) shouldBe true
      pageDocument.select("#payments-due-table tbody > tr").size() shouldBe 1
    }
    "display the payment details content" in new Setup(charges = whatYouOweDataWithMFADebits, mfaCreditsAndDebitsEnabled = true) {
      pageDocument.getElementById("hmrc-adjustment-heading").text  shouldBe hmrcAdjustmentHeading
      pageDocument.getElementById("hmrc-adjustment-line1").text  shouldBe hmrcAdjustmentLine1
    }
  }

  "agent" when {
    "The What you owe view with financial details model" when {
      s"have the title '${
        messages("agent.title_pattern.service_name.govuk", messages("whatYouOwe.heading"))
      }'" in new AgentSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
        pageDocument.title() shouldBe messages("agent.title_pattern.service_name.govuk", messages("whatYouOwe.heading"))
        pageDocument.getElementById("due-0-link").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(
          LocalDate.now().getYear, "1040000124").url
        pageDocument.getElementById("taxYearSummary-link-0").attr("href") shouldBe controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(
          LocalDate.now().getYear).url
      }
      "not have button Pay now with no chagres" in new AgentSetup(charges = noChargesModel) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }
      "not have button Pay now with charges" in new AgentSetup(charges = whatYouOweDataWithDataDueIn30Days()) {
        Option(pageDocument.getElementById("payment-button")) shouldBe None
      }
    }
  }

  "what you owe view" should {

    val unallocatedCreditMsg = "You have £100.00 in your account. We’ll use this to pay the amount due on the next due date."
    "show unallocated credits" when {
      "user is an individual with the feature switch on" in new Setup(creditCharges = creditDocumentDetailList,
        charges = whatYouOweDataWithDataDueInMoreThan30Days(), whatYouOweCreditAmountEnabled = true){
        pageDocument.getElementById("unallocated-credit-note").text() shouldBe unallocatedCreditMsg
      }

      "user is an agent with the feature switch on" in new AgentSetup(creditCharges =  creditDocumentDetailList,
        charges = whatYouOweDataWithDataDueInMoreThan30Days(), whatYouOweCreditAmountEnabled = true){
        pageDocument.getElementById("unallocated-credit-note").text() shouldBe unallocatedCreditMsg
      }
    }

    "not show unallocated credits" when {
      "user is an individual with the feature switch off" in new Setup(creditCharges= creditDocumentDetailList,
        charges = whatYouOweDataWithDataDueInMoreThan30Days()){
        pageDocument.getElementById("unallocated-credit-note") shouldBe null
      }

      "user is an agent with the feature switch on" in new AgentSetup(creditCharges= creditDocumentDetailList,
        charges = whatYouOweDataWithDataDueInMoreThan30Days()) {
        pageDocument.getElementById("unallocated-credit-note") shouldBe null
      }
    }
  }
}
