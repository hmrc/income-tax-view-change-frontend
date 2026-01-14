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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.triggeredMigration.CheckCompleteView

class CheckCompleteViewSpec extends TestSupport {

  val view: CheckCompleteView = app.injector.instanceOf[CheckCompleteView]
  def nextUpdatesLink(isAgent: Boolean): String =
    if(isAgent)controllers.routes.NextUpdatesController.showAgent().url
    else controllers.routes.NextUpdatesController.show().url
  val compatibleSoftwareLink: String = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  class Setup(isAgent: Boolean) {
    val pageDocument: Document = Jsoup.parse(contentAsString(view(isAgent, compatibleSoftwareLink, nextUpdatesLink(isAgent))))
  }

  object CheckCompleteMessages {
    val headingBanner = "Check complete You have confirmed that HMRC records only list your active businesses"

    val whatNextHeading = "What to do next"
    val whatNextP1 = "You now need to provide HMRC a quarterly update for each of your active sole trader and property income sources."
    val whatNextP2 = "To do this, you need to:"
    val whatNextUlLi1 = "get software that works with Making Tax Digital for Income Tax"
    val whatNextUlLi2 = "find out what is due and submit your late update(s)"

    val gettingSoftwareHeading = "Getting software"
    val gettingSoftwareP1 = "To submit your quarterly updates, you or your agent must have software compatible with Making Tax Digital for Income Tax. There are both paid and free options to choose from."
    val gettingSoftwareLinkText = "Find out about compatible software (opens in new tab)"

    val submitUpdatesHeading = "Submitting your update(s)"
    val submitUpdatesP1 = "In your account you can see which quarterly updates are due and what information you need to provide. Then you must use your compatible software to:"
    val submitUpdatesUlLi1 = "create a digital record of your property and sole trader income and expenses"
    val submitUpdatesUlLi2 = "send your quarterly update(s) to HMRC"
    val submitUpdatesLinkText = "Check your latest submission deadlines"

  }

  "Check complete page" when {
    "display the page" should {
      "have the correct content (individual)" in new Setup(isAgent = false) {

        pageDocument.getElementById("heading-banner").text() shouldBe CheckCompleteMessages.headingBanner

        pageDocument.getElementById("what-next-heading").text() shouldBe CheckCompleteMessages.whatNextHeading
        pageDocument.getElementById("what-next-p1").text() shouldBe CheckCompleteMessages.whatNextP1
        pageDocument.getElementById("what-next-p2").text() shouldBe CheckCompleteMessages.whatNextP2
        pageDocument.select("#what-next-bullets").get(0).text() shouldBe CheckCompleteMessages.whatNextUlLi1
        pageDocument.select("#what-next-bullets").get(1).text() shouldBe CheckCompleteMessages.whatNextUlLi2

        pageDocument.getElementById("getting-software-heading").text() shouldBe CheckCompleteMessages.gettingSoftwareHeading
        pageDocument.getElementById("getting-software-p1").text() shouldBe CheckCompleteMessages.gettingSoftwareP1
        pageDocument.getElementById("getting-software-link").text() shouldBe CheckCompleteMessages.gettingSoftwareLinkText
        pageDocument.getElementById("getting-software-link").attr("href") shouldBe compatibleSoftwareLink

        pageDocument.getElementById("submitting-your-update-heading").text() shouldBe CheckCompleteMessages.submitUpdatesHeading
        pageDocument.getElementById("submitting-your-update-p1").text() shouldBe CheckCompleteMessages.submitUpdatesP1
        pageDocument.select("#submitting-your-update-bullets").get(0).text() shouldBe CheckCompleteMessages.submitUpdatesUlLi1
        pageDocument.select("#submitting-your-update-bullets").get(1).text() shouldBe CheckCompleteMessages.submitUpdatesUlLi2
        pageDocument.getElementById("submitting-your-updates-link").text() shouldBe CheckCompleteMessages.submitUpdatesLinkText
        pageDocument.getElementById("submitting-your-updates-link").attr("href") shouldBe nextUpdatesLink(isAgent = false)

      }
      "have the correct content (agent)" in new Setup(isAgent = true) {

        pageDocument.getElementById("heading-banner").text() shouldBe CheckCompleteMessages.headingBanner

        pageDocument.getElementById("what-next-heading").text() shouldBe CheckCompleteMessages.whatNextHeading
        pageDocument.getElementById("what-next-p1").text() shouldBe CheckCompleteMessages.whatNextP1
        pageDocument.getElementById("what-next-p2").text() shouldBe CheckCompleteMessages.whatNextP2
        pageDocument.select("#what-next-bullets").get(0).text() shouldBe CheckCompleteMessages.whatNextUlLi1
        pageDocument.select("#what-next-bullets").get(1).text() shouldBe CheckCompleteMessages.whatNextUlLi2

        pageDocument.getElementById("getting-software-heading").text() shouldBe CheckCompleteMessages.gettingSoftwareHeading
        pageDocument.getElementById("getting-software-p1").text() shouldBe CheckCompleteMessages.gettingSoftwareP1
        pageDocument.getElementById("getting-software-link").text() shouldBe CheckCompleteMessages.gettingSoftwareLinkText
        pageDocument.getElementById("getting-software-link").attr("href") shouldBe compatibleSoftwareLink

        pageDocument.getElementById("submitting-your-update-heading").text() shouldBe CheckCompleteMessages.submitUpdatesHeading
        pageDocument.getElementById("submitting-your-update-p1").text() shouldBe CheckCompleteMessages.submitUpdatesP1
        pageDocument.select("#submitting-your-update-bullets").get(0).text() shouldBe CheckCompleteMessages.submitUpdatesUlLi1
        pageDocument.select("#submitting-your-update-bullets").get(1).text() shouldBe CheckCompleteMessages.submitUpdatesUlLi2
        pageDocument.getElementById("submitting-your-updates-link").text() shouldBe CheckCompleteMessages.submitUpdatesLinkText
        pageDocument.getElementById("submitting-your-updates-link").attr("href") shouldBe nextUpdatesLink(isAgent = true)

      }
    }
  }
}