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
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  class Setup(isAgent: Boolean = true) {
    val model: OptInCompletedViewModel = OptInCompletedViewModel(isAgent = isAgent, optInTaxYear = currentTaxYear)
    val pageDocument: Document = Jsoup.parse(contentAsString(optInCompletedCurrentYearView(model = model)))
  }

  "have the correct title" in new Setup(false) {
    pageDocument.title() shouldBe OptInCompletedCurrentYearViewSpec.title

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe "Opt In completed"
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You are now reporting quarterly from 2022 to 2023 tax year onwards"
  }

}
