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

package views.optOut

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.optout.OptOutMultiYearViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers._
import testConstants.BaseTestConstants.taxYear
import testUtils.TestSupport
import views.html.optOut.CheckOptOutAnswers

class CheckOptOutAnswersViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val optOutCheckAnswers: CheckOptOutAnswers = app.injector.instanceOf[CheckOptOutAnswers]

  val checkAnswersViewModel: OptOutMultiYearViewModel = OptOutMultiYearViewModel(TaxYear.forYearEnd(taxYear))
  val intentStartTaxYear: String = checkAnswersViewModel.intent.startYear.toString
  val intentEndTaxYear: String = checkAnswersViewModel.intent.endYear.toString
  class Setup(isAgent: Boolean = true) {
    val postAction: Call = controllers.optOut.routes.ConfirmOptOutController.submit(isAgent)
    val pageDocument: Document = Jsoup.parse(contentAsString(optOutCheckAnswers(checkAnswersViewModel ,postAction, isAgent)))
  }

  object checkOptOutAnswers {
    val heading: String = messages("optout.checkAnswers.heading")
    val title: String = messages("htmlTitle", heading)
    val optOutTable: String = messages("optout.checkAnswers.optOut")
    val optOutTableTaxYears: String = messages("optout.checkAnswers.taxYears", intentStartTaxYear, intentEndTaxYear)
    val optOutTableChange: String = messages("optout.checkAnswers.change")
    val paragraph1: String = messages("optout.checkAnswers.p1")
    val paragraph2: String = messages("optout.checkAnswers.p2")
    val confirmButton: String = messages("optout.checkAnswers.confirm")
    val cancelButton: String = messages("optout.checkAnswers.cancel")

    val changeOptOut: String = controllers.optOut.routes.OptOutChooseTaxYearController.show().url
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
    }

  }
}
