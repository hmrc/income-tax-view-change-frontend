/*
 * Copyright 2023 HM Revenue & Customs
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
import authV2.AuthActionsTestData.{defaultMTDITUser, getMinimalMTDITUser}
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.PaymentHistoryRefunds
import models.homePage._
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import models.obligations.NextUpdatesTileViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants._
import testConstants.FinancialDetailsTestConstants.financialDetailsModel
import testUtils.TestSupport
import views.html.Home

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.Random


class HomePageViewSpec extends TestSupport with FeatureSwitching {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = fixedDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  def getIncomeSource(migratedUser: Boolean): IncomeSourceDetailsModel = {
    if (migratedUser) {
      IncomeSourceDetailsModel(testNino, mtdbsa = testMtditid, yearOfMigration = Some("2018"), businesses = Nil, properties = Nil)
    } else {
      IncomeSourceDetailsModel(testNino, mtdbsa = testMtditid, yearOfMigration = None, businesses = Nil, properties = Nil)
    }
  }

  def testMtdItUser(hasSAUtr: Boolean = true, optIncomeSourceDetailsModel: Option[IncomeSourceDetailsModel] = None): MtdItUser[_] = {
    val incomeSourceDetailsModel = optIncomeSourceDetailsModel.getOrElse(getIncomeSource(Random.nextBoolean()))
    if (hasSAUtr) {
      defaultMTDITUser(Some(testUserTypeIndividual), incomeSourceDetailsModel)
        .addNavBar(testNavHtml)
    } else {
      getMinimalMTDITUser(Some(testUserTypeIndividual), incomeSourceDetailsModel)
        .addNavBar(testNavHtml)
    }
  }

  def testMtdItUserMigrated(hasSAUtr: Boolean = true): MtdItUser[_] = testMtdItUser(hasSAUtr, Some(getIncomeSource(true)))

  def testMtdItUserNotMigrated(hasSAUtr: Boolean = true): MtdItUser[_] = testMtdItUser(hasSAUtr, Some(getIncomeSource(false)))

  val updateDate: LocalDate = LocalDate.of(2100, 1, 1)
  val updateDateLongDate = "1 January 2100"
  val multipleOverdueUpdates = "3 Overdue updates"
  val nextPaymentDueDate: LocalDate = LocalDate.of(2019, 1, 31)
  val paymentDateLongDate = "31 January 2019"
  val multipleOverdueCharges = "3 Overdue charges"
  val overdueMessage = "! Warning You have overdue charges. You may be charged interest on these until they are paid in full."
  val overdueMessageForDunningLocks = "! Warning You have overdue payments and one or more of your tax decisions are being reviewed. You may be charged interest on these until they are paid in full."
  val currentDate = dateService.getCurrentDate
  private val viewModelFuture: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelOneOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1)), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelThreeOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 2, 1), LocalDate.of(2018, 3, 1)), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelNoUpdates: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelOptOut: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, true, false, ITSAStatus.NoStatus, None, None)
  val paymentTileOverdueDate: LocalDate = LocalDate.of(2020, 4, 6)
  val paymentTileFutureDate: LocalDate = LocalDate.of(2100, 4, 6)
  val paymentTileFutureDateLongFormat: String = paymentTileFutureDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY"))
  val paymentTileOverdueDateLongFormat: String = s"Overdue ${paymentTileOverdueDate.format(DateTimeFormatter.ofPattern("d MMMM YYYY"))}"
  val testFutureTaxYear = TaxYear(2099,2100)

  class Setup(paymentDueDate: LocalDate = nextPaymentDueDate, overDuePaymentsCount: Int = 0, paymentsAccruingInterestCount: Int = 0, reviewAndReconcileEnabled: Boolean = false,
              nextUpdatesTileViewModel: NextUpdatesTileViewModel = viewModelFuture, utr: Option[String] = Some("1234567890"), paymentHistoryEnabled: Boolean = true, ITSASubmissionIntegrationEnabled: Boolean = true,
              user: MtdItUser[_] = testMtdItUser(), dunningLockExists: Boolean = false, creditAndRefundEnabled: Boolean = false, displayCeaseAnIncome: Boolean = false,
              incomeSourcesEnabled: Boolean = false, incomeSourcesNewJourneyEnabled: Boolean = false, reportingFrequencyEnabled: Boolean = false, penaltiesAndAppealsIsEnabled: Boolean = true,
              penaltyPoints: Int = 0, submissionFrequency: String = "Annual", currentITSAStatus: ITSAStatus = ITSAStatus.Voluntary, yourSelfAssessmentChargesEnabled: Boolean = false) {

    val returnsTileViewModel = ReturnsTileViewModel(currentTaxYear = TaxYear(currentTaxYear - 1, currentTaxYear), iTSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled)

    val nextPaymentsTileViewModel = NextPaymentsTileViewModel(Some(paymentDueDate), overDuePaymentsCount, paymentsAccruingInterestCount, yourSelfAssessmentChargesEnabled)

    val paymentCreditAndRefundHistoryTileViewModel = PaymentCreditAndRefundHistoryTileViewModel(List(financialDetailsModel()), creditAndRefundEnabled, paymentHistoryEnabled, isUserMigrated = user.incomeSources.yearOfMigration.isDefined)

    val yourBusinessesTileViewModel = YourBusinessesTileViewModel(displayCeaseAnIncome, incomeSourcesEnabled, incomeSourcesNewJourneyEnabled)

    val yourReportingObligationsTileViewModel = YourReportingObligationsTileViewModel(TaxYear(currentTaxYear, currentTaxYear + 1), reportingFrequencyEnabled, currentITSAStatus)

    val penaltiesAndAppealsTileViewModel = PenaltiesAndAppealsTileViewModel(penaltiesAndAppealsIsEnabled, submissionFrequency, penaltyPoints)

    val homePageViewModel = HomePageViewModel(
      utr = utr,
      nextUpdatesTileViewModel = nextUpdatesTileViewModel,
      returnsTileViewModel = returnsTileViewModel,
      nextPaymentsTileViewModel = nextPaymentsTileViewModel,
      paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel,
      yourBusinessesTileViewModel = yourBusinessesTileViewModel,
      yourReportingObligationsTileViewModel = yourReportingObligationsTileViewModel,
      penaltiesAndAppealsTileViewModel = penaltiesAndAppealsTileViewModel,
      dunningLockExists = dunningLockExists
    )

    val home: Home = app.injector.instanceOf[Home]
    lazy val page: HtmlFormat.Appendable = home(
      homePageViewModel
    )(FakeRequest(), implicitly, user, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    val user2 = user.copy(authUserDetails = user.authUserDetails.copy(name = None))
    lazy val page2 = home(homePageViewModel)(FakeRequest(), implicitly, user2, implicitly)
    lazy val document2: Document = Jsoup.parse(contentAsString(page2))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

  }

  "home" should {

    "provided with a btaNavPartial" should {

      "render the btaNavPartial" in new Setup {
        document.getElementById(s"nav-bar-link-testEnHome").text shouldBe "testEnHome"
      }
    }

    s"have the correct link to the government homepage" in new Setup {
      document.getElementsByClass("govuk-header__link").attr("href") shouldBe "https://www.gov.uk"
    }

    s"have the title ${messages("htmlTitle", messages("home.heading"))}" in new Setup {
      document.title() shouldBe "Income Tax - Manage your Self Assessment - GOV.UK"
    }

    s"have the users name as caption" in new Setup {
      getTextOfElementById("sub-heading") shouldBe Some(testUserName)
    }

    s"have the page heading '${messages("home.heading")}'" in new Setup {
      getTextOfElementById("income-tax-heading") shouldBe Some(s"Income Tax")
    }

    "have the right keep-alive url in hmrc timeout dialog" in new Setup {
      val keepAliveUrl = "/report-quarterly/income-and-expenses/view/keep-alive"
      document.head().select("meta[name='hmrc-timeout-dialog']")
        .attr("data-keep-alive-url") shouldBe keepAliveUrl
    }

    s"have the subheading with the users name '$testUserName'" in new Setup {
      getTextOfElementById("sub-heading") shouldBe Some(testUserName)
    }

    "have the users UTR" in new Setup {
      getTextOfElementById("utr-reference-heading") shouldBe Some(s"Unique Taxpayer Reference (UTR): $testSaUtr")
    }

    "not have the users UTR when it is absent in user profile" in new Setup(user = testMtdItUser(false)) {
      getElementById("utr-reference-heading") shouldBe None
    }

    "have a language selection switch" which {

      "displays the correct content" in new Setup(user = testMtdItUser(false)) {
        val langSwitchScript: Option[Element] = getElementById("language-switch")
        langSwitchScript.map(_.select("li:nth-child(1)").text) shouldBe Some("English")
        langSwitchScript.map(_.select("li:nth-child(2)").text) shouldBe Some("Newid yr iaith ir Gymraeg Cymraeg")
      }
    }

    "have an updates tile" which {
      "has a heading" in new Setup {
        getElementById("updates-tile").map(_.select("h2").text) shouldBe Some("Next updates due")
      }
      "has the date of the next update due" in new Setup {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(updateDateLongDate)
      }
      "display an overdue tag when a single update is overdue" in new Setup(nextUpdatesTileViewModel = viewModelOneOverdue) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("Overdue " + "1 January 2018")
      }
      "has the correct number of overdue updates when three updates are overdue" in new Setup(nextUpdatesTileViewModel = viewModelThreeOverdue) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverdueUpdates)
      }
      "has a link to view updates" in new Setup {
        val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/next-updates")
        link.map(_.text) shouldBe Some("View update deadlines")
      }
      "is empty except for the title" when {
        "user has no open obligations" in new Setup(nextUpdatesTileViewModel = viewModelNoUpdates) {
          getElementById("updates-tile").map(_.text()) shouldBe Some("Next updates due")
        }
      }
      "has a link to view and manage updates - Opt Out" in new Setup(nextUpdatesTileViewModel = viewModelOptOut) {
        val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/next-updates")
        link.map(_.text) shouldBe Some("View deadlines and manage how you report")
      }

      "has next update and tax return dates when OptInOptOutContentUpdateR17 is enabled and ITSA status is Voluntary with no overdue updates" in {
        val testNextQuarterlyUpdateDueDate: LocalDate = LocalDate.of(2099, 11, 5)
        val testNextTaxReturnDueDate: LocalDate = LocalDate.of(testFutureTaxYear.endYear + 1, 1, 31)

        new Setup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(testNextQuarterlyUpdateDueDate),
            currentDate = LocalDate.of(testFutureTaxYear.startYear, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Voluntary,
            nextQuarterlyUpdateDueDate = Some(testNextQuarterlyUpdateDueDate),
            nextTaxReturnDueDate = Some(testNextTaxReturnDueDate))
        ) {
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")

          tile.select("span.govuk-tag--red") shouldBe empty

          paragraphs.get(0).text shouldBe "Next update due: 5 November 2099"
          paragraphs.get(1).text shouldBe "Next tax return due: 31 January 2101"

          val link: Element = tile.select("a.govuk-link").first()
          link.text shouldBe "View deadlines and manage how you report"
          link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
        }
      }

      "has overdue update and tax return dates when OptInOptOutContentUpdateR17 is enabled and ITSA status is Voluntary with 1 overdue update" in {
        val testOverdueUpdate: LocalDate = LocalDate.of(2000, 2, 5)
        val testNextQuarterlyUpdateDueDate: LocalDate = LocalDate.of(2099, 11, 5)
        val testNextTaxReturnDueDate: LocalDate = LocalDate.of(testFutureTaxYear.endYear + 1, 1, 31)

        new Setup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(testOverdueUpdate, testNextQuarterlyUpdateDueDate),
            currentDate = LocalDate.of(testFutureTaxYear.startYear, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Voluntary,
            nextQuarterlyUpdateDueDate = Some(testNextQuarterlyUpdateDueDate),
            nextTaxReturnDueDate = Some(testNextTaxReturnDueDate))
        ) {
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")

          val overdueTag: Elements = tile.select("span.govuk-tag.govuk-tag--red")
          overdueTag.text() shouldBe "Overdue"

          paragraphs.get(1).text shouldBe "Next update due: 5 November 2099"
          paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2101"

          val link: Element = tile.select("a.govuk-link").first()
          link.text shouldBe "View deadlines and manage how you report"
          link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
        }
      }

      "has overdue count and tax return when OptInOptOutContentUpdateR17 is enabled and ITSA status is Voluntary with multiple overdue updates" in {
        val testNextQuarterlyUpdateDueDate: LocalDate = LocalDate.of(2099, 11, 5)
        val testNextTaxReturnDueDate: LocalDate = LocalDate.of(testFutureTaxYear.endYear + 1, 1, 31)

        new Setup(
             nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(
              LocalDate.of(2000, 5, 5),
              LocalDate.of(2000, 8, 5),
              LocalDate.of(2000, 11, 5),
              testNextQuarterlyUpdateDueDate
            ),
            currentDate = LocalDate.of(testFutureTaxYear.startYear, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Voluntary,
            nextQuarterlyUpdateDueDate = Some(testNextQuarterlyUpdateDueDate),
            nextTaxReturnDueDate = Some(testNextTaxReturnDueDate))
        ) {
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")

          val overdueTag: Elements = tile.select("span.govuk-tag.govuk-tag--red")
          overdueTag.text() shouldBe "3 Overdue updates"

          paragraphs.get(1).text shouldBe "Next update due: 5 November 2099"
          paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2101"

          val link: Element = tile.select("a.govuk-link").first()
          link.text shouldBe "View deadlines and manage how you report"
          link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
        }
      }

      "has only tax return date when OptInOptOutContentUpdateR17 is enabled and ITSA status is Annual" in {
        val testNextQuarterlyUpdateDueDate: LocalDate = LocalDate.of(2099, 11, 5)
        val testNextTaxReturnDueDate: LocalDate = LocalDate.of(testFutureTaxYear.endYear + 1, 1, 31)

        new Setup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(testNextQuarterlyUpdateDueDate),
            currentDate = LocalDate.of(testFutureTaxYear.startYear, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Annual,
            nextQuarterlyUpdateDueDate = Some(testNextQuarterlyUpdateDueDate),
            nextTaxReturnDueDate = Some(testNextTaxReturnDueDate))
        ) {
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")

          tile.select("span.govuk-tag--red") shouldBe empty

          paragraphs.size() shouldBe 1
          paragraphs.get(0).text shouldBe "Next tax return due: 31 January 2101"

          val link: Element = tile.select("a.govuk-link").first()
          link.text shouldBe "View deadlines and manage how you report"
          link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
        }
      }

      "has overdue update and tax return dates when OptInOptOutContentUpdateR17 is enabled and ITSA status is Mandated with 1 overdue update" in new Setup(
        nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(LocalDate.of(2025, 4, 5)),
          currentDate = LocalDate.of(2025, 6, 24),
          isReportingFrequencyEnabled = true,
          showOptInOptOutContentUpdateR17 = true,
          currentYearITSAStatus = ITSAStatus.Mandated,
          nextQuarterlyUpdateDueDate = Some(LocalDate.of(2025, 4, 5)),
          nextTaxReturnDueDate = Some(LocalDate.of(2026, 1, 31)))
      ) {
        val tile: Element = getElementById("updates-tile").get
        val paragraphs: Elements = tile.select("p.govuk-body")

        val overdueTag: Elements = tile.select("span.govuk-tag.govuk-tag--red")
        overdueTag.text() shouldBe "Overdue"

        paragraphs.get(1).text shouldBe "Next update due: 5 April 2025"
        paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2026"

        val link: Element = tile.select("a.govuk-link").first()
        link.text shouldBe "View deadlines and manage how you report"
        link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/next-updates"
      }

      "has only title when OptInOptOutContentUpdateR17 is enabled and user has no obligations or tax return date" in new Setup(
        nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq.empty,
          currentDate = LocalDate.of(2025, 6, 24),
          isReportingFrequencyEnabled = true,
          showOptInOptOutContentUpdateR17 = true,
          currentYearITSAStatus = ITSAStatus.Voluntary,
          None,
          nextTaxReturnDueDate = None)
      ) {
        val tile: Element = getElementById("updates-tile").get

        tile.text().trim shouldBe "Your updates and deadlines"
        tile.select("span.govuk-tag--red") shouldBe empty
        tile.select("p.govuk-body") shouldBe empty
      }
    }

    "have a payments due tile" which {
      "has a heading" in new Setup {
        getElementById("payments-tile").map(_.select("h2").text) shouldBe Some("Next charges due")
      }
      "has the date of the next update due" in new Setup {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(paymentDateLongDate)
      }
      "don't display any warning messages when no payment is overdue and no payments are accruing interest" in new Setup(overDuePaymentsCount = 0) {
        getTextOfElementById("overdue-warning") shouldBe None
        getTextOfElementById("accrues-interest-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue" in new Setup(overDuePaymentsCount = 1) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessage)
      }

      "display an dunning lock overdue warning message when a payment is overdue" in new Setup(overDuePaymentsCount = 1, dunningLockExists = true) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageForDunningLocks)
      }

      "display daily interest warning when payments are accruing interest" in new Setup(paymentsAccruingInterestCount = 2, reviewAndReconcileEnabled = true) {
        val dailyInterestMessage = "! Warning You have charges with added daily interest. These charges will be accruing interest until they are paid in full."
        getTextOfElementById("accrues-interest-warning") shouldBe Some(dailyInterestMessage)
      }

      "display an overdue tag when a single update is overdue" in new Setup(overDuePaymentsCount = 1) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("Overdue " + paymentDateLongDate)
      }

      "display only the date when there are payments due but none being overdue" in new Setup(paymentDueDate = paymentTileFutureDate, overDuePaymentsCount = 0) {
        document.select("#payments-tile > div > p.govuk-body").text() shouldBe paymentTileFutureDateLongFormat
      }

      "display the date and an overdue tag when there is one payment that is overdue" in new Setup(paymentDueDate = paymentTileOverdueDate, overDuePaymentsCount = 1) {
        document.select("#payments-tile > div > p.govuk-body").text() shouldBe paymentTileOverdueDateLongFormat
      }

      "display daily interest tag when there are payments accruing interest" in new Setup(paymentsAccruingInterestCount = 2, reviewAndReconcileEnabled = true) {
        getElementById("accrues-interest-tag").map(_.text()) shouldBe Some(s"Daily interest charges")
      }

      "has the correct number of overdue updates when three updates are overdue" in new Setup(overDuePaymentsCount = 3) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverdueCharges)
      }
      "has a link to view payments" in new Setup {
        val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/what-you-owe")
        link.map(_.text) shouldBe Some("Check what you owe")
      }
    }

    "have a returns tile" which {
      "has a heading" in new Setup {
        getElementById("returns-tile").map(_.select("h2").text) shouldBe Some("Returns")
      }
      "has a link to the view payments page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").first)
        link.map(_.attr("href")) shouldBe Some(s"/report-quarterly/income-and-expenses/view/tax-year-summary/$currentTaxYear")
        link.map(_.text) shouldBe Some(s"View your current ${currentTaxYear - 1} to $currentTaxYear return")
      }
      "has a link to the update and submit page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) shouldBe Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
        link.map(_.text) shouldBe Some(s"Update and submit your ${currentTaxYear - 1} to $currentTaxYear return")
      }
      "dont have a link to the update and submit page when ITSASubmissionIntegrationEnabled is disabled" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) should not be Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
        link.map(_.text) should not be Some(s"Update and submit your ${currentTaxYear - 1} to $currentTaxYear return")
      }
      "has a link to the tax years page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").last)
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/tax-years")
        link.map(_.text) shouldBe Some("View all tax years")
      }
    }

    "have a payment history tile" which {

      "has a payment and history refunds heading when payment history feature switch is enabled" in new Setup(paymentHistoryEnabled = true, creditAndRefundEnabled = false) {
        getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some("Payment history and refunds")
      }
      "has a payment history heading when payment history feature switch is disabled" in new Setup(paymentHistoryEnabled = false, creditAndRefundEnabled = false) {
        disable(PaymentHistoryRefunds)
        getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some("Payment history")
      }
      "has a link to the payment and refund history page" which {
        "has payment and refund history link when CreditsRefundsRepay OFF / PaymentHistoryRefunds ON" in new Setup(creditAndRefundEnabled = false, paymentHistoryEnabled = true) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/payment-refund-history")
          link.map(_.text) shouldBe Some("Payment and refund history")
        }
        "has payment and credit history link when CreditsRefundsRepay ON / PaymentHistoryRefunds OFF" in new Setup(creditAndRefundEnabled = true, paymentHistoryEnabled = false) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/payment-refund-history")
          link.map(_.text) shouldBe Some("Payment and credit history")
        }
        "has payment, credit and refund history link when CreditsRefundsRepay ON / PaymentHistoryRefunds ON" in new Setup(creditAndRefundEnabled = true, paymentHistoryEnabled = true) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/payment-refund-history")
          link.map(_.text) shouldBe Some("Payment, credit and refund history")
        }
        "has payment history link when CreditsRefundsRepay OFF / PaymentHistoryRefunds OFF" in new Setup(creditAndRefundEnabled = false, paymentHistoryEnabled = false) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/payment-refund-history")
          link.map(_.text) shouldBe Some("Payment history")
        }
        s"has the available credit " in new Setup(creditAndRefundEnabled = true) {
          getTextOfElementById("available-credit") shouldBe Some("£100.00 is in your account")
        }
      }
      "has an link to the 'How to claim a refund' for not migrated user" in new Setup(user = testMtdItUserNotMigrated()) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
        // next line would change as part of MISUV-3710 implementation
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/payment-refund-history")
        link.map(_.text) shouldBe Some("Payment and refund history")
      }

    }

    "have an Income Sources tile with feature switch enabled" which {
      "has a heading" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true) {
        getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some("Income Sources")
      }
      "has a link to AddIncomeSourceController.show()" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true) {
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some("Add a new sole trader or property income source")
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
      }
      "has a link to ManageIncomeSourceController.show()" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true) {
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").text()) shouldBe Some("View and manage income sources")
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/income-sources/manage/view-and-manage-income-sources")
      }
    }
    "not have an Income Sources tile" when {
      "feature switch is disabled" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = false) {
        getElementById("income-sources-tile") shouldBe None
      }
    }
    "have a Your Businesses tile" when {
      "the new income sources journey FS is enabled" which {
        "has a heading" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
          getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some("Your businesses")
        }
        "has a link to ManageYourBusinessController.show()" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some("Add, manage or cease a business or income source")
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/manage-your-businesses")
        }
      }
    }

    "have a Reporting Obligations tile" when {
      "the reporting obligations page FS is enabled" which {
        "has a heading" in new Setup(user = testMtdItUserMigrated(), reportingFrequencyEnabled = true) {
          getElementById("reporting-obligations-tile").map(_.select("h2").first().text()) shouldBe Some("Your reporting obligations")
        }
        "has text for reporting quarterly(voluntary)" in new Setup(user = testMtdItUserMigrated(), reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Voluntary) {
          getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to:")
          document.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "use Making Tax Digital for Income Tax submit a tax return"
        }
        "has text for reporting quarterly(mandated)" in new Setup(user = testMtdItUserMigrated(), reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Mandated) {
          getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to:")
          document.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "use Making Tax Digital for Income Tax submit a tax return"
        }
        "has text for reporting annually" in new Setup(user = testMtdItUserMigrated(), reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Annual) {
          getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to submit a tax return")
        }

        "has a link to the reporting obligations page" in new Setup(user = testMtdItUserMigrated(), reportingFrequencyEnabled = true) {
          getElementById("reporting-obligations-link").map(_.text()) shouldBe Some("View and manage your reporting obligations")
          getElementById("reporting-obligations-link").map(_.attr("href")) shouldBe Some(controllers.routes.ReportingFrequencyPageController.show(false).url)
        }
      }

      "the reporting frequency page FS is disabled" which {
        "does not have the Reporting Obligations tile" in new Setup(user = testMtdItUserMigrated(), reportingFrequencyEnabled = false) {
          getElementById("reporting-obligations-tile") shouldBe None
        }
      }
    }

    "have a Penalties and Appeals tile" when {
      "PenaltiesAndAppeals FS is enabled" which {
        "has a heading" in new Setup(submissionFrequency = "Annual", penaltyPoints = 2) {
          getElementById("penalties-and-appeals-tile").map(_.select("h2").first().text()) shouldBe Some("Penalties and appeals")
        }
        "has a link to Self Assessment Penalties and Appeals page" in new Setup(penaltiesAndAppealsIsEnabled = true) {
          getElementById("sa-penalties-and-appeals-link").map(_.text()) shouldBe Some("Check Self Assessment penalties and appeals")
          getElementById("sa-penalties-and-appeals-link").map(_.attr("href")) shouldBe Some(appConfig.incomeTaxPenaltiesFrontend)
        }
        "has a two-points penalty tag" in new Setup(submissionFrequency = "Annual", penaltyPoints = 3) {
          getElementById("penalty-points-tag").map(_.text()) shouldBe Some("2 Penalty points")
        }
        "has a four-points penalty tag" in new Setup(submissionFrequency = "Quarterly", penaltyPoints = 4) {
          getElementById("penalty-points-tag").map(_.text()) shouldBe Some("4 Penalty points")
        }
        "has no penalty tag if 2 points reached but User is reporting Quarterly" in new Setup(submissionFrequency = "Quarterly", penaltyPoints = 2) {
          getElementById("penalty-points-tag").map(_.text()).isDefined shouldBe false
        }
        "has no penalty tag if user has only 1 point" in new Setup(penaltyPoints = 1) {
          getElementById("penalty-points-tag").map(_.text()).isDefined shouldBe false
        }
      }
    }

    "show the 'Claim refund' link for migrated user" when {
      "the claim a refund feature switch is on" in new Setup(user = testMtdItUserMigrated(), creditAndRefundEnabled = true) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").last())
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/claim-refund")
        link.map(_.text) shouldBe Some("Claim a refund")
      }
    }

    "show the 'How to claim a refund' link for not migrated user" when {
      "the claim a refund feature switch is on" in new Setup(user = testMtdItUserNotMigrated(), creditAndRefundEnabled = true) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").last())
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/how-to-claim-refund")
        link.map(_.text) shouldBe Some("How to claim a refund")
      }
    }
  }
}
