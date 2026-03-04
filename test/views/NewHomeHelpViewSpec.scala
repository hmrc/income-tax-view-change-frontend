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


package views

import auth.MtdItUser
import authV2.AuthActionsTestData.getMtdItUser
import config.featureswitch.FeatureSwitching
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.NewHomeHelpView

class NewHomeHelpViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  private val newHomeHelpView: NewHomeHelpView = app.injector.instanceOf[NewHomeHelpView]

  class TestSetup(
                   origin: Option[String] = None,
                   isAgent: Boolean = false,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   welshLang: Boolean = false
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
      if (isAgent) getMtdItUser(Agent)(request)
      else getMtdItUser(Individual)(request)

    lazy val page: HtmlFormat.Appendable =
      newHomeHelpView(
        origin,
        isAgent,
        yourTasksUrl,
        recentActivityUrl,
        overViewUrl,
        helpUrl
      )(testMessages, request, testUser)

    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val cards: Elements = document.select(".govuk-summary-card-no-border")
  }

  "New Home Help page" should {

    "display the section heading and open-in-new-tab message" in new TestSetup() {
      document.select("h2.govuk-heading-m").first().text() shouldBe messages("new.home.help.heading")
      document.select("p.govuk-body").first().text() shouldBe messages("new.home.help.openInNewTab")
    }

    "display 4 help cards for an individual user" in new TestSetup(isAgent = false) {
      cards.size() shouldBe 4

      cards.get(0).text() should include(messages("home.help.link.makingTaxDigital.incomeTax"))
      cards.get(1).text() should include(messages("home.help.link.selfAssessment.payTaxBill"))
      cards.get(2).text() should include(messages("home.help.link.makingTaxDigital.findSoftware"))
      cards.get(3).text() should include(messages("home.help.link.taxSupport.getHelp"))
    }

    "display only 2 help cards for a supporting or primary agent user" in new TestSetup(isAgent = true) {
      cards.size() shouldBe 2

      cards.get(0).text() should include(messages("home.help.link.makingTaxDigital.incomeTax"))
      cards.get(1).text() should include(messages("home.help.link.makingTaxDigital.findSoftware"))

      document.text() should not include messages("home.help.link.selfAssessment.payTaxBill")
      document.text() should not include messages("home.help.link.taxSupport.getHelp")
    }

    "render correct English links and open in new tab" in new TestSetup(isAgent = false) {
      val expectedLinks: Seq[String] = List(
        "https://www.gov.uk/guidance/use-making-tax-digital-for-income-tax",
        "https://www.gov.uk/pay-self-assessment-tax-bill",
        "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax",
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
        "https://www.gov.uk/taluch-bil-treth-hunanasesiad",
        "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax.cy",
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