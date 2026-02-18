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
import authV2.AuthActionsTestData.defaultMTDITUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.http.HeaderNames
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.HtmlFormat
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.NewHomeYourTasksView


class NewHomeYourTasksViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val newHomeYourTasksView: NewHomeYourTasksView = app.injector.instanceOf[NewHomeYourTasksView]
  val testMtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), businessAndPropertyAligned)

  class TestSetup(
                   origin: Option[String] = None,
                   isAgent: Boolean = false,
                   yourTasksUrl: String = "testYourTasksUrl",
                   recentActivityUrl: String = "testRecentActivityUrl",
                   overViewUrl: String = "testOverviewUrl",
                   helpUrl: String = "testHelpUrl",
                   welshLang: Boolean = false) {

    val testMessages: Messages = if (welshLang) {
      app.injector.instanceOf[MessagesApi].preferred(FakeRequest().withHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy"))
    } else {
      messages
    }

    val testUrl = "testUrl"

    lazy val page: HtmlFormat.Appendable =
      newHomeYourTasksView(
        origin,
        isAgent,
        yourTasksUrl,
        recentActivityUrl,
        overViewUrl,
        helpUrl)(testMessages, FakeRequest(), testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  //TODO proceed with this test until done
  "New Home Your Tasks page for individuals" should {
    "display the the correct content" in new TestSetup() {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe "Your tasks"
      document.select(".govuk-summary-card-no-border").get(0).text() shouldBe "View updates and deadlines"
      document.select(".govuk-summary-card-no-border").get(0).hasCorrectHref("/report-quarterly/income-and-expenses/view/dummyIndividualURL")
    }
  }

  //TODO proceed with this test until done
  "New Home Overview page for agents" should {
    "display the the correct content" in new TestSetup(isAgent = true) {
      document.select("h2.govuk-heading-m").get(0).text() shouldBe "Your tasks"
      document.select(".govuk-summary-card-no-border").get(0).text() shouldBe "View updates and deadlines"
      document.select(".govuk-summary-card-no-border").get(0).hasCorrectHref("/report-quarterly/income-and-expenses/view/dummyAgentURL")
    }
  }

}
