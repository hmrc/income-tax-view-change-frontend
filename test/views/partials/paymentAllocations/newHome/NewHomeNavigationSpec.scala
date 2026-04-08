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

package views.partials.paymentAllocations.newHome

import testUtils.TestSupport
import views.html.partials.newHome.NewHomeNavigation
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.twirl.api.HtmlFormat
import play.api.test.Helpers.*

class NewHomeNavigationSpec extends TestSupport {

  lazy val newHomeNavigation = app.injector.instanceOf[NewHomeNavigation]

  class Setup(yourTasksUrl: String = "your-tasks-url",
              recentActivityUrl: String = "recent-activity-url",
              overViewUrl: String = "overview-url",
              helpUrl: String = "help-url",
              activeTab: String = "your-tasks",
              isRecentActivityEnabled: Boolean = false) {

    val html: HtmlFormat.Appendable = newHomeNavigation(yourTasksUrl, recentActivityUrl, overViewUrl, helpUrl, activeTab, isRecentActivityEnabled)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  "NewHomeNavigation" should {
    "display the correct tabs" when {
      "the 'Recent Activity' feature switch is ENABLED" in new Setup(isRecentActivityEnabled = true){
        val navigationTabs: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select("a")
        navigationTabs.size() shouldBe 4
        navigationTabs.get(0).text() shouldBe "Your tasks"
        navigationTabs.get(1).text() shouldBe "Recent activity"
        navigationTabs.get(2).text() shouldBe "Overview"
        navigationTabs.get(3).text() shouldBe "Help"

      }
      "the 'Recent Activity' feature switch is DISABLED" in new Setup(isRecentActivityEnabled = false) {
        val navigationTabs: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select("a")
        navigationTabs.size() shouldBe 3
        navigationTabs.get(0).text() shouldBe "Your tasks"
        navigationTabs.get(1).text() shouldBe "Overview"
        navigationTabs.get(2).text() shouldBe "Help"
      }
    }
    "display the correct active tab" when {
      "the Your tasks tab is selected" in new Setup(activeTab = "your-tasks") {
        val activeTab: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select(".govuk-service-navigation__item--active")
        activeTab.text() shouldBe "Your tasks"
      }
      "the Recent Activity feature switch is ENABLED and Recent activity tab is selected" in new Setup(activeTab = "recent-activity" , isRecentActivityEnabled= true) {
        val activeTab: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select(".govuk-service-navigation__item--active")
        activeTab.text() shouldBe "Recent activity"
      }
      "the Recent Activity feature switch is DISABLED and Recent activity tab is selected" in new Setup(activeTab = "recent-activity" , isRecentActivityEnabled= false) {
        val activeTab: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select(".govuk-service-navigation__item--active")
        activeTab.text() shouldBe "Your tasks"
      }
      "the Overview tab is selected" in new Setup(activeTab = "overview", isRecentActivityEnabled = true) {
        val activeTab: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select(".govuk-service-navigation__item--active")
        activeTab.text() shouldBe "Overview"
      }
      "the Help tab is selected" in new Setup(activeTab = "help", isRecentActivityEnabled = true) {
        val activeTab: Elements = pageDocument.selectFirst(".govuk-service-navigation__wrapper").select(".govuk-service-navigation__item--active")
        activeTab.text() shouldBe "Help"
      }
    }
  }
}
