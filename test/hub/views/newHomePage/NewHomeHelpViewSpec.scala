/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.views.newHomePage

import common.auth.actions.AuthActionsTestData.getMtdItUser
import common.auth.MtdItUser
import common.config.featureswitch.FeatureSwitching
import common.testUtils.{TestSupport, ViewSpec}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.http.HeaderCarrier
import hub.views.html.newHomePage.NewHomeHelpView

class NewHomeHelpViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  private val newHomeHelpView: NewHomeHelpView = app.injector.instanceOf[NewHomeHelpView]

  class TestSetup(
                   origin: Option[String] = None,
                   isAgent: Boolean = false,
                   isSupportingAgent: Boolean = false,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   welshLang: Boolean = false,
                   isRecentActivityEnabled: Boolean = false
                 ) {

    implicit val hc: HeaderCarrier = HeaderCarrier()
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val testMessages: Messages =
      if (welshLang) {
        app.injector.instanceOf[MessagesApi]
          .preferred(FakeRequest().withHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy"))
      } else {
        messages
      }

    implicit val testUser: MtdItUser[_] =
      if (isSupportingAgent) getMtdItUser(Agent, isSupportingAgent = true)(request)
      else if (isAgent) getMtdItUser(Agent)(request)
      else getMtdItUser(Individual)(request)

    lazy val page: HtmlFormat.Appendable =
      newHomeHelpView(
        origin,
        yourTasksUrl,
        recentActivityUrl,
        overViewUrl,
        helpUrl,
        isRecentActivityEnabled
      )(testMessages, request, testUser)

    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val cards: Elements = document.select(".govuk-summary-card-no-border")
  }

  "New Home Help page" should {

    "display the correct heading" in new TestSetup() {
      document.getElementById("income-tax-heading").text() shouldBe "Self Assessment"
    }

    "display the correct service navigation section" when {
      "Recent Activity feature switch is ENABLED" in new TestSetup(isRecentActivityEnabled = true) {
        document.getElementsByClass("govuk-service-navigation__item--active").eq(0).text() shouldBe "Help"
        document.getElementsByClass("govuk-service-navigation__item").eq(0).text() shouldBe "Your tasks"
        document.getElementsByClass("govuk-service-navigation__item").eq(1).text() shouldBe "Recent activity"
        document.getElementsByClass("govuk-service-navigation__item").eq(2).text() shouldBe "Overview"
      }
      "Recent Activity feature switch is DISABLED" in new TestSetup(isRecentActivityEnabled = false) {
        document.getElementsByClass("govuk-service-navigation__item--active").eq(0).text() shouldBe "Help"
        document.getElementsByClass("govuk-service-navigation__item").eq(0).text() shouldBe "Your tasks"
        document.getElementsByClass("govuk-service-navigation__item").eq(1).text() shouldBe "Overview"
      }
    }

    "display the section heading and open-in-new-tab message" in new TestSetup() {
      document.select("h2.govuk-heading-m").first().text() shouldBe messages("new.home.help.heading")
      document.select("p.govuk-body").first().text() shouldBe messages("new.home.help.openInNewTab")
    }

    "display 6 help cards for an individual user" in new TestSetup(isAgent = false) {
      cards.size() shouldBe 6

      cards.get(0).text() should include(messages("home.help.link.makingTaxDigital.incomeTax"))
      cards.get(1).text() should include(messages("home.help.link.makingTaxDigital.changeCircumstances"))
      cards.get(2).text() should include(messages("home.help.link.makingTaxDigital.findSoftware"))
      cards.get(3).text() should include(messages("home.help.link.makingTaxDigital.findPenaltyInfo"))
      cards.get(4).text() should include(messages("home.help.link.selfAssessment.payTaxBill"))
      cards.get(5).text() should include(messages("home.help.link.taxSupport.getHelp"))
    }

    "display only 4 help cards for a primary agent user" in new TestSetup(isAgent = true) {
      cards.size() shouldBe 4

      cards.get(0).text() should include(messages("home.help.link.makingTaxDigital.incomeTax"))
      cards.get(1).text() should include(messages("home.help.link.makingTaxDigital.changeCircumstances"))
      cards.get(2).text() should include(messages("home.help.link.makingTaxDigital.findSoftware"))
      cards.get(3).text() should include(messages("home.help.link.makingTaxDigital.findPenaltyInfo"))

      document.text() should not include messages("home.help.link.selfAssessment.payTaxBill")
      document.text() should not include messages("home.help.link.taxSupport.getHelp")
    }

    "display only 2 help cards for a supporting agent user" in new TestSetup(isSupportingAgent = true) {
      cards.size() shouldBe 2

      cards.get(0).text() should include(messages("home.help.link.makingTaxDigital.incomeTax"))
      cards.get(1).text() should include(messages("home.help.link.makingTaxDigital.findSoftware"))

      document.text() should not include messages("home.help.link.makingTaxDigital.changeCircumstances")
      document.text() should not include messages("home.help.link.makingTaxDigital.findPenaltyInfo")
      document.text() should not include messages("home.help.link.selfAssessment.payTaxBill")
      document.text() should not include messages("home.help.link.taxSupport.getHelp")
    }

    "render correct English links and open in new tab" in new TestSetup(isAgent = false) {
      val expectedLinks: Seq[String] = List(
        "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax",
        "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax/if-your-circumstances-change",
        "https://www.gov.uk/guidance/choose-the-right-software-for-making-tax-digital-for-income-tax",
        "https://www.gov.uk/guidance/penalties-for-making-tax-digital-for-income-tax",
        "https://www.gov.uk/pay-self-assessment-tax-bill",
        "https://www.gov.uk/difficulties-paying-hmrc"
      )

      cards.size() shouldBe expectedLinks.size

      for (i <- 0 until cards.size()) {
        val anchor = cards.get(i).selectFirst("a")

        anchor.attr("href") shouldBe expectedLinks(i)
        anchor.attr("target") shouldBe "_blank"
        anchor.attr("rel") shouldBe "noopener noreferrer"
      }
    }

    "render correct Welsh links and open in new tab" in new TestSetup(isAgent = false, welshLang = true) {
      val expectedLinks: Seq[String] = List(
        "https://www.gov.uk/guidance/defnyddio-r-cynllun-troi-treth-yn-ddigidol-ar-gyfer-treth-incwm",
        "https://www.gov.uk/guidance/defnyddio-r-cynllun-troi-treth-yn-ddigidol-ar-gyfer-treth-incwm/os-bydd-eich-amgylchiadau-n-newid",
        "https://www.gov.uk/guidance/choose-the-right-software-for-making-tax-digital-for-income-tax.cy",
        "https://www.gov.uk/guidance/penalties-for-making-tax-digital-for-income-tax.cy",
        "https://www.gov.uk/taluch-bil-treth-hunanasesiad",
        "https://www.gov.uk/anawsterau-talu-cthem"
      )

      cards.size() shouldBe expectedLinks.size

      for (i <- 0 until cards.size()) {
        val anchor = cards.get(i).selectFirst("a")

        anchor.attr("href") shouldBe expectedLinks(i)
        anchor.attr("target") shouldBe "_blank"
        anchor.attr("rel") shouldBe "noopener noreferrer"
      }
    }
  }
}