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
import services.QuarterlyUpdatesCountForTaxYear
import services.reportingFrequency.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import testUtils.TestSupport
import views.html.optOut.OptOutChooseTaxYear
import views.messages.OptOutChooseTaxYear._

class OptOutChooseTaxYearViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val OptOutChooseTaxYearView: OptOutChooseTaxYear = app.injector.instanceOf[OptOutChooseTaxYear]

  val confirmOptOutURL: String = controllers.optOut.routes.ConfirmOptOutController.show(isAgent = false).url
  val confirmOptOutURLAgent: String = controllers.optOut.routes.ConfirmOptOutController.show(isAgent = true).url

  val cancelButtonHref: String = controllers.routes.NextUpdatesController.show().url
  val cancelButtonAgentHref: String = controllers.routes.NextUpdatesController.showAgent().url

  val quarterlyFulfilledUpdatesCount = 2

  val taxYear: TaxYear = TaxYear.forYearEnd(2024)
  val availableOptOutTaxYear: Seq[TaxYear] = Seq(taxYear)
  val availableOptOutTaxYearsList: List[String] = List(taxYear.toString)

  val submissionsCountForTaxYearModel: QuarterlyUpdatesCountForTaxYearModel =
    QuarterlyUpdatesCountForTaxYearModel(Seq(QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2024), 6)))
  val submissionsCountEmpty: QuarterlyUpdatesCountForTaxYearModel =
    QuarterlyUpdatesCountForTaxYearModel(Seq())


  class Setup(isAgent: Boolean = true) {

    val cancelURL: String = if (isAgent) controllers.routes.NextUpdatesController.showAgent().url else controllers.routes.NextUpdatesController.show().url
    val pageDocument: Document = Jsoup.parse(
      contentAsString(
        OptOutChooseTaxYearView(
          form = ConfirmOptOutMultiTaxYearChoiceForm(availableOptOutTaxYearsList),
          availableOptOutTaxYear = availableOptOutTaxYear,
          submissionCounts = submissionsCountForTaxYearModel,
          quarterlyFulfilledUpdatesCount = quarterlyFulfilledUpdatesCount,
          isAgent = isAgent,
          cancelURL = cancelURL
        )
      )
    )
  }

  class SetupNoSubmissions(isAgent: Boolean = true) extends Setup {
    override val pageDocument: Document =
      Jsoup.parse(
        contentAsString(
          OptOutChooseTaxYearView(
            form = ConfirmOptOutMultiTaxYearChoiceForm(availableOptOutTaxYearsList),
            availableOptOutTaxYear = availableOptOutTaxYear,
            submissionCounts = submissionsCountEmpty,
            quarterlyFulfilledUpdatesCount = 0,
            isAgent = isAgent,
            cancelURL = cancelURL
          )
        )
      )
  }

  "Opt-out confirm page (with submissions)" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe heading
    }

    "show the warning block (for already having done submissions) user: non-agent" in new Setup(false) {
      pageDocument.getElementById("warning-inset").text shouldBe warningInsertMessage(quarterlyFulfilledUpdatesCount)
    }

    "show the warning block (for already having done submissions) user: agent" in new Setup(true) {
      pageDocument.getElementById("warning-inset").text shouldBe warningInsertMessage(quarterlyFulfilledUpdatesCount)
    }

    "have the correct summary heading and page contents" in new Setup(false) {

      pageDocument.getElementById("description").text() shouldBe summary
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe whichTaxYear
      pageDocument.getElementById("cancel-button").text() shouldBe cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelButtonHref
      pageDocument.getElementById("continue-button").text() shouldBe continueButton
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {

      pageDocument.getElementById("description").text() shouldBe summary
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe whichTaxYear
      pageDocument.getElementById("cancel-button").text() shouldBe cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe cancelButtonAgentHref
      pageDocument.getElementById("continue-button").text() shouldBe continueButton
    }
  }

  "Opt-out confirm page (without submissions)" should {

    "not show the warning block (for no submissions) user: non-agent" in new SetupNoSubmissions(false) {
      Option(pageDocument.getElementById("warning-inset")) shouldBe None
    }

    "not show the warning block (for no submissions) user: agent" in new SetupNoSubmissions(true) {
      Option(pageDocument.getElementById("warning-inset")) shouldBe None
    }
  }
}

