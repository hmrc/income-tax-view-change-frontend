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

import models.incomeSourceDetails.TaxYear
import models.optin.OptInCompletedViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.OptInCompletedCurrentYearView

object OptInCompletedCurrentYearViewSpec {
  val title = "Opt In completed - Manage your Income Tax updates - GOV.UK"
}

class OptInCompletedCurrentYearViewSpec extends TestSupport {

  val optInCompletedCurrentYearView: OptInCompletedCurrentYearView = app.injector.instanceOf[OptInCompletedCurrentYearView]


  class Setup(isAgent: Boolean = true, taxYear: TaxYear) {
    val model: OptInCompletedViewModel = OptInCompletedViewModel(isAgent = isAgent, optInTaxYear = taxYear)
    val pageDocument: Document = Jsoup.parse(contentAsString(optInCompletedCurrentYearView(model = model)))
  }

  val forYearEnd = 2023
  val year22_23: TaxYear = TaxYear.forYearEnd(forYearEnd)
  s"have the correct content for year $year22_23" in new Setup(false, year22_23) {
    pageDocument.title() shouldBe OptInCompletedCurrentYearViewSpec.title

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe "Opt In completed"
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You are now reporting quarterly from 2022 to 2023 tax year onwards"
    pageDocument.getElementById("quarterly-update-due").text() shouldBe "5 February 2023"
    pageDocument.getElementById("current-year-due").text() shouldBe "31 January 2024"
    pageDocument.getElementById("upcoming-updates-link").text() shouldBe "View your upcoming updates"
    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2022 to 2023 tax year, you would have to report quarterly from 6 April 2024."
    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

  }

  val anotherForYearEnd = 2022
  val year21_22: TaxYear = TaxYear.forYearEnd(anotherForYearEnd)
  s"have the correct content for year $year21_22" in new Setup(false, year21_22) {
    pageDocument.title() shouldBe OptInCompletedCurrentYearViewSpec.title

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe "Opt In completed"
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You are now reporting quarterly from 2021 to 2022 tax year onwards"
    pageDocument.getElementById("quarterly-update-due").text() shouldBe "5 February 2022"
    pageDocument.getElementById("current-year-due").text() shouldBe "31 January 2023"
    pageDocument.getElementById("upcoming-updates-link").text() shouldBe "View your upcoming updates"
    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2021 to 2022 tax year, you would have to report quarterly from 6 April 2023."
    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

  }

}
