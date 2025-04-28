/*
 * Copyright 2023 HM Revenue & Customs
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

package views.optOut

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optOut.OptOutCancelledView
import views.messages.OptOutCancelledViewMessages._

class OptOutCancelledViewSpec extends TestSupport {

  val view: OptOutCancelledView = app.injector.instanceOf[OptOutCancelledView]

  def nextUpdatesUrl(isAgent: Boolean): String = {
    if (isAgent) {
      controllers.routes.NextUpdatesController.showAgent().url
    } else {
      controllers.routes.NextUpdatesController.show().url
    }
  }

  def homePageUrl(isAgent: Boolean): String = {
    if (isAgent) {
      controllers.routes.HomeController.showAgent().url
    } else {
      controllers.routes.HomeController.show().url
    }
  }

  def manageYourBusinessesUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url

  def bullet(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"

  object Selectors {
    val title = "reporting-frequency-heading"
    val h1 = "opt-out-cancelled"
    val p1 = "continue-to-report-quarterly"
    val h2 = "reporting-annually"
    val p2 = "can-choose-to-report-annually"
    val p3Link = "#manage-your-businesses"
    val p3 = "change-how-often"
  }

  "OptOutCancelledView" when {

    "the user is an Agent" should {

      "return the correct content" in {

        disableAllSwitches()

        val isAgentFlag = true

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                isAgent = isAgentFlag,
                taxYearOpt = Some(dateService.getCurrentTaxYear)
              )
            )
          )

        def testContentByIds(idsAndContent: Seq[(String, String)]): Unit =
          idsAndContent.foreach {
            case (selectors, content) =>
              pageDocument.getElementById(selectors).text() shouldBe content
          }

        val expectedContent: Seq[(String, String)] =
          Seq(
            Selectors.h1 -> h1Content,
            Selectors.p1 -> p1Content,
            Selectors.h2 -> h2Content,
            Selectors.p2 -> p2Content,
            Selectors.p3 -> p3Content
          )

        pageDocument.title() shouldBe titleAgentContent

        testContentByIds(expectedContent)

        pageDocument.select(bullet(1)).text() shouldBe bullet1LinkContent
        pageDocument.select(bullet(1)).attr("href") shouldBe nextUpdatesUrl(isAgentFlag)

        pageDocument.select(bullet(2)).text() shouldBe bullet2LinkContent
        pageDocument.select(bullet(2)).attr("href") shouldBe homePageUrl(isAgentFlag)

        pageDocument.select(Selectors.p3Link).text() shouldBe p3LinkContent
        pageDocument.select(Selectors.p3Link).attr("href") shouldBe manageYourBusinessesUrl
      }
    }

    "the user is Non-Agent" should {

      "return the correct content" in {

        disableAllSwitches()
        
        val isAgentFlag = false

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                isAgent = isAgentFlag,
                taxYearOpt = Some(dateService.getCurrentTaxYear)
              )
            )
          )

        def testContentByIds(idsAndContent: Seq[(String, String)]): Unit =
          idsAndContent.foreach {
            case (selectors, content) =>
              pageDocument.getElementById(selectors).text() shouldBe content
          }

        val expectedContent: Seq[(String, String)] =
          Seq(
            Selectors.h1 -> h1Content,
            Selectors.p1 -> p1Content,
            Selectors.h2 -> h2Content,
            Selectors.p2 -> p2Content,
            Selectors.p3 -> p3Content,
          )

        pageDocument.title() shouldBe titleIndividualContent

        testContentByIds(expectedContent)

        pageDocument.select(bullet(1)).text() shouldBe bullet1LinkContent
        pageDocument.select(bullet(1)).attr("href") shouldBe nextUpdatesUrl(isAgentFlag)

        pageDocument.select(bullet(2)).text() shouldBe bullet2LinkContent
        pageDocument.select(bullet(2)).attr("href") shouldBe homePageUrl(isAgentFlag)

        pageDocument.select(Selectors.p3Link).text() shouldBe p3LinkContent
        pageDocument.select(Selectors.p3Link).attr("href") shouldBe manageYourBusinessesUrl
      }
    }
  }
}
