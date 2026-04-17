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

package views.newHomePage

import config.featureswitch.FeatureSwitching
import models.newHomePage.{RecentActivityCard, RecentActivityViewModel}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import testUtils.{TestSupport, ViewSpec}
import views.html.newHomePage.NewHomeRecentActivityView
import play.api.test.Helpers.defaultAwaitTimeout

import java.time.LocalDate

class NewHomeRecentActivityViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  val newHomeRecentActivityView: NewHomeRecentActivityView = app.injector.instanceOf[NewHomeRecentActivityView]

  val defaultViewModel = RecentActivityViewModel(Seq.empty)

  class TestSetup(recentActivityViewModel: RecentActivityViewModel = defaultViewModel,
                  isAgent: Boolean = false,
                  isGovUkRebrandEnabled: Boolean = true) {

    val testMessages: Messages = messages

    lazy val page = newHomeRecentActivityView(
      isAgent = isAgent,
      yourTasksUrl = "/tasksUrl",
      recentActivityUrl = "/recentActivityUrl",
      overviewUrl = "/overviewUrl",
      helpUrl = "/helpUrl",
      recentActivityViewModel = recentActivityViewModel,
      isGovUkRebrandEnabled = isGovUkRebrandEnabled
    )(testMessages, FakeRequest(), tsTestUser)

    lazy val document = Jsoup.parse(contentAsString(page))
  }


  "New Home Recent Activity page" should {
    "display the correct heading" when {
      "useRebrand is false" in new TestSetup(isGovUkRebrandEnabled = false) {
        document.getElementById("income-tax-heading").text() shouldBe "Income Tax"
      }
      "useRebrand is true" in new TestSetup(isGovUkRebrandEnabled = true) {
        document.getElementById("income-tax-heading").text() shouldBe "Self Assessment"
      }
    }

    "display the correct h2" in new TestSetup() {
      document.getElementById("recent-activities-heading").text() shouldBe "Recent activity"
    }

    "display the correct task cards" when {
      "no activity card is defined" in new TestSetup() {
        document.getElementById("no-recent-activity-text").text() shouldBe "You have no recent activity."
      }

      "a recent activity card is displayed" in new TestSetup(recentActivityViewModel = defaultViewModel.copy(recentActivityCards = Seq(RecentActivityCard("ActivityCardText", "/link", "CardContent", "DateContent", LocalDate.of(2025, 1, 1))))) {
        document.getElementById("recent-activity-card-0").text() shouldBe "ActivityCardText CardContent DateContent"
      }
    }
  }
}
