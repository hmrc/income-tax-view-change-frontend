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
import config.FrontendAppConfig
import config.featureswitch._
import models.homePage._
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
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
import views.html.agent.{PrimaryAgentHome, SupportingAgentHome}

import java.time.{LocalDate, Month}
import scala.util.Try

class SupportingAgentHomePageViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  lazy val backUrl: String = controllers.agent.routes.ConfirmClientUTRController.show.url

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = fixedDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, Month.APRIL, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val testMtdItUserNotMigrated: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testNino, testMtditid, None, Nil, Nil),
    btaNavPartial = None,
    Some(testSaUtr),
    Some(testCredId),
    Some(Agent),
    Some(testArn),
    Some(testClientName)
  )(FakeRequest())

  val testMtdItUserMigrated: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testNino, testMtditid, Some("2018"), Nil, Nil),
    btaNavPartial = None,
    Some(testSaUtr),
    Some(testCredId),
    Some(Agent),
    Some(testArn),
    None
  )(FakeRequest())

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
                  user: MtdItUser[_] = testMtdItUserNotMigrated
                 ) {

    val agentHome: SupportingAgentHome = app.injector.instanceOf[SupportingAgentHome]

    val yourBusinessesTileViewModel = YourBusinessesTileViewModel(displayCeaseAnIncome, incomeSourcesEnabled, incomeSourcesNewJourneyEnabled)

    val view: HtmlFormat.Appendable = agentHome(
      yourBusinessesTileViewModel,
      nextUpdatesTileViewModel
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

      s"have the page heading ${messages("home.agent.heading")}" in new TestSetup(user = testMtdItUserMigrated) {
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
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"OVERDUE 1 January $year2018")
          }
          "is not overdue" in new TestSetup(nextPaymentDueDate = Some(nextUpdateDue)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"1 January 2100")
          }
          "is a count of overdue updates" in new TestSetup(nextUpdatesTileViewModel = viewModelTwoOverdue) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 OVERDUE UPDATES")
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
          link.map(_.text) shouldBe Some(messages("home.updates.view.opt-out"))
        }
      }

      "have a language selection switch" which {
        "displays the current language" in new TestSetup {
          val langSwitch: Option[Element] = getElementById("lang-switch-en")
          langSwitch.map(_.select("li:nth-child(1)").text) shouldBe Some(messages("language-switcher.english"))
        }

        "changes with JS ENABLED" in new TestSetup {
          val langSwitchScript: Option[Element] = getElementById("lang-switch-en-js")
          langSwitchScript.toString.contains("/report-quarterly/income-and-expenses/view/switch-to-welsh") shouldBe true
          langSwitchScript.toString.contains(messages("language-switcher.welsh")) shouldBe true
        }

        "changes with JS DISABLED" in new TestSetup {
          val langSwitchNoScript: Option[Element] = getElementById("lang-switch-en-no-js")
          langSwitchNoScript.map(_.select("a").attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/switch-to-welsh")
          langSwitchNoScript.map(_.select("a span:nth-child(2)").text) shouldBe Some(messages("language-switcher.welsh"))
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
          "has a link to AddIncomeSourceController.show()" in new TestSetup(user = testMtdItUserMigrated, incomeSourcesEnabled = true, incomeSourcesNewJourneyEnabled = true) {
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some(messages("home.incomeSources.newJourney.view"))
            getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(controllers.manageBusinesses.routes.ManageYourBusinessesController.show(true).url)
          }
        }
      }
    }
  }

}
