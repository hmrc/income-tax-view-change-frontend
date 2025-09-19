/*
 * Copyright 2025 HM Revenue & Customs
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

package views.triggeredMigration

import forms.triggeredMigration.CheckActiveBusinessesConfirmForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.triggeredMigration.CheckActiveBusinessesConfirmView

class CheckActiveBusinessesConfirmViewSpec extends TestSupport{

  val view: CheckActiveBusinessesConfirmView = app.injector.instanceOf[CheckActiveBusinessesConfirmView]

  object CheckActiveBusinessConfirmMessages {
    val title = "Have you checked that HMRC records only list your active businesses? - Manage your Self Assessment - GOV.UK"
    val heading = "Have you checked that HMRC records only list your active businesses?"
    val bodyText = "You can change any other business details at a later date."
    val yesText = "Yes"
    val yesHint = "If the previous page only lists your active businesses then the check is complete"
    val noText = "No"
    val noHint = "Select this option if you have not checked the previous page"
    val continue = "Continue"
    val errorSummaryHeading = "There is a problem"
    val errorMessage = "Select yes if you’ve checked that HMRC records only list your active businesses"
  }

  class Setup(form: Form[CheckActiveBusinessesConfirmForm], isAgent: Boolean = false) {
    val pageDocument: Document = Jsoup.parse(contentAsString(
      view(
        form = form,
        postAction = controllers.triggeredMigration.routes.CheckActiveBusinessesConfirmController.submit(isAgent),
        backUrl = "/check-hmrc-records",
        isAgent = isAgent
      )
    ))
  }


  "Check Active Businesses Confirm page" should {
    Seq(false, true).foreach { isAgent =>
      val role = if (isAgent) "Agent" else "Individual"

      s"render correctly with no errors for $role" in new Setup(CheckActiveBusinessesConfirmForm(), isAgent) {
        pageDocument.title() shouldBe CheckActiveBusinessConfirmMessages.title
        pageDocument.select("h1").text() shouldBe CheckActiveBusinessConfirmMessages.heading
        pageDocument.getElementById("check-active-businesses-confirm-form").text() should include(CheckActiveBusinessConfirmMessages.bodyText)

        pageDocument.select("input[type=radio][value=Yes]").size() shouldBe 1
        pageDocument.text() should include(CheckActiveBusinessConfirmMessages.yesText)
        pageDocument.text() should include(CheckActiveBusinessConfirmMessages.yesHint)

        pageDocument.select("input[type=radio][value=No]").size() shouldBe 1
        pageDocument.text() should include(CheckActiveBusinessConfirmMessages.noText)
        pageDocument.text() should include(CheckActiveBusinessConfirmMessages.noHint)

        pageDocument.getElementById("continue-button").text() shouldBe CheckActiveBusinessConfirmMessages.continue
      }

      s"render error messages when form has errors for $role" in new Setup(CheckActiveBusinessesConfirmForm().bind(Map.empty[String, String]), isAgent) {
          pageDocument.select(".govuk-error-summary__title").text() shouldBe CheckActiveBusinessConfirmMessages.errorSummaryHeading
          pageDocument.select(".govuk-error-summary__list").text() should include(CheckActiveBusinessConfirmMessages.errorMessage)
          pageDocument.select(".govuk-error-message").text() should include(CheckActiveBusinessConfirmMessages.errorMessage)
        }
    }
  }
}
