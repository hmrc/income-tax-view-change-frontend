/*
 * Copyright 2024 HM Revenue & Customs
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

package financials.views.claimToAdjustPoa

import common.auth.MtdItUser
import common.testUtils.TestSupport
import financials.controllers.claimToAdjustPoa.routes as claimToAdjustPoaRoutes
import financials.views.html.claimToAdjustPoa.ApiFailureSubmittingPoaView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.test.Helpers.contentAsString

class ApiFailureSubmittingPoaViewSpec extends TestSupport {

  class Setup(isAgent: Boolean) {

    val view: ApiFailureSubmittingPoaView = app.injector.instanceOf[ApiFailureSubmittingPoaView]
    implicit val testUser: MtdItUser[?] = if (isAgent) agentUserConfirmedClient() else individualUser

    val document: Document =
      Jsoup.parse(
        contentAsString(
          view()
        )
      )
  }

  def getHomeControllerLink(isAgent: Boolean): String = appConfig.homePageUrl(isAgent)

  def executeTest(isAgent: Boolean): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: CheckYourAnswersView" should {
      "render the heading" in new Setup(isAgent) {
        document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("claimToAdjustPoa.apiFailure.heading")
      }
      "render the first paragraph" in new Setup(isAgent) {
        document.getElementById("paragraph-text-1").text() shouldBe
          messages("claimToAdjustPoa.apiFailure.para1")
      }
      "render the second paragraph" in new Setup(isAgent) {
        document.getElementById("paragraph-text-2").text() shouldBe
          messages("claimToAdjustPoa.apiFailure.para2")
      }
      "render the first bullet point with the correct link" in new Setup(isAgent) {
        document.getElementsByClass("govuk-!-margin-bottom-4").get(0).text() shouldBe
          messages("claimToAdjustPoa.apiFailure.bullet1Text") + " " + messages("claimToAdjustPoa.apiFailure.bullet1Link")
        document.getElementById("link-1").attr("href") shouldBe
          claimToAdjustPoaRoutes.AmendablePoaController.show(isAgent).url
      }
      "render the second bullet point with the correct link" in new Setup(isAgent) {
        document.getElementsByClass("govuk-!-margin-bottom-4").get(1).text() shouldBe
          messages("claimToAdjustPoa.apiFailure.bullet2Text") + " " + messages("claimToAdjustPoa.apiFailure.bullet2Link")
        document.getElementById("link-2").attr("href") shouldBe
          getHomeControllerLink(isAgent)
      }
    }
  }

  executeTest(isAgent = false)
  executeTest(isAgent = true)
}
