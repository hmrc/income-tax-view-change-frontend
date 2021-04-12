/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.TaxYears


class TaxYearsViewSpec extends ViewSpec {

  val taxYears: TaxYears = app.injector.instanceOf[TaxYears]

  val year2020: Int = 2020
  val year2021: Int = 2021

  def view(years: List[Int] = List(year2021, year2020), submissionIntegrationEnabled: Boolean = true): Html = {
    taxYears(years, testBackUrl, itsaSubmissionIntegrationEnabled = submissionIntegrationEnabled)
  }

  object TaxYearsMessages {
    val title: String = "Tax years - Your client’s Income Tax details - GOV.UK"
    val heading: String = "Tax years"
    val noCalculations: String = "You don’t have an estimate right now. We’ll show your next Income Tax estimate when you submit a report using software."
    def taxYearDates(year: Int): String = s"6 April ${year - 1} to 5 April $year"
    def updateReturn(year: Int): String = s"Update return ${taxYearDates(year)}"
    def viewReturn(year: Int): String = s"View return ${taxYearDates(year)}"
    val back: String = "Back"
  }

  "The Confirm Client page" should {

    "have a title" in new Setup(view()) {
      document.title shouldBe TaxYearsMessages.title
    }

    "have a heading" in new Setup(view()) {
      content hasPageHeading TaxYearsMessages.heading
    }

    "have a back link" in new Setup(view()) {
      content.backLink.text shouldBe TaxYearsMessages.back
      content.hasBackLinkTo(testBackUrl)
    }

    "have a message that there are no calculations" when {
      "there are no tax years available" in new Setup(view(years = Nil)) {
        content.selectHead("#no-taxYears").text shouldBe TaxYearsMessages.noCalculations
      }
    }

    "have a list containing tax years" which {
      "has a 2021 year row" which {
        "includes a submission service link" when {
          "the submission integration flag is enabled" in new Setup(view()) {
            val row: Element = content.selectHead("dl").selectNth("div", 1)
            row.selectNth("dd", 1).text shouldBe TaxYearsMessages.taxYearDates(year2021)
            row.selectNth("dd", 2).selectHead("a").text shouldBe TaxYearsMessages.updateReturn(year2021)
            row.selectNth("dd", 2).selectHead("a").attr("href") shouldBe appConfig.submissionFrontendUrl
            row.selectNth("dd", 2).selectHead("a").selectHead("span").text shouldBe TaxYearsMessages.taxYearDates(year2021)
            row.selectNth("dd", 2).selectHead("a").selectHead("span").attr("class") shouldBe "visuallyhidden"
            row.selectNth("dd", 3).selectHead("a").text shouldBe TaxYearsMessages.viewReturn(year2021)
            row.selectNth("dd", 3).selectHead("a").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(year2021).url
            row.selectNth("dd", 3).selectHead("a").selectHead("span").text shouldBe TaxYearsMessages.taxYearDates(year2021)
            row.selectNth("dd", 3).selectHead("a").selectHead("span").attr("class") shouldBe "visuallyhidden"
          }
        }
        "does not include a submission service link" when {
          "the submission integration flag is disabled" in new Setup(view(submissionIntegrationEnabled = false)) {
            val row: Element = content.selectHead("dl").selectNth("div", 1)
            row.selectNth("dd", 1).text shouldBe TaxYearsMessages.taxYearDates(year2021)
            row.selectNth("dd", 2).selectHead("a").text shouldBe TaxYearsMessages.viewReturn(year2021)
            row.selectNth("dd", 2).selectHead("a").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(year2021).url
            row.selectNth("dd", 2).selectHead("a").selectHead("span").text shouldBe TaxYearsMessages.taxYearDates(year2021)
            row.selectNth("dd", 2).selectHead("a").selectHead("span").attr("class") shouldBe "visuallyhidden"
          }
        }
      }
      "has a 2020 year row" which {
        "includes a submission service link" when {
          "the submission integration flag is enabled" in new Setup(view()) {
            val row: Element = content.selectHead("dl").selectNth("div", 2)
            row.selectNth("dd", 1).text shouldBe TaxYearsMessages.taxYearDates(year2020)
            row.selectNth("dd", 2).selectHead("a").text shouldBe TaxYearsMessages.updateReturn(year2020)
            row.selectNth("dd", 2).selectHead("a").attr("href") shouldBe appConfig.submissionFrontendUrl
            row.selectNth("dd", 2).selectHead("a").selectHead("span").text shouldBe TaxYearsMessages.taxYearDates(year2020)
            row.selectNth("dd", 2).selectHead("a").selectHead("span").attr("class") shouldBe "visuallyhidden"
            row.selectNth("dd", 3).selectHead("a").text shouldBe TaxYearsMessages.viewReturn(year2020)
            row.selectNth("dd", 3).selectHead("a").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(year2020).url
            row.selectNth("dd", 3).selectHead("a").selectHead("span").text shouldBe TaxYearsMessages.taxYearDates(year2020)
            row.selectNth("dd", 3).selectHead("a").selectHead("span").attr("class") shouldBe "visuallyhidden"
          }
        }
        "does not include a submission service link" when {
          "the submission integration flag is disabled" in new Setup(view(submissionIntegrationEnabled = false)) {
            val row: Element = content.selectHead("dl").selectNth("div", 2)
            row.selectNth("dd", 1).text shouldBe TaxYearsMessages.taxYearDates(year2020)
            row.selectNth("dd", 2).selectHead("a").text shouldBe TaxYearsMessages.viewReturn(year2020)
            row.selectNth("dd", 2).selectHead("a").attr("href") shouldBe controllers.agent.routes.TaxYearOverviewController.show(year2020).url
            row.selectNth("dd", 2).selectHead("a").selectHead("span").text shouldBe TaxYearsMessages.taxYearDates(year2020)
            row.selectNth("dd", 2).selectHead("a").selectHead("span").attr("class") shouldBe "visuallyhidden"
          }
        }
      }
    }
  }
}
