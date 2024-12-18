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

package views.nextUpdates

import config.FrontendAppConfig
import models.incomeSourceDetails.TaxYear
import models.obligations._
import models.optout.{NextUpdatesQuarterlyReportingContentChecks, OptOutMultiYearViewModel, OptOutOneYearViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import services.optout.{OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated}
import testConstants.BusinessDetailsTestConstants.{business1, testTradeName}
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import testUtils.TestSupport
import views.html.nextUpdates.NextUpdatesOptOut

import java.time.LocalDate

class NextUpdatesOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val nextUpdatesView: NextUpdatesOptOut = app.injector.instanceOf[NextUpdatesOptOut]

  class Setup(currentObligations: NextUpdatesViewModel, quarterlyUpdateContentShow: Boolean = true, isSupportingAgent: Boolean = false) {

    val checks: NextUpdatesQuarterlyReportingContentChecks = if (quarterlyUpdateContentShow) NextUpdatesQuarterlyReportingContentChecks(
      currentYearItsaStatus = true,
      previousYearItsaStatus = true,
      previousYearCrystallisedStatus = true)
    else
      NextUpdatesQuarterlyReportingContentChecks(
        currentYearItsaStatus = false,
        previousYearItsaStatus = true,
        previousYearCrystallisedStatus = true)

    val optOutOneYearViewModel: OptOutOneYearViewModel = OptOutOneYearViewModel(TaxYear.forYearEnd(2024), Some(OneYearOptOutFollowedByAnnual))
    val pageDocument: Document = Jsoup.parse(contentAsString(
      nextUpdatesView(currentObligations, Some(optOutOneYearViewModel), checks, "testBackURL", isSupportingAgent = isSupportingAgent)
    ))

    val optOutMultiYearViewModel: OptOutMultiYearViewModel = OptOutMultiYearViewModel()
    val pageDocumentMultiYearOptOut: Document = Jsoup.parse(contentAsString(
      nextUpdatesView(currentObligations, Some(optOutMultiYearViewModel), checks, "testBackURL", isSupportingAgent = isSupportingAgent)
    ))

    val optOutOneYearViewModelWithMandated = optOutOneYearViewModel.copy(state = Some(OneYearOptOutFollowedByMandated))
    val pageDocumentWithWarning: Document = Jsoup.parse(contentAsString(
      nextUpdatesView(currentObligations, Some(optOutOneYearViewModelWithMandated), checks, "testBackURL", isSupportingAgent = isSupportingAgent)
    ))
  }

  object obligationsMessages {
    val heading: String = messages("nextUpdates.heading")
    val title: String = messages("htmlTitle", heading)
    val summary: String = messages("nextUpdates.dropdown.info")
    val summaryQuarterly: String = messages("obligations.quarterlyUpdates")
    val quarterlyLine1: String = messages("nextUpdates.dropdown.quarterlyReturn.text")
    val quarterlyLine2: String = messages("nextUpdates.dropdown.quarterlyReturn.text.lin2")
    val declarationLine1: String = messages("nextUpdates.dropdown.finalDeclaration.text")
    val summaryDeclaration: String = messages("obligations.finalDeclarationUpdate")
    val updatesInSoftware: String = messages("nextUpdates.updates.software.heading")
    val updatesInSoftwareDesc: String = s"${messages("nextUpdates.updates.software.dec1")} ${messages("nextUpdates.updates.software.dec2")} ${messages("pagehelp.opensInNewTabText")} ${messages("nextUpdates.updates.software.dec3")}"
    val info: String = s"${messages("nextUpdates.previousYears.textOne")} ${messages("nextUpdates.previousYears.link")} ${messages("nextUpdates.previousYears.textTwo")}"
    val oneYearOptOutMessage: String = s"${messages("nextUpdates.optOutOneYear-1", "2023", "2024")} ${messages("nextUpdates.optOutOneYear-2")}"
    val multiYearOptOutMessage: String = s"${messages("nextUpdates.optOutMultiYear-1")} ${messages("nextUpdates.optOutMultiYear-2")}"
  }

  val confirmOptOutLink = "/report-quarterly/income-and-expenses/view/optout/review-confirm-taxyear"
  val singleYearOptOutWarningLink = "/report-quarterly/income-and-expenses/view/optout/single-taxyear-warning"
  val multiYearOptOutLink = "/report-quarterly/income-and-expenses/view/optout/choose-taxyear"

  lazy val obligationsModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
    business1.incomeSourceId,
    twoObligationsSuccessModel.obligations
  ))).obligationsByDate.map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
  })

  "Next Updates page" should {

    "have the correct title" in new Setup(obligationsModel) {
      pageDocument.title() shouldBe obligationsMessages.title
    }

    "have the correct heading" in new Setup(obligationsModel) {
      pageDocument.select("h1").text() shouldBe obligationsMessages.heading
    }

    "have the correct summary heading" in new Setup(obligationsModel) {
      pageDocument.select("summary").text() shouldBe obligationsMessages.summary
    }

    "have a summary section for quarterly updates" in new Setup(obligationsModel) {
      pageDocument.select("details h2").get(0).text() shouldBe obligationsMessages.summaryQuarterly
    }

    "have the correct details for quarterly updates section" in new Setup(obligationsModel) {
      pageDocument.getElementById("quarterly-dropdown-line1").text() shouldBe obligationsMessages.quarterlyLine1
      pageDocument.getElementById("quarterly-dropdown-line2").text() shouldBe obligationsMessages.quarterlyLine2
    }

    "don't show quarterly updates section" in new Setup(obligationsModel, quarterlyUpdateContentShow = false) {
      pageDocument.select("#quarterly-dropdown-line1").isEmpty shouldBe true
      pageDocument.select("#quarterly-dropdown-line2").isEmpty shouldBe true
    }


    "have a summary section for final declarations" in new Setup(obligationsModel) {
      pageDocument.select("details h2").get(1).text() shouldBe obligationsMessages.summaryDeclaration
    }

    "have the correct line 1 for final declaration section" in new Setup(obligationsModel) {
      pageDocument.getElementById("final-declaration-line1").text() shouldBe obligationsMessages.declarationLine1
    }

    "have an updates accordion" in new Setup(obligationsModel) {
      pageDocument.select("div .govuk-accordion").size() == 1
    }

    s"have the information ${obligationsMessages.info}" when {
      "a primary agent or individual" in new Setup(obligationsModel) {
        pageDocument.select("p:nth-child(6)").text shouldBe obligationsMessages.info
        pageDocument.select("p:nth-child(6) a").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }
    }

    s"not have the information ${obligationsMessages.info}" when {
      "a supporting agent" in new Setup(obligationsModel, isSupportingAgent = true) {
        pageDocument.body.text() shouldNot include(obligationsMessages.info)
      }
    }

    s"have the correct TradeName" in new Setup(obligationsModel) {
      val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")

      val table = section.select(".govuk-table")

      table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe messages("nextUpdates.quarterly")
      table.select(".govuk-table__cell:nth-of-type(2)").text() shouldBe messages(testTradeName)
    }

    s"have the Submitting updates in software" in new Setup(obligationsModel) {
      pageDocument.getElementById("updates-software-heading").text() shouldBe obligationsMessages.updatesInSoftware
      pageDocument.getElementById("updates-software-link").text() shouldBe obligationsMessages.updatesInSoftwareDesc
    }

    s"don't show the Submitting updates in software section" in new Setup(obligationsModel, quarterlyUpdateContentShow = false) {
      pageDocument.select("#updates-software-heading").isEmpty shouldBe true
      pageDocument.select("#updates-software-link").isEmpty shouldBe true
    }

    "have the one year opt out message" in new Setup(obligationsModel) {
      pageDocument.getElementById("one-year-opt-out-message").text() shouldBe obligationsMessages.oneYearOptOutMessage
    }

    "have the confirm opt out link" in new Setup(obligationsModel) {
      pageDocument.getElementById("confirm-opt-out-link").attr("href") shouldBe confirmOptOutLink
    }

    "have the single year opt out warning link" in new Setup(obligationsModel) {
      pageDocumentWithWarning.getElementById("single-year-opt-out-warning-link").attr("href") shouldBe singleYearOptOutWarningLink
    }

    "have the multi year opt out message" in new Setup(obligationsModel) {
      pageDocumentMultiYearOptOut.getElementById("multi-year-opt-out-message").text() shouldBe obligationsMessages.multiYearOptOutMessage
    }

    "have the multi year opt out message link" in new Setup(obligationsModel) {
      pageDocumentMultiYearOptOut.getElementById("opt-out-link").attr("href") shouldBe multiYearOptOutLink
    }
  }
}
