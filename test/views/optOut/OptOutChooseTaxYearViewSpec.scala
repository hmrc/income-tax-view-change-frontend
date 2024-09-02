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

import config.FrontendAppConfig
import forms.optOut.ConfirmOptOutMultiTaxYearChoiceForm
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.reportingfreq.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import testUtils.TestSupport
import views.html.optOut.OptOutChooseTaxYear

class OptOutChooseTaxYearViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val optOutChooseTaxYearView: OptOutChooseTaxYear = app.injector.instanceOf[OptOutChooseTaxYear]
  //test data needs to be added
  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val availableOptOutTaxYear: Seq[TaxYear] = Seq(taxYear)
  val availableOptOutTaxYearsList: List[String] = List(taxYear.toString)
  val submissionsCountForTaxYearModel: QuarterlyUpdatesCountForTaxYearModel =
    QuarterlyUpdatesCountForTaxYearModel(Seq(QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2024), 6)))

  class Setup(isAgent: Boolean = true) {
    val cancelURL = if (isAgent) controllers.routes.NextUpdatesController.showAgent.url else controllers.routes.NextUpdatesController.show().url
    val pageDocument: Document = Jsoup.parse(contentAsString(optOutChooseTaxYearView(ConfirmOptOutMultiTaxYearChoiceForm(availableOptOutTaxYearsList), availableOptOutTaxYear, submissionsCountForTaxYearModel, isAgent, cancelURL)))
  }

  object optOutChooseTaxYear {
    val heading: String = messages("optout.chooseOptOutTaxYear.heading")
    val title: String = messages("htmlTitle", heading)
    val summary1: String = messages("optout.chooseOptOutTaxYear.desc1")
    val summary2: String = messages("optout.chooseOptOutTaxYear.desc2")
    val whichTaxYear: String = messages("optout.chooseOptOutTaxYear.whichTaxYear")
    val confirmOptOutURL: String = controllers.optOut.routes.ConfirmOptOutController.show(isAgent = false).url
    val confirmOptOutURLAgent: String = controllers.optOut.routes.ConfirmOptOutController.show(isAgent = true).url
    val cancelButton: String = messages("optout.chooseOptOutTaxYear.cancel")
    val cancelButtonHref: String = controllers.routes.NextUpdatesController.show().url
    val cancelButtonAgentHref: String = controllers.routes.NextUpdatesController.showAgent.url
    val continueButton: String = messages("optout.chooseOptOutTaxYear.continue")
  }

  "Opt-out confirm page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe optOutChooseTaxYear.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe optOutChooseTaxYear.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("description1").text() shouldBe optOutChooseTaxYear.summary1
      pageDocument.getElementById("description2").text() shouldBe optOutChooseTaxYear.summary2
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe optOutChooseTaxYear.whichTaxYear
      pageDocument.getElementById("cancel-button").text() shouldBe optOutChooseTaxYear.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe optOutChooseTaxYear.cancelButtonHref
      pageDocument.getElementById("continue-button").text() shouldBe optOutChooseTaxYear.continueButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("description1").text() shouldBe optOutChooseTaxYear.summary1
      pageDocument.getElementById("description2").text() shouldBe optOutChooseTaxYear.summary2
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe optOutChooseTaxYear.whichTaxYear
      pageDocument.getElementById("cancel-button").text() shouldBe optOutChooseTaxYear.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe optOutChooseTaxYear.cancelButtonAgentHref
      pageDocument.getElementById("continue-button").text() shouldBe optOutChooseTaxYear.continueButton
    }

  }
}
