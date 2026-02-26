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
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import views.html.agent.SupportingAgentHomeView

import java.time.{LocalDate, Month}
import scala.util.Try

class SupportingAgentHomePageViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  lazy val backUrl: String = controllers.agent.routes.ConfirmClientUTRController.show().url

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = fixedDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, Month.APRIL, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val testMtdItUserNotMigrated: MtdItUser[_] = defaultMTDITUser(Some(Agent),
    IncomeSourceDetailsModel(testNino, testMtditid, None, Nil, Nil), isSupportingAgent = true
  )

  val testMtdItUserMigrated: MtdItUser[_] = defaultMTDITUser(Some(Agent),
    IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, Nil), isSupportingAgent = true)

  val testMtdItUserNoClientName: MtdItUser[_] = getMinimalMTDITUser(Some(Agent), IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, Nil), isSupportingAgent = true)


  val year2018: Int = 2018
  val year2019: Int = 2019

  val nextUpdateDue: LocalDate = LocalDate.of(2100, Month.JANUARY, 1)

  val nextPaymentDue: LocalDate = LocalDate.of(year2019, Month.JANUARY, 31)

  val currentDate: LocalDate = dateService.getCurrentDate
  private val viewModelFuture: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, isReportingFrequencyEnabled = false, showOptInOptOutContentUpdateR17 = false, ITSAStatus.NoStatus, None, None)
  private val viewModelOneOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1)), currentDate, isReportingFrequencyEnabled = false, showOptInOptOutContentUpdateR17 = false, ITSAStatus.NoStatus, None, None)
  private val viewModelTwoOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 2, 1)), currentDate, isReportingFrequencyEnabled = false, showOptInOptOutContentUpdateR17 = false, ITSAStatus.NoStatus, None, None)
  private val viewModelNoUpdates: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(), currentDate, isReportingFrequencyEnabled = false, showOptInOptOutContentUpdateR17 = false, ITSAStatus.NoStatus, None, None)
  private val viewModelOptOut: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, isReportingFrequencyEnabled = true, showOptInOptOutContentUpdateR17 = false, ITSAStatus.NoStatus, None, None)

  class TestSetup(nextPaymentDueDate: Option[LocalDate] = Some(nextPaymentDue),
                  nextUpdatesTileViewModel: NextUpdatesTileViewModel = viewModelFuture,
                  displayCeaseAnIncome: Boolean = false,
                  reportingFrequencyEnabled: Boolean = false,
                  currentITSAStatus: ITSAStatus = ITSAStatus.Voluntary,
                  user: MtdItUser[_] = testMtdItUserNotMigrated
                 ) {

    val agentHome: SupportingAgentHomeView = app.injector.instanceOf[SupportingAgentHomeView]

    val yourBusinessesTileViewModel: YourBusinessesTileViewModel = YourBusinessesTileViewModel(displayCeaseAnIncome)

    val yourReportingObligationsTileViewModel: YourReportingObligationsTileViewModel = YourReportingObligationsTileViewModel(TaxYear(currentTaxYear, currentTaxYear + 1), reportingFrequencyEnabled, currentITSAStatus)

    val view: HtmlFormat.Appendable = agentHome(
      yourBusinessesTileViewModel,
      nextUpdatesTileViewModel,
      yourReportingObligationsTileViewModel
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
        document.title() shouldBe messages("htmlTitle.agent", messages("home.agent.heading"))
      }

      s"have the page caption You are signed in as a supporting agent" in new TestSetup {
        document.getElementsByClass("govuk-caption-xl").text() shouldBe "You are signed in as a supporting agent"
      }

      s"have the page heading ${messages("home.agent.headingWithClientName", testClientNameString)}" in new TestSetup {
        document.select("h1").text() shouldBe messages("home.agent.headingWithClientName", testClientNameString)
      }

      s"have the page heading ${messages("home.agent.heading")}" in new TestSetup(user = testMtdItUserNoClientName) {
        document.select("h1").text() shouldBe messages("home.agent.heading")
      }

      s"have the hint with the clients utr '$testSaUtr' " in new TestSetup {
        getHintNth() shouldBe Some(s"Unique Taxpayer Reference (UTR): $testSaUtr")
      }

      "not have an next payment due tile" in new TestSetup {
        getElementById("payments-tile") shouldBe None
      }

      "have a your submission deadlines tile" which {
        "has a heading" in new TestSetup {
          getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(messages("home.updates.heading"))
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
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/submission-deadlines")
          link.map(_.text) shouldBe Some(messages("home.updates.view"))
        }
        "is empty except for the title" when {
          "user has no open obligations" in new TestSetup(nextUpdatesTileViewModel = viewModelNoUpdates) {
            getElementById("updates-tile").map(_.text()) shouldBe Some("Your submission deadlines View update deadlines")
          }
        }
        "has a link to view and manage updates - Opt Out" in new TestSetup(nextUpdatesTileViewModel = viewModelOptOut) {
          val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/submission-deadlines")
          link.map(_.text) shouldBe Some(messages("home.updates.view.reportingFrequency"))
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
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")
          val link: Element = tile.select("a.govuk-link").first()

          paragraphs.get(0).text shouldBe "Next update due: 5 November 2099"
          paragraphs.get(1).text shouldBe "Next tax return due: 31 January 2100"
          link.text shouldBe "View your deadlines"
          link.attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/submission-deadlines"
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
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")
          val tag: Elements = tile.select("span.govuk-tag.govuk-tag--red")
          val link: Element = tile.select("a.govuk-link").first()

          tag.text() shouldBe "Overdue"
          paragraphs.get(1).text shouldBe "Next update due: 1 October 2024"
          paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2025"
          link.text shouldBe "View your deadlines"
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
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")
          val tag: Elements = tile.select("span.govuk-tag.govuk-tag--red")
          val link: Element = tile.select("a.govuk-link").first()

          tag.text() shouldBe "3 Overdue updates"
          paragraphs.get(1).text shouldBe "Next update due: 5 May 2024"
          paragraphs.get(2).text shouldBe "Next tax return due: 31 January 2025"
          link.text shouldBe "View your deadlines"
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
          val tile: Element = getElementById("updates-tile").get
          val paragraphs: Elements = tile.select("p.govuk-body")
          val tag: Elements = tile.select("span.govuk-tag.govuk-tag--red")

          paragraphs.size() shouldBe 1
          paragraphs.get(0).text shouldBe "Next tax return due: 31 January 2101"
          tag shouldBe empty
        }
      }

      "have a language selection switch" which {

        "displays the correct content" in new TestSetup {
          val langSwitchScript: Option[Element] = getElementById("language-switch")
          langSwitchScript.map(_.select("li:nth-child(1)").text) shouldBe Some("English")
          langSwitchScript.map(_.select("li:nth-child(2)").text) shouldBe Some("Newid yr iaith i’r Gymraeg Cymraeg")
        }
      }

      "not have a returns tile" in new TestSetup {
        getElementById("returns-tile") shouldBe None
      }

      "not have a payment history tile" in new TestSetup() {
        getElementById("payment-history-tile") shouldBe None
      }

      s"have a change client link" in new TestSetup {

        val link: Option[Elements] = getElementById("changeClientLink").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/remove-client-sessions")
        link.map(_.text) shouldBe Some(messages("home.agent.changeClientLink"))
      }

      s"have a read more about differences between agents and supporting agents link" in new TestSetup {

        val link: Option[Elements] = getElementById("read-more-about-differences-link").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("https://www.gov.uk/guidance/choose-agents-for-making-tax-digital-for-income-tax")
        link.map(_.text) shouldBe Some("Read more about the difference between main and supporting agents on GOV.UK (opens in new tab).")
      }
    }

    "using the manage businesses journey" should {
      "have a Your Businesses tile" when {
        "using the manage businesses journey" which {
          "has a heading" in new TestSetup(user = testMtdItUserMigrated) {
            getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some(messages("home.incomeSources.newJourneyHeading"))
          }
          "has a link to ManageYourBusinessController.show()" in new TestSetup(user = testMtdItUserMigrated) {
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some(messages("home.incomeSources.newJourney.view"))
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)
          }
        }
      }

      "have a Reporting Obligations tile" when {
        "the reporting obligations page FS is enabled" which {
          "has a heading" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true) {
            getElementById("reporting-obligations-tile").map(_.select("h2").first().text()) shouldBe Some("Your reporting obligations")
          }
          "has text for reporting quarterly(voluntary)" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Voluntary) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to:")
            document.getElementsByClass("govuk-list govuk-list--bullet").first().text() shouldBe "use Making Tax Digital for Income Tax submit a tax return"
          }
          "has text for reporting quarterly(mandated)" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Mandated) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to:")
            document.getElementsByClass("govuk-list govuk-list--bullet").first().text() shouldBe "use Making Tax Digital for Income Tax submit a tax return"
          }
          "has text for reporting annually" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Annual) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"For the $currentTaxYear to ${currentTaxYear + 1} tax year you need to submit a tax return")
          }

          "has a link to the reporting obligations page" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true) {
            getElementById("reporting-obligations-link").map(_.text()) shouldBe Some("View and manage your reporting obligations")
            getElementById("reporting-obligations-link").map(_.attr("href")) shouldBe Some(controllers.reportingObligations.routes.ReportingFrequencyPageController.show(true).url)
          }
        }

        "the reporting frequency page FS is disabled" which {
          "does not have the Reporting Obligations tile" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = false) {
            getElementById("reporting-obligations-tile") shouldBe None
          }
        }
      }
    }
  }
}