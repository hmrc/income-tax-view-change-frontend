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
import views.html.agent.SupportingAgentHome

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

  val testMtdItUserNoClientName = getMinimalMTDITUser(Some(Agent), IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, Nil), isSupportingAgent = true)


  val year2018: Int = 2018
  val year2019: Int = 2019

  val nextUpdateDue: LocalDate = LocalDate.of(2100, Month.JANUARY, 1)

  val nextPaymentDue: LocalDate = LocalDate.of(year2019, Month.JANUARY, 31)

  val currentDate = dateService.getCurrentDate
  private val viewModelFuture: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, false)
  private val viewModelOneOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1)), currentDate, false)
  private val viewModelTwoOverdue: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2018, 1, 1),
    LocalDate.of(2018, 2, 1)), currentDate, false)
  private val viewModelNoUpdates: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(), currentDate, false)
  private val viewModelOptOut: NextUpdatesTileViewModel = NextUpdatesTileViewModel(Seq(LocalDate.of(2100, 1, 1)), currentDate, true)

  class TestSetup(nextPaymentDueDate: Option[LocalDate] = Some(nextPaymentDue),
                  nextUpdatesTileViewModel: NextUpdatesTileViewModel = viewModelFuture,
                  displayCeaseAnIncome: Boolean = false,
                  incomeSourcesEnabled: Boolean = false,
                  incomeSourcesNewJourneyEnabled: Boolean = false,
                  reportingFrequencyEnabled: Boolean = false,
                  currentITSAStatus: ITSAStatus = ITSAStatus.Voluntary,
                  user: MtdItUser[_] = testMtdItUserNotMigrated
                 ) {

    val agentHome: SupportingAgentHome = app.injector.instanceOf[SupportingAgentHome]

    val yourBusinessesTileViewModel = YourBusinessesTileViewModel(displayCeaseAnIncome, incomeSourcesEnabled, incomeSourcesNewJourneyEnabled)

    val accountSettingsTileViewModel = AccountSettingsTileViewModel(TaxYear(currentTaxYear, currentTaxYear + 1), reportingFrequencyEnabled, currentITSAStatus)

    val view: HtmlFormat.Appendable = agentHome(
      yourBusinessesTileViewModel,
      nextUpdatesTileViewModel,
      accountSettingsTileViewModel
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

      "have an next updates due tile" which {
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
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/next-updates")
          link.map(_.text) shouldBe Some(messages("home.updates.view"))
        }
        "is empty except for the title" when {
          "user has no open obligations" in new TestSetup(nextUpdatesTileViewModel = viewModelNoUpdates) {
            getElementById("updates-tile").map(_.text()) shouldBe Some(messages("home.updates.heading"))
          }
        }
        "has a link to view and manage updates - Opt Out" in new TestSetup(nextUpdatesTileViewModel = viewModelOptOut) {
          val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/next-updates")
          link.map(_.text) shouldBe Some(messages("home.updates.view.reportingFrequency"))
        }
      }

      "have a language selection switch" which {

        "displays the correct content" in new TestSetup {
          val langSwitchScript: Option[Element] = getElementById("language-switch")
          langSwitchScript.map(_.select("li:nth-child(1)").text) shouldBe Some("English")
          langSwitchScript.map(_.select("li:nth-child(2)").text) shouldBe Some("Newid yr iaith ir Gymraeg Cymraeg")
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

    "the feature switches are disabled" should {
      "not have an Income Sources tile" in new TestSetup(incomeSourcesEnabled = false) {
        getElementById("income-sources-tile") shouldBe None
      }
    }

    "the feature switches are enabled" should {
      "have an Income Sources tile" which {
        "has a heading" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some(messages("home.incomeSources.heading"))
        }
        "has a link to AddIncomeSourceController.showAgent()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some(messages("home.incomeSources.addIncomeSource.view"))
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
        }
        "has a link to ManageIncomeSourceController.showAgent()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").text()) shouldBe Some(messages("home.incomeSources.manageIncomeSource.view"))
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").attr("href")) shouldBe Some(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url)
        }
      }
      "have a Your Businesses tile" when {
        "the new income sources journey FS is enabled" which {
          "has a heading" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
            getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some(messages("home.incomeSources.newJourneyHeading"))
          }
          "has a link to ManageYourBusinessController.show()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some(messages("home.incomeSources.newJourney.view"))
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url)
          }
        }
      }

      "have an Account Settings tile" when {
        "the reporting frequency page FS is enabled" which {
          "has a heading" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true) {
            getElementById("account-settings-tile").map(_.select("h2").first().text()) shouldBe Some("Your account settings")
          }
          "has text for reporting quarterly(voluntary)" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Voluntary) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"Reporting quarterly for $currentTaxYear to ${currentTaxYear + 1} tax year")
          }
          "has text for reporting quarterly(mandated)" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Mandated) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"Reporting quarterly for $currentTaxYear to ${currentTaxYear + 1} tax year")
          }
          "has text for reporting annually" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true, currentITSAStatus = ITSAStatus.Annual) {
            getElementById("current-itsa-status").map(_.text()) shouldBe Some(s"Reporting annually for $currentTaxYear to ${currentTaxYear + 1} tax year")
          }

          "has a link to the reporting frequency page" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = true) {
            getElementById("reporting-frequency-link").map(_.text()) shouldBe Some("Manage your reporting frequency")
            getElementById("reporting-frequency-link").map(_.attr("href")) shouldBe Some(controllers.routes.ReportingFrequencyPageController.show(true).url)
          }
        }

        "the reporting frequency page FS is disabled" which {
          "does not have the Account Settings tile" in new TestSetup(user = testMtdItUserMigrated, reportingFrequencyEnabled = false) {
            getElementById("account-settings-tile") shouldBe None
          }
        }
      }

    }
  }

}
