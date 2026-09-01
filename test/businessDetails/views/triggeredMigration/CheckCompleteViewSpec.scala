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

package businessDetails.views.triggeredMigration

import businessDetails.controllers.triggeredMigration.routes as triggeredMigrationRoutes
import businessDetails.views.html.triggeredMigration.CheckCompleteView
import common.testUtils.TestSupport
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import common.config.featureswitch.FeatureSwitching
import play.api.mvc.Call

class CheckCompleteViewSpec extends TestSupport with FeatureSwitching {

  val view: CheckCompleteView = app.injector.instanceOf[CheckCompleteView]

  class Setup(isAgent: Boolean) {
    val postAction: Call = triggeredMigrationRoutes.CheckCompleteController.submit(isAgent)
    val pageDocument: Document = Jsoup.parse(contentAsString(view(isAgent, postAction)))
  }

  object CheckCompleteMessages {
    val headingBanner = "Check complete You have confirmed that HMRC records only list your active businesses"

    val whatNextHeading = "What to do next"
    val whatNextP1 = "We will use the information you have provided to update our records."
    val whatNextP2 = "Any updates, deadlines or actions relating to periods when your income sources were active will be shown in your HMRC online account."
    val whatNextP3 = "You can check whether there are any updates, deadlines or actions that apply to you."
  }

  "Check complete page" when {
    "display the page" should {
      "have the correct content (individual)" in new Setup(isAgent = false) {
        pageDocument.getElementById("heading-banner").text() shouldBe CheckCompleteMessages.headingBanner

        pageDocument.getElementById("what-next-heading").text() shouldBe CheckCompleteMessages.whatNextHeading
        pageDocument.getElementById("what-next-p1").text() shouldBe CheckCompleteMessages.whatNextP1
        pageDocument.getElementById("what-next-p2").text() shouldBe CheckCompleteMessages.whatNextP2
        pageDocument.getElementById("what-next-p3").text() shouldBe CheckCompleteMessages.whatNextP3
      }

      "have the correct content (agent)" in new Setup(isAgent = true) {
        pageDocument.getElementById("heading-banner").text() shouldBe CheckCompleteMessages.headingBanner

        pageDocument.getElementById("what-next-heading").text() shouldBe CheckCompleteMessages.whatNextHeading
        pageDocument.getElementById("what-next-p1").text() shouldBe CheckCompleteMessages.whatNextP1
        pageDocument.getElementById("what-next-p2").text() shouldBe CheckCompleteMessages.whatNextP2
        pageDocument.getElementById("what-next-p3").text() shouldBe CheckCompleteMessages.whatNextP3
      }
    }
  }
}