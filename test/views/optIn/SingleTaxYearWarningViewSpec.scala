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

package views.optIn

import forms.optIn.SingleTaxYearOptInWarningForm
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.SingleTaxYearWarningView
import views.messages.OptInSingleTaxYearWarningMessages._

class SingleTaxYearWarningViewSpec extends TestSupport {

  val view: SingleTaxYearWarningView = app.injector.instanceOf[SingleTaxYearWarningView]

  val form: Form[SingleTaxYearOptInWarningForm] = SingleTaxYearOptInWarningForm(TaxYear(2025, 2026))

  def homePageUrl(isAgent: Boolean): String = {
    if (isAgent) {
      controllers.routes.HomeController.showAgent().url
    } else {
      controllers.routes.HomeController.show().url
    }
  }

  def bullet(i: Int): String = s"#dropdown-inset-text-bullet-$i"

  object Selectors {
    val title = "single-tax-year-warning-heading"
    val h1 = "single-tax-year-warning-heading"
    val p1 = "single-tax-year-warning-paragraph"
    val h2VolSubheading = "voluntary-reporting-subheading"
    val volInsetParagraph = "voluntary-reporting-inset"
    val volDescParagraph = "voluntary-reporting-description"
    val dropDown = "#main-content > div > div > div > details > summary > span"
    val dropDownParagraph1 = "dropdown-inset-text-first-paragraph"
    val dropDownParagraph2 = "dropdown-inset-text-second-paragraph"
    val yesHint = "still-opt-in-radio-button-yes-item-hint"
    val noHint = "still-opt-in-radio-button-no-item-hint"
  }

  "SingleTaxYearWarningView" when {

    "the user is an Agent" should {

      "return the correct content" in {

        disableAllSwitches()

        val isAgentFlag = true

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                form = form,
                submitAction = controllers.optIn.routes.ConfirmTaxYearController.show(isAgentFlag),
                isAgent = isAgentFlag,
                taxYear = TaxYear(2025, 2026)
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
            Selectors.h2VolSubheading -> h2VolSubheadingContent,
            Selectors.volInsetParagraph -> volInsetParagraphContent,
            Selectors.volDescParagraph -> volDescParagraphContent,
            Selectors.dropDownParagraph1 -> dropDownParagraph1,
            Selectors.dropDownParagraph2 -> dropDownParagraph2,
            Selectors.yesHint -> yesHintContent,
            Selectors.noHint -> noHintContent
          )

        pageDocument.title() shouldBe titleContentAgent

        testContentByIds(expectedContent)

        pageDocument.select(Selectors.dropDown).text() shouldBe dropDownContent
        pageDocument.select(bullet(1)).text() shouldBe bulletContent1
        pageDocument.select(bullet(2)).text() shouldBe bulletContent2
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe stillOptInContent
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
                form = form,
                submitAction = controllers.optIn.routes.ConfirmTaxYearController.show(isAgentFlag),
                isAgent = isAgentFlag,
                taxYear = TaxYear(2025, 2026)
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
            Selectors.h2VolSubheading -> h2VolSubheadingContent,
            Selectors.volInsetParagraph -> volInsetParagraphContent,
            Selectors.volDescParagraph -> volDescParagraphContent,
            Selectors.dropDownParagraph1 -> dropDownParagraph1,
            Selectors.dropDownParagraph2 -> dropDownParagraph2,
            Selectors.yesHint -> yesHintContent,
            Selectors.noHint -> noHintContent
          )

        pageDocument.title() shouldBe titleContent

        testContentByIds(expectedContent)

        pageDocument.select(Selectors.dropDown).text() shouldBe dropDownContent
        pageDocument.select(bullet(1)).text() shouldBe bulletContent1
        pageDocument.select(bullet(2)).text() shouldBe bulletContent2
        pageDocument.getElementsByClass("govuk-fieldset__legend--m").text() shouldBe stillOptInContent
      }
    }
  }
}
