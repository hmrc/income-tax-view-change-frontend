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

package views.agent

import auth.MtdItUser
import authV2.AuthActionsTestData.{defaultMTDITUser, getMinimalMTDITUser}
import config.FrontendAppConfig
import config.featureswitch._
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
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import views.html.agent.PrimaryAgentHome

import java.time.{LocalDate, Month}
import scala.util.Try

class PrimaryAgentHomePageViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  lazy val backUrl: String = controllers.agent.routes.ConfirmClientUTRController.show().url

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = fixedDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, Month.APRIL, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val testMtdItUserNotMigrated: MtdItUser[_] = defaultMTDITUser(Some(Agent),
    IncomeSourceDetailsModel(testNino, testMtditid, None, Nil, Nil)
  )

  val testMtdItUserMigrated: MtdItUser[_] = defaultMTDITUser(Some(Agent),
    IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, Nil))

  val testMtdItUserNoClientName = getMinimalMTDITUser(Some(Agent), IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, Nil))

  val year2018: Int = 2018
  val year2019: Int = 2019

  val nextUpdateDue: LocalDate = LocalDate.of(2100, Month.JANUARY, 1)

  val nextPaymentDue: LocalDate = LocalDate.of(year2019, Month.JANUARY, 31)

  val currentDate = dateService.getCurrentDate
  private val viewModelFuture: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelOneOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1)), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelTwoOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 2, 1)), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelNoUpdates: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(), currentDate, false, false, ITSAStatus.NoStatus, None, None)
  private val viewModelOptOut: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, true, false, ITSAStatus.NoStatus, None, None)

  class TestSetup(nextPaymentDueDate: Option[LocalDate] = Some(nextPaymentDue),
                  overduePaymentExists: Boolean = true,
                  overDuePaymentsCount: Int = 0,
                  paymentsAccruingInterestCount: Int = 0,
                  yourSelfAssessmentChargesEnabled: Boolean = false,
                  nextUpdatesTileViewModel: NextUpdatesTileViewModel = viewModelFuture,
                  utr: Option[String] = None,
                  paymentHistoryEnabled: Boolean = true,
                  ITSASubmissionIntegrationEnabled: Boolean = true,
                  dunningLockExists: Boolean = false,
                  currentTaxYear: Int = currentTaxYear,
                  displayCeaseAnIncome: Boolean = false,
                  incomeSourcesEnabled: Boolean = false,
                  incomeSourcesNewJourneyEnabled: Boolean = false,
                  creditAndRefundEnabled: Boolean = false,
                  user: MtdItUser[_] = testMtdItUserNotMigrated,
                  reportingFrequencyEnabled: Boolean = false,
                  penaltiesAndAppealsIsEnabled: Boolean = true,
                  submissionFrequency: String = "Annual",
                  penaltyPoints: Int = 0,
                  currentITSAStatus: ITSAStatus = ITSAStatus.Voluntary
                 ) {

    val agentHome: PrimaryAgentHome = app.injector.instanceOf[PrimaryAgentHome]

    val paymentCreditAndRefundHistoryTileViewModel = PaymentCreditAndRefundHistoryTileViewModel(List(financialDetailsModel()), creditAndRefundEnabled, paymentHistoryEnabled, isUserMigrated = user.incomeSources.yearOfMigration.isDefined)

    val returnsTileViewModel = ReturnsTileViewModel(currentTaxYear = TaxYear(currentTaxYear - 1, currentTaxYear), iTSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled)

    val nextPaymentsTileViewModel = NextPaymentsTileViewModel(nextPaymentDueDate, overDuePaymentsCount, paymentsAccruingInterestCount, yourSelfAssessmentChargesEnabled)

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
    val view: HtmlFormat.Appendable = agentHome(
      homePageViewModel
    )(FakeRequest(), implicitly, user, mockAppConfig)

    lazy val document: Document = Jsoup.parse(contentAsString(view))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

    def getHintNth(index: Int = 0): Option[String] = {
      Try(document.getElementsByClass("govuk-hint").get(index).text).toOption
    }
  }


  "home" when {

    "all features are enabled" should {

      s"have the correct link to the government homepage" in new TestSetup {
        document.getElementsByClass("govuk-header__link").attr("href") shouldBe "https://www.gov.uk"
      }

      s"have the title ${messages("htmlTitle.agent", messages("home.agent.heading"))}" in new TestSetup() {
        document.title() shouldBe "Your client’s Income Tax - Manage your Self Assessment - GOV.UK"
      }

      s"have the page caption You are signed in as a main agent" in new TestSetup {
        document.getElementsByClass("govuk-caption-xl").text() shouldBe "You are signed in as a main agent"
      }

      s"have the page heading ${messages("home.agent.headingWithClientName", testClientNameString)}" in new TestSetup {
        document.select("h1").text() shouldBe "Jon Jones’s Income Tax"
      }

      s"have the page heading ${messages("home.agent.heading")}" in new TestSetup(user = testMtdItUserNoClientName) {
        document.select("h1").text() shouldBe "Your client’s Income Tax"
      }

      s"have the hint with the clients utr '$testSaUtr' " in new TestSetup {
        getHintNth() shouldBe Some(s"Unique Taxpayer Reference (UTR): $testSaUtr")
      }

      "have an next payment due tile" which {
        "has a heading" in new TestSetup {
          getElementById("payments-tile").map(_.select("h2").text) shouldBe Some("Next charges due")
        }
        "has content of the next payment due" which {
          "is overdue" in new TestSetup(nextPaymentDueDate = Some(nextPaymentDue), overDuePaymentsCount = 1) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"Overdue 31 January $year2019")
          }
          "has payments accruing interest" in new TestSetup(nextPaymentDueDate = Some(nextPaymentDue), paymentsAccruingInterestCount = 2) {
            getElementById("accrues-interest-tag").map(_.text()) shouldBe Some(s"Daily interest charges")
          }
          "is not overdue" in new TestSetup(nextPaymentDueDate = Some(nextPaymentDue)) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"31 January $year2019")
          }
          "is a count of overdue payments" in new TestSetup(nextPaymentDueDate = Some(nextPaymentDue), overDuePaymentsCount = 2) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 Overdue charges")
          }
          "has no next payment" in new TestSetup(nextPaymentDueDate = None) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"No payments due")
          }
        }
        "has the date of the next payment due" in new TestSetup {
          val paymentDueDateLongDate: String = s"31 January $year2019"
          getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(paymentDueDateLongDate)
        }
        "has a link to check what your client owes" in new TestSetup {
          val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some(controllers.routes.WhatYouOweController.showAgent().url)
          link.map(_.text) shouldBe Some("Check what your client owes")
        }
      }

      "dont display any warning message when no payment is overdue, and no payments are accruing interest" in new TestSetup(overduePaymentExists = false) {
        getTextOfElementById("overdue-warning") shouldBe None
        getTextOfElementById("accrues-interest-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue and dunning lock does not exist" in new TestSetup(overDuePaymentsCount = 1) {
        val overdueMessageWithoutDunningLock = "! Warning You have overdue charges. You may be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageWithoutDunningLock)
      }

      "display an overdue warning message when a payment is overdue and dunning lock exists" in new TestSetup(overDuePaymentsCount = 1, dunningLockExists = true) {
        val overdueMessageWithDunningLock = "! Warning Your client has overdue payments and one or more of their tax decisions are being reviewed. They may be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageWithDunningLock)
      }

      "display daily interest warning when payments are accruing interest" in new TestSetup(paymentsAccruingInterestCount = 2) {
        val dailyInterestMessage = "! Warning You have charges with added daily interest. These charges will be accruing interest until they are paid in full."
        getTextOfElementById("accrues-interest-warning") shouldBe Some(dailyInterestMessage)
      }

      "have a next updates due tile" which {
        "has a heading" in new TestSetup {
          getElementById("updates-tile").map(_.select("h2").text) shouldBe Some("Next updates due")
        }
        "has content of the next update due" which {
          "is overdue" in new TestSetup(nextUpdatesTileViewModel = viewModelOneOverdue) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"Overdue 1 January $year2018")
          }
          "is not overdue" in new TestSetup(nextPaymentDueDate = Some(nextUpdateDue)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"1 January 2100")
          }
          "is a count of overdue updates" in new TestSetup(nextUpdatesTileViewModel = viewModelTwoOverdue) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 Overdue updates")
          }
        }
        "has a link to view updates" in new TestSetup {
          val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/next-updates")
          link.map(_.text) shouldBe Some("View update deadlines")
        }
        "is empty except for the title" when {
          "user has no open obligations" in new TestSetup(nextUpdatesTileViewModel = viewModelNoUpdates) {
            getElementById("updates-tile").map(_.text()) shouldBe Some("Next updates due")
          }
        }
        "has a link to view and manage updates - Opt Out" in new TestSetup(nextUpdatesTileViewModel = viewModelOptOut) {
          val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/next-updates")
          link.map(_.text) shouldBe Some("View deadlines and manage how you report")
        }

        "has next update and tax return dates when OptInOptOutContentUpdateR17 is enabled and ITSA status is Voluntary with no overdue updates" in new TestSetup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(LocalDate.of(2099, 11, 5)),
            currentDate = LocalDate.of(2025, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Voluntary,
            nextQuarterlyUpdateDueDate = Some(LocalDate.of(2099, 11, 5)),
            nextTaxReturnDueDate = Some(LocalDate.of(2100, 1, 31)))
        ) {
          val tile = getElementById("updates-tile").get
          val paragraphs = tile.select("p.govuk-body")
          val link = tile.select("a.govuk-link").first()

          paragraphs.get(0).text shouldBe "Next update due: 5 November 2099"
          paragraphs.get(1).text shouldBe "Next tax return due: 31 January 2100"
          link.text shouldBe "View deadlines and manage how you report"
          link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/next-updates"
        }

        "has overdue update and tax return when OptInOptOutContentUpdateR17 is enabled and 1 overdue update exists" in new TestSetup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(LocalDate.of(2024, 10, 1)),
            currentDate = LocalDate.of(2025, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Mandated,
            nextQuarterlyUpdateDueDate = Some(LocalDate.of(2024, 10, 1)),
            nextTaxReturnDueDate = Some(LocalDate.of(2025, 1, 31)))

        ) {
          val tile = getElementById("updates-tile").get
          val paragraphs = tile.select("p.govuk-body")
          val tag = tile.select("span.govuk-tag.govuk-tag--red")
          val link = tile.select("a.govuk-link").first()

          tag.text() shouldBe "Overdue"
          paragraphs.get(1).text shouldBe "Next update due: 1 October 2024"
          paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2025"
          link.text shouldBe "View deadlines and manage how you report"
        }

        "has multiple overdue updates and tax return with OptInOptOutContentUpdateR17 enabled" in new TestSetup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(
            LocalDate.of(2024, 5, 5),
            LocalDate.of(2024, 8, 5),
            LocalDate.of(2024, 11, 5)
          ),
            currentDate = LocalDate.of(2025, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Voluntary,
            nextQuarterlyUpdateDueDate = Some(LocalDate.of(2024, 5, 5)),
            nextTaxReturnDueDate = Some(LocalDate.of(2025, 1, 31)))
        ) {
          val tile = getElementById("updates-tile").get
          val paragraphs = tile.select("p.govuk-body")
          val tag = tile.select("span.govuk-tag.govuk-tag--red")
          val link = tile.select("a.govuk-link").first()

          tag.text() shouldBe "3 Overdue updates"
          paragraphs.get(1).text shouldBe "Next update due: 5 May 2024"
          paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2025"
          link.text shouldBe "View deadlines and manage how you report"
        }

        "has only the tax return due when ITSA status is Annual and OptInOptOutContentUpdateR17 is enabled" in new TestSetup(
          nextUpdatesTileViewModel = NextUpdatesTileViewModel(dueDates = Seq(LocalDate.of(2100, 11, 5)),
            currentDate = LocalDate.of(2025, 6, 24),
            isReportingFrequencyEnabled = true,
            showOptInOptOutContentUpdateR17 = true,
            currentYearITSAStatus = ITSAStatus.Annual,
            None,
            nextTaxReturnDueDate = Some(LocalDate.of(2101, 1, 31)))
        ) {
          val tile = getElementById("updates-tile").get
          val paragraphs = tile.select("p.govuk-body")
          val tag = tile.select("span.govuk-tag.govuk-tag--red")

          paragraphs.size() shouldBe 1
          paragraphs.get(0).text shouldBe "Next tax return due: 31 January 2101"
          tag shouldBe empty
        }
      }

      "have a language selection switch" which {

        "displays the correct content" in new TestSetup {
          val langSwitchScript: Option[Element] = getElementById("language-switch")
          langSwitchScript.map(_.select("li:nth-child(1)").text) shouldBe Some("English")
          langSwitchScript.map(_.select("li:nth-child(2)").text) shouldBe Some("Newid yr iaith ir Gymraeg Cymraeg")
        }
      }

      "have a returns tile" which {
        "has a heading" in new TestSetup {
          getElementById("returns-tile").map(_.select("h2").text) shouldBe Some("Returns")
        }
        "has a link to the view payments page" in new TestSetup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(currentTaxYear).url)
          link.map(_.text) shouldBe Some(s"View your client’s current ${currentTaxYear - 1} to $currentTaxYear return")
        }
        "has a link to the update and submit page" in new TestSetup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
          link.map(_.attr("href")) shouldBe Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
          link.map(_.text) shouldBe Some(s"Update and submit your client’s ${currentTaxYear - 1} to $currentTaxYear return")
        }
        "dont have a link to the update and submit page when ITSASubmissionIntegrationEnabled is disabled" in new TestSetup(ITSASubmissionIntegrationEnabled = false) {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
          link.map(_.attr("href")) should not be Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
          link.map(_.text) should not be Some(s"Update and submit your ${currentTaxYear - 1} to ${currentTaxYear} return")
        }
        "has a link to the tax years page" in new TestSetup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").last)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearsController.showAgentTaxYears().url)
          link.map(_.text) shouldBe Some("View all tax years")
        }
      }

      "have a payment history tile" which {
        "has payment and refund history heading when payment history feature switch is enabled" in new TestSetup() {
          getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some("Payment history and refunds")
        }

        "has payment history heading when payment history feature switch is disabled" in new TestSetup(paymentHistoryEnabled = false) {
          getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some("Payment history")
        }

        "has payment and refund history link when CreditsRefundsRepay OFF / PaymentHistoryRefunds ON" in new TestSetup(creditAndRefundEnabled = false, paymentHistoryEnabled = true) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.showAgent().url)
          link.map(_.text) shouldBe Some("Payment and refund history")
        }
        "has payment and credit history link when CreditsRefundsRepay ON / PaymentHistoryRefunds OFF" in new TestSetup(creditAndRefundEnabled = true, paymentHistoryEnabled = false) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.showAgent().url)
          link.map(_.text) shouldBe Some("Payment and credit history")
        }
        "has payment, credit and refund history link when CreditsRefundsRepay ON / PaymentHistoryRefunds ON" in new TestSetup(creditAndRefundEnabled = true, paymentHistoryEnabled = true) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.showAgent().url)
          link.map(_.text) shouldBe Some("Payment, credit and refund history")
        }
        "has payment history link when CreditsRefundsRepay OFF / PaymentHistoryRefunds OFF" in new TestSetup(paymentHistoryEnabled = false, creditAndRefundEnabled = false) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.showAgent().url)
          link.map(_.text) shouldBe Some("Payment history")
        }

        s"has the available credit " in new TestSetup(creditAndRefundEnabled = true) {
          getElementById("available-credit").map(_.text) shouldBe Some("£100.00 is in your account")
        }
      }


      "have a Reporting Obligations tile" when {
        "the reporting obligations page FS is enabled" which {
          "has a heading" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true) {
            getElementById("reporting-obligations-tile").map(_.select("h2").first().text()) shouldBe Some("Your reporting obligations")
          }
          "has text for reporting quarterly(voluntary)" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Voluntary) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to:")
            document.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "use Making Tax Digital for Income Tax submit a tax return"
          }
          "has text for reporting quarterly(mandated)" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Mandated) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to:")
            document.getElementsByClass("govuk-list govuk-list--bullet").text() shouldBe "use Making Tax Digital for Income Tax submit a tax return"
          }
          "has text for reporting annually" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Annual) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to submit a tax return")
          }

          "has a link to the reporting obligations page" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true) {
            getElementById("reporting-obligations-link").map(_.text()) shouldBe Some("View and manage your reporting obligations")
            getElementById("reporting-obligations-link").map(_.attr("href")) shouldBe Some(controllers.routes.ReportingFrequencyPageController.show(true).url)
          }
        }

        "the reporting obligations page FS is disabled" which {
          "does not have the Reporting Obligations tile" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = false) {
            getElementById("reporting-obligations-tile") shouldBe None
          }
        }
      }

      s"have a change client link" in new TestSetup {

        val link: Option[Elements] = getElementById("changeClientLink").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/remove-client-sessions")
        link.map(_.text) shouldBe Some("Change client")
      }
    }

    "the feature switches are disabled" should {
      "not have a your income tax returns tile" in new TestSetup(ITSASubmissionIntegrationEnabled = false) {
        getElementById("manage-income-tax-tile") shouldBe None
      }
      "not have a link to previous payments in the tax years tile" in new TestSetup(ITSASubmissionIntegrationEnabled = false) {
        document.getOptionalSelector("tax-years-tile").flatMap(_.getOptionalSelector("a:nth-of-type(2)")) shouldBe None
      }
      "not have an Income Sources tile" in new TestSetup(incomeSourcesEnabled = false) {
        getElementById("income-sources-tile") shouldBe None
      }
    }

    "the feature switches are enabled" should {
      "not have a link to the saViewLandPTile when isAgent" in new TestSetup(ITSASubmissionIntegrationEnabled = true) {
        val link: Option[Elements] = getElementById("saViewLandPTile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe None
      }

      "dont display the saViewLandPTile when isAgent is true" in new TestSetup(ITSASubmissionIntegrationEnabled = true) {
        getElementById("saViewLandPTile") shouldBe None
      }

      "not have a link to the saViewLandPTile when isAgent and UTR is present" in new TestSetup(ITSASubmissionIntegrationEnabled = true) {
        val link: Option[Elements] = getElementById("saViewLandPTile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe None
      }

      "dont display the saViewLandPTile when isAgent is true and UTR is present" in new TestSetup(ITSASubmissionIntegrationEnabled = true) {
        getElementById("saViewLandPTile") shouldBe None
      }
      "have an Income Sources tile" which {
        "has a heading" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some("Income Sources")
        }
        "has a link to AddIncomeSourceController.showAgent()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some("Add a new sole trader or property income source")
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
        }
        "has a link to ManageIncomeSourceController.showAgent()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").text()) shouldBe Some("View and manage income sources")
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").attr("href")) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url)
        }
      }
      "have a Your Businesses tile" when {
        "the new income sources journey FS is enabled" which {
          "has a heading" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
            getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some("Your businesses")
          }
          "has a link to ManageYourBusinessController.show()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some("Add, manage or cease a business or income source")
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)
          }
        }
      }

      "have a Penalties and Appeals tile" when {
        "PenaltiesAndAppeals FS is enabled" which {
          "has a heading" in new TestSetup(submissionFrequency = "Annual", penaltyPoints = 2) {
            getElementById("penalties-and-appeals-tile").map(_.select("h2").first().text()) shouldBe Some("Penalties and appeals")
          }
          "has a link to Self Assessment Penalties and Appeals page" in new TestSetup(penaltiesAndAppealsIsEnabled = true) {
            getElementById("sa-penalties-and-appeals-link").map(_.text()) shouldBe Some("Check Self Assessment penalties and appeals")
            getElementById("sa-penalties-and-appeals-link").map(_.attr("href")) shouldBe Some(appConfig.incomeTaxPenaltiesFrontend)
          }
          "has a two-points penalty tag" in new TestSetup(submissionFrequency = "Annual", penaltyPoints = 3) {
            getElementById("penalty-points-tag").map(_.text()) shouldBe Some("2 Penalty points")
          }
          "has a four-points penalty tag" in new TestSetup(submissionFrequency = "Quarterly", penaltyPoints = 4) {
            getElementById("penalty-points-tag").map(_.text()) shouldBe Some("4 Penalty points")
          }
          "has no penalty tag if 2 points reached but User is reporting Quarterly" in new TestSetup(submissionFrequency = "Quarterly", penaltyPoints = 2) {
            getElementById("penalty-points-tag").map(_.text()).isDefined shouldBe false
            getElementById("penalty-points-tag").map(_.text()).isDefined shouldBe false
          }
          "has no penalty tag if user has only 1 point" in new TestSetup(penaltyPoints = 1) {
            getElementById("penalty-points-tag").map(_.text()).isDefined shouldBe false
            getElementById("penalty-points-tag").map(_.text()).isDefined shouldBe false
          }
        }
      }
    }
  }

}
