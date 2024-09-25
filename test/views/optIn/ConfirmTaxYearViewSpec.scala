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
import models.optin.ConfirmTaxYearViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.ConfirmTaxYear

class ConfirmTaxYearViewSpec extends TestSupport {

  val view: ConfirmTaxYear = app.injector.instanceOf[ConfirmTaxYear]

  val forYearEnd = 2023
  val taxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)

  class Setup(isAgent: Boolean = true, intent: TaxYear) {
    private val cancelURL = if (isAgent) controllers.routes.ReportingFrequencyPageController.show(true).url else
      controllers.routes.ReportingFrequencyPageController.show(false).url
    private val model = ConfirmTaxYearViewModel(availableOptInTaxYear = intent, isAgent = isAgent,
      cancelURL = cancelURL)
    val pageDocument: Document = Jsoup.parse(contentAsString(view(model)))
  }

  object optInChooseTaxYear {
    val heading: String = messages("optIn.confirmTaxYear.heading", "2022", "2023")

    val title: String = messages("htmlTitle", heading)

    val titleAgent: String = messages("htmlTitle.agent", heading)

    val desc: String = messages("optIn.confirmTaxYear.desc")

    val text: String = messages("optIn.confirmTaxYear.text")

    val confirmButton: String = messages("optIn.confirmTaxYear.confirmSaveBtn")

    val cancelButton: String = messages("optIn.confirmTaxYear.cancel")
  }

  def runTest(availableOptInTaxYear: TaxYear, isAgent: Boolean): Unit = {

    "have the correct title" in new Setup(isAgent, availableOptInTaxYear) {
      if (isAgent) pageDocument.title() shouldBe optInChooseTaxYear.titleAgent else pageDocument.title() shouldBe optInChooseTaxYear.title
    }

    "have the correct heading" in new Setup(isAgent, availableOptInTaxYear) {
      pageDocument.select("h1").text() shouldBe optInChooseTaxYear.heading
    }

    "have the correct summary heading and page contents" in new Setup(isAgent, availableOptInTaxYear) {
      pageDocument.getElementById("confirm-tax-year-desc").text() shouldBe optInChooseTaxYear.desc
      pageDocument.getElementById("insetText_confirmYear").text() shouldBe optInChooseTaxYear.text

      pageDocument.getElementById("confirm-button").text() shouldBe optInChooseTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
    }

  }

  "run test for individual" should {
    runTest(availableOptInTaxYear = taxYear, isAgent = false)
  }

  "run test for agent" should {
    runTest(availableOptInTaxYear = taxYear, isAgent = true)
  }

}
