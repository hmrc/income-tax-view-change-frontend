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
import models.admin.{OptOutFs, ReportingFrequencyPage}
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
  val submissionsCountEmpty: QuarterlyUpdatesCountForTaxYearModel =
    QuarterlyUpdatesCountForTaxYearModel(Seq())

  class Setup(isAgent: Boolean = true) {
    val cancelURL = if (isAgent) controllers.routes.NextUpdatesController.showAgent().url else controllers.routes.NextUpdatesController.show().url
    val pageDocument: Document = Jsoup.parse(contentAsString(optOutChooseTaxYearView(ConfirmOptOutMultiTaxYearChoiceForm(availableOptOutTaxYearsList), availableOptOutTaxYear, submissionsCountForTaxYearModel, isAgent, cancelURL)))
  }
  class SetupNoSubmissions(isAgent: Boolean = true) extends Setup{
    //This is for tests without previous tax submissions
    override val pageDocument: Document = Jsoup.parse(contentAsString(optOutChooseTaxYearView(ConfirmOptOutMultiTaxYearChoiceForm(availableOptOutTaxYearsList), availableOptOutTaxYear, submissionsCountEmpty, isAgent, cancelURL)))
  }

  object optOutChooseTaxYear {
    val heading: String = messages("optout.chooseOptOutTaxYear.heading")
    val title: String = messages("htmlTitle", heading)
    val summary: String = messages("optout.chooseOptOutTaxYear.desc")
    val whichTaxYear: String = messages("optout.chooseOptOutTaxYear.whichTaxYear")
    val confirmOptOutURL: String =
      if(areAllEnabled(OptOutFs, ReportingFrequencyPage)) controllers.optOutNew.routes.ConfirmOptOutUpdateController.show(isAgent = false, taxYear.toString).url
      else controllers.optOut.routes.ConfirmOptOutController.show(isAgent = false).url
    val confirmOptOutURLAgent: String =
      if(areAllEnabled(OptOutFs, ReportingFrequencyPage)) controllers.optOutNew.routes.ConfirmOptOutUpdateController.show(isAgent = true, taxYear = taxYear.toString).url
      else controllers.optOut.routes.ConfirmOptOutController.show(isAgent = true).url
    val cancelButton: String = messages("optout.chooseOptOutTaxYear.cancel")
    val cancelButtonHref: String = controllers.routes.NextUpdatesController.show().url
    val cancelButtonAgentHref: String = controllers.routes.NextUpdatesController.showAgent().url
    val continueButton: String = messages("optout.chooseOptOutTaxYear.continue")
    val warningInsertMessage: String = messages("optout.chooseOptOutTaxYear.submissions.deleted")
    val warningInsertClass: String = "govuk-inset-text"
  }

  "Opt-out confirm page (with submissions)" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe optOutChooseTaxYear.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe optOutChooseTaxYear.heading
    }

    "show the warning block (for already having done submissions) user: non-agent" in new Setup(false) {
      pageDocument.getElementById("warning-inset").text shouldBe optOutChooseTaxYear.warningInsertMessage
      pageDocument.getElementById("warning-inset").className shouldBe optOutChooseTaxYear.warningInsertClass
    }

    "show the warning block (for already having done submissions) user: agent" in new Setup(true) {
      pageDocument.getElementById("warning-inset").text shouldBe optOutChooseTaxYear.warningInsertMessage
      pageDocument.getElementById("warning-inset").className shouldBe optOutChooseTaxYear.warningInsertClass
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("description").text() shouldBe optOutChooseTaxYear.summary
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe optOutChooseTaxYear.whichTaxYear
      pageDocument.getElementById("cancel-button").text() shouldBe optOutChooseTaxYear.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe optOutChooseTaxYear.cancelButtonHref
      pageDocument.getElementById("continue-button").text() shouldBe optOutChooseTaxYear.continueButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("description").text() shouldBe optOutChooseTaxYear.summary
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe optOutChooseTaxYear.whichTaxYear
      pageDocument.getElementById("cancel-button").text() shouldBe optOutChooseTaxYear.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe optOutChooseTaxYear.cancelButtonAgentHref
      pageDocument.getElementById("continue-button").text() shouldBe optOutChooseTaxYear.continueButton
    }

  }

  "Opt-out confirm page (without submissions)" should {

    "don't show the warning block (for no submissions) user: non-agent" in new SetupNoSubmissions(false) {
      Option(pageDocument.getElementById("warning-inset")) shouldBe None
    }

    "don't show the warning block (for no submissions) user: agent" in new SetupNoSubmissions(true) {
      Option(pageDocument.getElementById("warning-inset")) shouldBe None
    }
  }
}

