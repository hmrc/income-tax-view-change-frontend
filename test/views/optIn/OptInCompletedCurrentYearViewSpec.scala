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
  val forYearEnd = 2023

  class Setup(isAgent: Boolean = true, taxYear: TaxYear) {
    val model: OptInCompletedViewModel = OptInCompletedViewModel(isAgent = isAgent, optInTaxYear = taxYear)
    val pageDocument: Document = Jsoup.parse(contentAsString(optInCompletedCurrentYearView(model = model)))
  }

  s"have the correct content for year ${TaxYear.forYearEnd(forYearEnd)}" in new Setup(false, TaxYear.forYearEnd(forYearEnd)) {
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

  s"have the correct content for year ${TaxYear.forYearEnd(forYearEnd).nextYear}" in new Setup(false, TaxYear.forYearEnd(forYearEnd).nextYear) {
    pageDocument.title() shouldBe OptInCompletedCurrentYearViewSpec.title

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe "Opt In completed"
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You are now reporting quarterly from 2023 to 2024 tax year onwards"
    pageDocument.getElementById("quarterly-update-due").text() shouldBe "5 February 2024"
    pageDocument.getElementById("current-year-due").text() shouldBe "31 January 2025"
    pageDocument.getElementById("upcoming-updates-link").text() shouldBe "View your upcoming updates"
    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2023 to 2024 tax year, you would have to report quarterly from 6 April 2025."
    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

  }

}
