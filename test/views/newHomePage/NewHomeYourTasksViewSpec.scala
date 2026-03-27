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
import implicits.ImplicitDateFormatter
import models.newHomePage.YourTaskCardType.{FINANCIALS, PENALTIES, SUBMISSIONS}
import models.newHomePage.YourTasksCard.{DatelessTaskCard, OverdueTaskCard, UpcomingTaskCard}
import models.newHomePage.{HandleYourTasksViewModel, MaturityLevel, NoTaskCard}
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.{TestSupport, ViewSpec}
import views.html.newHomePage.NewHomeYourTasksView

class NewHomeYourTasksViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  val newHomeOverviewView: NewHomeYourTasksView = app.injector.instanceOf[NewHomeYourTasksView]

  val defaultViewModel = HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, None)
  val noTaskCard = NoTaskCard("No Task Heading", "No Task Content")
  def overdueTaskCard(index: String): OverdueTaskCard = OverdueTaskCard(s"OverdueContent$index", "OverdueLinkText", "/Overdue", "DueTag", None, Some("100"), PENALTIES)
  def datelessTaskCard(index: String): DatelessTaskCard = DatelessTaskCard(s"DatelessContent$index", "DatelessLinkText", "/Dateless", Some("100"), FINANCIALS)
  def upcomingTaskCard(index:String): UpcomingTaskCard = UpcomingTaskCard(s"UpcomingContent$index", "UpcomingLinkText", "/Upcoming", "DueTag", None, Some("100"), MaturityLevel.Upcoming, SUBMISSIONS)

  class TestSetup(handleYourTasksViewModel: HandleYourTasksViewModel = defaultViewModel,
                  isAgent: Boolean = false,
                  isGovUkRebrandEnabled: Boolean = true,
                  isRecentActivityEnabled: Boolean = false) {

    val testMessages: Messages = messages

    lazy val page = newHomeOverviewView(
      isAgent = isAgent,
      yourTasksUrl = "/tasksUrl",
      recentActivityUrl = "/recentActivityUrl",
      overviewUrl = "/overviewUrl",
      helpUrl = "/helpUrl",
      viewModel = handleYourTasksViewModel,
      isGovUkRebrandEnabled = isGovUkRebrandEnabled,
      isRecentActivityEnabled = isRecentActivityEnabled
    )(testMessages, FakeRequest(), tsTestUser)

    lazy val document = Jsoup.parse(contentAsString(page))
  }

  "New Home Your Tasks page" should {
    "display the correct heading" when {
      "useRebrand is false" in new TestSetup(isGovUkRebrandEnabled = false) {
        document.getElementById("income-tax-heading").text() shouldBe "Income Tax"
      }
      "useRebrand is true" in new TestSetup(isGovUkRebrandEnabled = true) {
        document.getElementById("income-tax-heading").text() shouldBe "Self Assessment"
      }
    }

    "display the correct service navigation section" when {
      "Recent Activity feature switch is ENABLED" in new TestSetup(isRecentActivityEnabled = true) {
        document.getElementsByClass("govuk-service-navigation__item--active").eq(0).text() shouldBe "Your tasks"
        document.getElementsByClass("govuk-service-navigation__item").eq(1).text() shouldBe "Recent activity"
        document.getElementsByClass("govuk-service-navigation__item").eq(2).text() shouldBe "Overview"
        document.getElementsByClass("govuk-service-navigation__item").eq(3).text() shouldBe "Help"
      }
      "Recent Activity feature switch is DISABLED" in new TestSetup(isRecentActivityEnabled = false) {
        document.getElementsByClass("govuk-service-navigation__item--active").eq(0).text() shouldBe "Your tasks"
        document.getElementsByClass("govuk-service-navigation__item").eq(1).text() shouldBe "Overview"
        document.getElementsByClass("govuk-service-navigation__item").eq(2).text() shouldBe "Help"
      }
    }

    "display the correct h2" in new TestSetup() {
      document.getElementById("your-tasks-heading").text() shouldBe "Your tasks"
    }

    "display the correct task cards" when {
      "no task card is defined" in new TestSetup(handleYourTasksViewModel = defaultViewModel.copy(noTaskCard = Some(noTaskCard))) {
        document.getElementById("noTaskCard").text() shouldBe "No Task Heading No Task Content"
      }
      "there is only an overdue task" in new TestSetup(handleYourTasksViewModel = defaultViewModel.copy(overdueTasks = Seq(overdueTaskCard("1")))) {
        document.getElementById("overdueTaskCard-0").text() shouldBe "OverdueLinkText OverdueContent1"
      }
      "there is only a dateless task" in new TestSetup(handleYourTasksViewModel = defaultViewModel.copy(datelessTasks = Seq(datelessTaskCard("1")))) {
        document.getElementById("datelessTaskCard-0").text() shouldBe "DatelessLinkText DatelessContent1"
      }
      "there is only an upcoming task" in new TestSetup(handleYourTasksViewModel = defaultViewModel.copy(upcomingTasks = Seq(upcomingTaskCard("1")))) {
        document.getElementById("upcomingTaskCard-0").text() shouldBe "UpcomingLinkText UpcomingContent1"
      }
      "there are multiple overdue tasks" in new TestSetup(defaultViewModel.copy(overdueTasks = Seq(overdueTaskCard("1"), overdueTaskCard("2")))) {
        document.getElementById("overdueTaskCard-0").text() shouldBe "OverdueLinkText OverdueContent1"
        document.getElementById("overdueTaskCard-1").text() shouldBe "OverdueLinkText OverdueContent2"
      }
      "there are multiple dateless tasks" in new TestSetup(defaultViewModel.copy(datelessTasks = Seq(datelessTaskCard("1"), datelessTaskCard("2")))) {
        document.getElementById("datelessTaskCard-0").text() shouldBe "DatelessLinkText DatelessContent1"
        document.getElementById("datelessTaskCard-1").text() shouldBe "DatelessLinkText DatelessContent2"
      }
      "there are multiple upcoming tasks" in new TestSetup(defaultViewModel.copy(upcomingTasks = Seq(upcomingTaskCard("1"), upcomingTaskCard("2")))) {
        document.getElementById("upcomingTaskCard-0").text() shouldBe "UpcomingLinkText UpcomingContent1"
        document.getElementById("upcomingTaskCard-1").text() shouldBe "UpcomingLinkText UpcomingContent2"
      }
      "there are multiple of all kinds of tasks" in new TestSetup(defaultViewModel.copy(
        overdueTasks = Seq(overdueTaskCard("1"), overdueTaskCard("2")),
        datelessTasks = Seq(datelessTaskCard("1"), datelessTaskCard("2")),
        upcomingTasks = Seq(upcomingTaskCard("1"), upcomingTaskCard("2"))
      )) {
        document.getElementById("overdueTaskCard-0").text() shouldBe "OverdueLinkText OverdueContent1"
        document.getElementById("overdueTaskCard-1").text() shouldBe "OverdueLinkText OverdueContent2"

        document.getElementById("datelessTaskCard-0").text() shouldBe "DatelessLinkText DatelessContent1"
        document.getElementById("datelessTaskCard-1").text() shouldBe "DatelessLinkText DatelessContent2"

        document.getElementById("upcomingTaskCard-0").text() shouldBe "UpcomingLinkText UpcomingContent1"
        document.getElementById("upcomingTaskCard-1").text() shouldBe "UpcomingLinkText UpcomingContent2"
      }
    }
  }
}