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

package views.optIn.oldJourney

import models.incomeSourceDetails.TaxYear
import models.optin.ConfirmTaxYearViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.oldJourney.ConfirmTaxYearView

class ConfirmTaxYearViewSpec extends TestSupport {

  val view: ConfirmTaxYearView = app.injector.instanceOf[ConfirmTaxYearView]

  val forCurrentYearEnd = 2025
  val currentTaxYear: TaxYear = TaxYear.forYearEnd(forCurrentYearEnd)

  val forNextYearEnd = 2026
  val nextTaxYear: TaxYear = TaxYear.forYearEnd(forNextYearEnd)

  class Setup(isAgent: Boolean = true, intent: TaxYear, isNextTaxYear: Boolean) {

    private val cancelURL = controllers.routes.ReportingFrequencyPageController.show(isAgent).url

    private val currentYearModel =
      ConfirmTaxYearViewModel(
        availableOptInTaxYear = intent,
        isNextTaxYear = isNextTaxYear,
        isAgent = isAgent,
        cancelURL = cancelURL
      )
    val pageDocument: Document = Jsoup.parse(contentAsString(view(currentYearModel)))
  }

  object optInConfirmCurrentTaxYear {
    val heading: String = messages("optIn.confirmTaxYear.heading", "2024", "2025")
    val title: String = messages("htmlTitle", heading)
    val titleAgent: String = messages("htmlTitle.agent", heading)
    val desc: String = messages("optIn.confirmTaxYear.desc")
    val text: String = messages("optIn.confirmTaxYear.text")
    val confirmButton: String = messages("optIn.confirmTaxYear.confirmSaveBtn")
    val cancelButton: String = messages("optIn.confirmTaxYear.cancel")
  }

  object optInConfirmNextTaxYear {
    val heading: String = messages("optIn.confirmNextTaxYear.heading", "2025", "2026")
    val title: String = messages("htmlTitle", heading)
    val titleAgent: String = messages("htmlTitle.agent", heading)
    val desc: String = messages("optIn.confirmNextTaxYear.desc", "6 April " + forCurrentYearEnd)
    val confirmButton: String = messages("optIn.confirmTaxYear.confirmSaveBtn")
    val cancelButton: String = messages("optIn.confirmTaxYear.cancel")
  }

  def runCurrentYearTest(availableOptInTaxYear: TaxYear, isAgent: Boolean): Unit = {

    "have the correct title" in new Setup(isAgent, availableOptInTaxYear, isNextTaxYear = false) {
      if (isAgent) pageDocument.title() shouldBe optInConfirmCurrentTaxYear.titleAgent else pageDocument.title() shouldBe optInConfirmCurrentTaxYear.title
    }

    "have the correct heading" in new Setup(isAgent, availableOptInTaxYear, isNextTaxYear = false) {
      pageDocument.select("h1").text() shouldBe optInConfirmCurrentTaxYear.heading
    }

    "have the correct summary heading and page contents" in new Setup(isAgent, availableOptInTaxYear, isNextTaxYear = false) {
      pageDocument.getElementById("confirm-tax-year-desc").text() shouldBe optInConfirmCurrentTaxYear.desc
      pageDocument.getElementById("insetText_confirmYear").text() shouldBe optInConfirmCurrentTaxYear.text

      pageDocument.getElementById("confirm-button").text() shouldBe optInConfirmCurrentTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInConfirmCurrentTaxYear.cancelButton
    }

  }

  def runNextYearTest(availableOptInTaxYear: TaxYear, isAgent: Boolean): Unit = {

    "have the correct title for next tax year" in new Setup(isAgent, availableOptInTaxYear, isNextTaxYear = true) {
      if (isAgent) pageDocument.title() shouldBe optInConfirmNextTaxYear.titleAgent else pageDocument.title() shouldBe optInConfirmNextTaxYear.title
    }

    "have the correct heading for next tax year" in new Setup(isAgent, availableOptInTaxYear, isNextTaxYear = true) {
      pageDocument.select("h1").text() shouldBe optInConfirmNextTaxYear.heading
    }

    "have the correct summary heading and page contents for next tax year" in new Setup(isAgent, availableOptInTaxYear, isNextTaxYear = true) {
      pageDocument.getElementById("confirm-tax-year-desc").text() shouldBe optInConfirmNextTaxYear.desc

      pageDocument.getElementById("confirm-button").text() shouldBe optInConfirmNextTaxYear.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInConfirmNextTaxYear.cancelButton
    }

  }

  "run test for individual" should {
    runCurrentYearTest(availableOptInTaxYear = currentTaxYear, isAgent = false)
    runNextYearTest(availableOptInTaxYear = nextTaxYear, isAgent = false)
  }

  "run test for agent" should {
    runCurrentYearTest(availableOptInTaxYear = currentTaxYear, isAgent = true)
    runNextYearTest(availableOptInTaxYear = nextTaxYear, isAgent = true)
  }

}
