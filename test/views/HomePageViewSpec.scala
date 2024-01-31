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
import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import models.homePage.PaymentCreditAndRefundHistoryTileViewModel
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.nextUpdates.NextUpdatesTileViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.Home

import java.time.LocalDate
import scala.util.Random


class HomePageViewSpec extends TestSupport with FeatureSwitching {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  def testMtdItUser(saUtr: Option[String] = Some("testUtr")): MtdItUser[_] = {
    val yearOfMigrationOpt = if (Random.nextBoolean()) {
      Some("2018")
    } else {
      None
    }
    MtdItUser(
      testMtditid,
      testNino,
      Some(testRetrievedUserName),
      IncomeSourceDetailsModel(testNino, mtdbsa = testMtditid, yearOfMigration = yearOfMigrationOpt, businesses = Nil, properties = Nil),
      testNavHtml,
      saUtr,
      Some("testCredId"),
      Some(Individual),
      None
    )(FakeRequest())
  }

  def testMtdItUserMigrated(saUtr: Option[String] = Some("testUtr")): MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testNino, mtdbsa = testMtditid, yearOfMigration = Some("2018"), businesses = Nil, properties = Nil),
    testNavHtml,
    saUtr,
    Some("testCredId"),
    Some(Individual),
    None
  )(FakeRequest())

  def testMtdItUserNotMigrated(saUtr: Option[String] = Some("testUtr")): MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testNino, mtdbsa = testMtditid, yearOfMigration = None, businesses = Nil, properties = Nil),
    testNavHtml,
    saUtr,
    Some("testCredId"),
    Some(Individual),
    None
  )(FakeRequest())

  def viewUpdateAndSubmitLinkWithDateRange(taxYear: Int): String = s"${messages("home.your-returns.updatesLink", taxYear - 1, taxYear)}"

  val updateDate: LocalDate = LocalDate.of(2100, 1, 1)
  val updateDateLongDate = "1 January 2100"
  val multipleOverdueUpdates = s"${messages("home.updates.overdue.updates", "3")}"
  val nextPaymentDueDate: LocalDate = LocalDate.of(2019, 1, 31)
  val paymentDateLongDate = "31 January 2019"
  val multipleOverduePayments = s"${messages("home.updates.overdue.payments", "3")}"
  val overdueMessage = s"! Warning ${messages("home.overdue.message.dunningLock.false")}"
  val overdueMessageForDunningLocks = s"! Warning ${messages("home.overdue.message.dunningLock.true")}"
  val currentDate = dateService.getCurrentDate(isEnabled(TimeMachineAddYear))
  private val viewModelFuture: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate)
  private val viewModelOneOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1)), currentDate)
  private val viewModelThreeOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 2, 1), LocalDate.of(2018, 3, 1)), currentDate)
  private val viewModelNoUpdates: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(), currentDate)


  class Setup(paymentDueDate: Option[LocalDate] = Some(nextPaymentDueDate), overDuePaymentsCount: Option[Int] = Some(0),
              nextUpdatesTileViewModel: NextUpdatesTileViewModel = viewModelFuture, utr: Option[String] = Some("1234567890"), paymentHistoryEnabled: Boolean = true, ITSASubmissionIntegrationEnabled: Boolean = true,
              user: MtdItUser[_] = testMtdItUser(), dunningLockExists: Boolean = false, isAgent: Boolean = false, creditAndRefundEnabled: Boolean = false, displayCeaseAnIncome: Boolean = false,
              incomeSourcesEnabled: Boolean = false) {

    val home: Home = app.injector.instanceOf[Home]
    lazy val page: HtmlFormat.Appendable = home(
      availableCredit = Some(100),
      nextPaymentDueDate = paymentDueDate,
      overDuePaymentsCount = overDuePaymentsCount,
      nextUpdatesTileViewModel = nextUpdatesTileViewModel,
      paymentCreditAndRefundHistoryTileViewModel = PaymentCreditAndRefundHistoryTileViewModel(creditAndRefundEnabled, paymentHistoryEnabled),
      Some("1234567890"),
      ITSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled,
      dunningLockExists = dunningLockExists,
      displayCeaseAnIncome = displayCeaseAnIncome,
      incomeSourcesEnabled = incomeSourcesEnabled,
      currentTaxYear = currentTaxYear,
      isAgent = isAgent,
      creditAndRefundEnabled = creditAndRefundEnabled,
      paymentHistoryEnabled = paymentHistoryEnabled,
      isUserMigrated = user.incomeSources.yearOfMigration.isDefined
    )(FakeRequest(), implicitly, user, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

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
      document.title() shouldBe s"${messages("htmlTitle", messages("home.heading"))}"
    }

    s"have the page heading '${messages("home.heading")}'" in new Setup {
      getTextOfElementById("income-tax-heading") shouldBe Some(messages("home.heading"))
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
      getTextOfElementById("utr-reference-heading") shouldBe Some(messages("home.unique.taxpayer.reference", "testUtr"))
    }

    "not have the users UTR when it is absent in user profile" in new Setup(user = testMtdItUser(saUtr = None)) {
      getElementById("utr-reference-heading") shouldBe None
    }

    "have a language selection switch" which {
      "displays the current language" in new Setup {
        val langSwitch: Option[Element] = getElementById("lang-switch-en")
        langSwitch.map(_.select("li:nth-child(1)").text) shouldBe Some(messages("language-switcher.english"))
      }

      "changes with JS ENABLED" in new Setup {
        val langSwitchScript: Option[Element] = getElementById("lang-switch-en-js")
        langSwitchScript.toString.contains("/report-quarterly/income-and-expenses/view/switch-to-welsh") shouldBe true
        langSwitchScript.toString.contains(messages("language-switcher.welsh")) shouldBe true
      }

      "changes with JS DISABLED" in new Setup {
        val langSwitchNoScript: Option[Element] = getElementById("lang-switch-en-no-js")
        langSwitchNoScript.map(_.select("a").attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/switch-to-welsh")
        langSwitchNoScript.map(_.select("a span:nth-child(2)").text) shouldBe Some(messages("language-switcher.welsh"))
      }
    }

    "have an updates tile" which {
      "has a heading" in new Setup {
        getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(messages("home.updates.heading"))
      }
      "has the date of the next update due" in new Setup {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(updateDateLongDate)
      }
      "display an overdue tag when a single update is overdue" in new Setup(nextUpdatesTileViewModel = viewModelOneOverdue) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("OVERDUE " + "1 January 2018")
      }
      "has the correct number of overdue updates when three updates are overdue" in new Setup(nextUpdatesTileViewModel = viewModelThreeOverdue) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverdueUpdates)
      }
      "has a link to view updates" in new Setup {
        val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.NextUpdatesController.getNextUpdates().url)
        link.map(_.text) shouldBe Some(messages("home.updates.view"))
      }

      "is empty except for the title" when {
        "user has no open obligations" in new Setup(nextUpdatesTileViewModel = viewModelNoUpdates) {
          getElementById("updates-tile").map(_.text()) shouldBe Some(messages("home.updates.heading"))
        }
      }
    }

    "have a payments due tile" which {
      "has a heading" in new Setup {
        getElementById("payments-tile").map(_.select("h2").text) shouldBe Some(messages("home.payments.heading"))
      }
      "has the date of the next update due" in new Setup {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(paymentDateLongDate)
      }
      s"has the available credit " in new Setup(creditAndRefundEnabled = true) {
        getTextOfElementById("available-credit") shouldBe Some("£100.00 is in your account")
      }
      "don't display an overdue warning message when no payment is overdue" in new Setup(overDuePaymentsCount = Some(0)) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue" in new Setup(overDuePaymentsCount = Some(1)) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessage)
      }

      "as an agent don't display an overdue warning message when no payment is overdue" in new Setup(overDuePaymentsCount = Some(0), isAgent = true) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an dunning lock overdue warning message when a payment is overdue" in new Setup(overDuePaymentsCount = Some(1), dunningLockExists = true) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageForDunningLocks)
      }

      "display an overdue tag when a single update is overdue" in new Setup(overDuePaymentsCount = Some(1)) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("OVERDUE " + paymentDateLongDate)
      }

      "has the correct number of overdue updates when three updates are overdue" in new Setup(overDuePaymentsCount = Some(3)) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverduePayments)
      }
      "has a link to view payments" in new Setup {
        val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.WhatYouOweController.show().url)
        link.map(_.text) shouldBe Some(messages("home.payments.view"))
      }
    }

    "have a returns tile" which {
      "has a heading" in new Setup {
        getElementById("returns-tile").map(_.select("h2").text) shouldBe Some(messages("home.tax-years.heading"))
      }
      "has a link to the view payments page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").first)
        link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(currentTaxYear).url)
        link.map(_.text) shouldBe Some(s"${messages("home.returns.viewLink", s"${currentTaxYear - 1}", s"$currentTaxYear")}")
      }
      "has a link to the update and submit page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) shouldBe Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
        link.map(_.text) shouldBe Some(s"${messages("home.your-returns.updatesLink", s"${currentTaxYear - 1}", s"$currentTaxYear")}")
      }
      "dont have a link to the update and submit page when ITSASubmissionIntegrationEnabled is disabled" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) should not be Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
        link.map(_.text) should not be Some(s"${messages("home.your-returns.updatesLink", s"${currentTaxYear - 1}", s"$currentTaxYear")}")
      }
      "has a link to the tax years page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").last)
        link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearsController.showTaxYears().url)
        link.map(_.text) shouldBe Some(messages("home.tax-years.view"))
      }
    }

    "have a payment history tile" which {

      "has a payment and history refunds heading when payment history feature switch is enabled" in new Setup {
        getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some(messages("home.paymentHistoryRefund.heading"))
      }
      "have a payment history tile" which {
        "has a payment history heading when payment history feature switch is disabled" in new Setup(paymentHistoryEnabled = false) {
          getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some(messages("home.paymentHistory.heading"))
        }
      }
      "has a link to the payment and refund history page" which {
        "has a payment and refund history link when payment history feature switch is enabled" in new Setup() {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.show().url)
          link.map(_.text) shouldBe Some(messages("home.paymentHistoryRefund.view"))
        }
      }
      "has a link to the payment history page" which {
        "has a payment history link when payment history feature switch is disabled" in new Setup(paymentHistoryEnabled = false) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.show().url)
          link.map(_.text) shouldBe Some(messages("home.paymentHistory.view"))
        }
      }
      "has an link to the 'How to claim a refund' for not migrated user" in new Setup(user = testMtdItUserNotMigrated()) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
        // next line would change as part of MISUV-3710 implementation
        link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.show().url)
        link.map(_.text) shouldBe Some(messages("home.paymentHistoryRefund.view"))
      }

    }

    "have an Income Sources tile with feature switch enabled" which {
      "has a heading" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true) {
        getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some(messages("home.incomeSources.heading"))
      }
      "has a link to AddIncomeSourceController.show()" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true) {
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some(messages("home.incomeSources.addIncomeSource.view"))
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
      }
      "has a link to ManageIncomeSourceController.show()" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = true) {
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").text()) shouldBe Some(messages("home.incomeSources.manageIncomeSource.view"))
        getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").attr("href")) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false).url)
      }
    }
    "not have an Income Sources tile" when {
      "feature switch is disabled" in new Setup(user = testMtdItUserMigrated(), incomeSourcesEnabled = false) {
        getElementById("income-sources-tile") shouldBe None
      }
    }

    "show the 'Claim refund' link for migrated user" when {
      "the claim a refund feature switch is on" in new Setup(user = testMtdItUserMigrated(), creditAndRefundEnabled = true) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").last())
        link.map(_.attr("href")) shouldBe Some(controllers.routes.CreditAndRefundController.show().url)
        link.map(_.text) shouldBe Some(messages("home.credAndRefund.view"))
      }
    }

    "show the 'How to claim a refund' link for not migrated user" when {
      "the claim a refund feature switch is on" in new Setup(user = testMtdItUserNotMigrated(), creditAndRefundEnabled = true) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").last())
        link.map(_.attr("href")) shouldBe Some(controllers.routes.NotMigratedUserController.show().url)
        link.map(_.text) shouldBe Some(messages("notmigrated.user.heading"))
      }
    }

    "show the 'How to claim a refund' link for not migrated agent" when {
      "the claim a refund feature switch is on" in new Setup(user = testMtdItUserNotMigrated(),
        creditAndRefundEnabled = true, isAgent = true) {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").last())
        link.map(_.attr("href")) shouldBe Some(controllers.routes.NotMigratedUserController.showAgent().url)
        link.map(_.text) shouldBe Some(messages("notmigrated.user.heading"))
      }
    }
  }
}
