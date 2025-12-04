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

package views.optOut.oldJourney

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.optout.MultiYearOptOutCheckpointViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testConstants.BaseTestConstants.taxYear
import testUtils.TestSupport
import views.html.optOut.oldJourney.CheckOptOutAnswersView

class CheckOptOutAnswersViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val optOutCheckAnswers: CheckOptOutAnswersView = app.injector.instanceOf[CheckOptOutAnswersView]

  val checkAnswersViewModel: MultiYearOptOutCheckpointViewModel = MultiYearOptOutCheckpointViewModel(TaxYear.forYearEnd(taxYear))
  val intentStartTaxYear: String = checkAnswersViewModel.startYear
  val intentEndTaxYear: String = checkAnswersViewModel.endYear

  class Setup(isAgent: Boolean = true) {
    val cancelURL: String = if (isAgent) controllers.routes.NextUpdatesController.showAgent().url else controllers.routes.NextUpdatesController.show().url
    val pageDocument: Document = Jsoup.parse(contentAsString(optOutCheckAnswers(checkAnswersViewModel, isAgent, cancelURL)))
  }

  object checkOptOutAnswers {
    val heading: String = "Check your answers"
    val title: String = s"$heading - Manage your Self Assessment - GOV.UK"
    val optOutTable: String = "Opt out from"
    val optOutTableTaxYears: String = "2019 to 2020 tax year onwards"
    val optOutTableChange: String = "Change"
    val paragraph1: String = "If you opt out, you can submit your tax return through your HMRC online account or compatible software."
    val paragraph2: String = "In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."
    val confirmButton: String = "Confirm and save"
    val cancelButton: String = "Cancel"
    val cancelButtonHref: String = controllers.routes.NextUpdatesController.show().url
    val cancelButtonAgentHref: String = controllers.routes.NextUpdatesController.showAgent().url

    val changeOptOut: String = controllers.optOut.oldJourney.routes.OptOutChooseTaxYearController.show(isAgent = false).url
    val changeOptOutAgent: String = controllers.optOut.oldJourney.routes.OptOutChooseTaxYearController.show(isAgent = true).url

    val confirmOptOutURL: String = controllers.optOut.routes.ConfirmedOptOutController.show(isAgent = false).url
    val confirmOptOutURLAgent: String = controllers.optOut.routes.ConfirmedOptOutController.show(isAgent = true).url
  }


  "Opt-out confirm page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe checkOptOutAnswers.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe checkOptOutAnswers.heading
    }

    "render the summary list of opt out years" in new Setup(false) {
      pageDocument.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe checkOptOutAnswers.optOutTable
      pageDocument.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe checkOptOutAnswers.optOutTableTaxYears
      pageDocument.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe checkOptOutAnswers.optOutTableChange
      pageDocument.getElementById("change").attr("href") shouldBe checkOptOutAnswers.changeOptOut

    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("optOut-summary").text() shouldBe checkOptOutAnswers.paragraph1
      pageDocument.getElementById("optOut-warning").text() shouldBe checkOptOutAnswers.paragraph2

      pageDocument.getElementById("confirm-button").text() shouldBe checkOptOutAnswers.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe checkOptOutAnswers.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe checkOptOutAnswers.cancelButtonHref
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("optOut-summary").text() shouldBe checkOptOutAnswers.paragraph1
      pageDocument.getElementById("optOut-warning").text() shouldBe checkOptOutAnswers.paragraph2

      pageDocument.getElementById("confirm-button").text() shouldBe checkOptOutAnswers.confirmButton
      pageDocument.getElementById("cancel-button").text() shouldBe checkOptOutAnswers.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe checkOptOutAnswers.cancelButtonAgentHref
    }

  }
}
