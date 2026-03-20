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
import models.newHomePage.HandleYourTasksViewModel
import org.jsoup.Jsoup
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.{TestSupport, ViewSpec}
import views.html.newHomePage.NewHomeYourTasksView

class NewHomeYourTasksViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  val newHomeOverviewView: NewHomeYourTasksView = app.injector.instanceOf[NewHomeYourTasksView]

  val defaultViewModel = HandleYourTasksViewModel(Seq.empty, Seq.empty, Seq.empty, None)

  class TestSetup(handleYourTasksViewModel: HandleYourTasksViewModel = defaultViewModel,
                  isAgent: Boolean = false,
                  isGovUkRebrandEnabled: Boolean = true) {

    val testMessages: Messages = messages

    lazy val page = newHomeOverviewView(
      isAgent = isAgent,
      yourTasksUrl = "/tasksUrl",
      recentActivityUrl = "/recentActivityUrl",
      overviewUrl = "/overviewUrl",
      helpUrl = "/helpUrl",
      viewModel = handleYourTasksViewModel,
      isGovUkRebrandEnabled = isGovUkRebrandEnabled
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

    }

    "display the correct h2" when {

    }

    "display the correct task cards" when {
      "no task card is defined" in {

      }
      "there is only an overdue task" in {

      }
      "there is only a dateless task" in {

      }
      "there is only an upcoming task" in {

      }
      "there are multiple overdue tasks" in {

      }
      "there are multiple dateless tasks" in {

      }
      "there are multiple upcoming tasks" in {

      }
      "there are multiple of all kinds of tasks" in {

      }
    }
  }
}