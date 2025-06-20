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

import auth.MtdItUser
import config.FrontendAppConfig
import models.admin.{FeatureSwitch, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import models.obligations._
import models.optout.{NextUpdatesQuarterlyReportingContentChecks, OptOutMultiYearViewModel, OptOutOneYearViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.optout.{OneYearOptOutFollowedByAnnual, OneYearOptOutFollowedByMandated}
import testConstants.BusinessDetailsTestConstants.{business1, testTradeName}
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import testUtils.TestSupport
import views.html.components.link
import views.html.nextUpdates.NextUpdatesOptOut

import java.time.LocalDate

class NextUpdatesOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def nextUpdatesView: NextUpdatesOptOut = app.injector.instanceOf[NextUpdatesOptOut]

  val linkComponent: link = app.injector.instanceOf[link]

  class Setup(quarterlyUpdateContentShow: Boolean = true, isSupportingAgent: Boolean = false, reportingFrequencyPageFsEnabled: Boolean = true) {

    val user: MtdItUser[_] =
      getIndividualUser(FakeRequest())
        .addFeatureSwitches(List(
          FeatureSwitch(ReportingFrequencyPage, reportingFrequencyPageFsEnabled)
        ))

    val checks: NextUpdatesQuarterlyReportingContentChecks =
      if (quarterlyUpdateContentShow) NextUpdatesQuarterlyReportingContentChecks(
        currentYearItsaStatus = true,
        previousYearItsaStatus = true,
        previousYearCrystallisedStatus = true)
      else
        NextUpdatesQuarterlyReportingContentChecks(
          currentYearItsaStatus = false,
          previousYearItsaStatus = true,
          previousYearCrystallisedStatus = true)


    val optOutOneYearViewModel: OptOutOneYearViewModel =
      OptOutOneYearViewModel(TaxYear.forYearEnd(2024), Some(OneYearOptOutFollowedByAnnual))

    val optOutOneYearViewModelWithMandated: OptOutOneYearViewModel = optOutOneYearViewModel.copy(state = Some(OneYearOptOutFollowedByMandated))

    val optOutMultiYearViewModel: OptOutMultiYearViewModel =
      OptOutMultiYearViewModel()

    lazy val obligationsModel: NextUpdatesViewModel =
      NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
        business1.incomeSourceId,
        twoObligationsSuccessModel.obligations
      ))).obligationsByDate(user).map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
        DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
      })

    def oneYearOptOutAnnualView: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          obligationsModel,
          checks,
          "testBackURL",
          isSupportingAgent = isSupportingAgent,
          reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(false).url,
          reportingFrequencyEnabled = reportingFrequencyPageFsEnabled
        )(implicitly, user)
      ))

    def pageDocumentWithReportingContent: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          currentObligations = obligationsModel,
          checks = checks,
          backUrl = "testBackURL",
          isSupportingAgent = isSupportingAgent,
          reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(false).url,
          reportingFrequencyEnabled = reportingFrequencyPageFsEnabled
        )(implicitly, user)
      ))

    def pageDocumentWithWarning: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          currentObligations = obligationsModel,
          checks = checks,
          backUrl = "testBackURL",
          isSupportingAgent = isSupportingAgent,
          reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(false).url,
          reportingFrequencyEnabled = reportingFrequencyPageFsEnabled
        )(implicitly, user)
      ))

    def pageDocumentWithWarningWithReportingContent: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          currentObligations = obligationsModel,
          checks = checks,
          backUrl = "testBackURL",
          isSupportingAgent = isSupportingAgent,
          reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(false).url,
          reportingFrequencyEnabled = reportingFrequencyPageFsEnabled
        )(implicitly, user)
      ))


    def pageDocumentMultiYearOptOut: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          currentObligations = obligationsModel,
          checks = checks,
          backUrl = "testBackURL",
          isSupportingAgent = isSupportingAgent,
          reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(false).url,
          reportingFrequencyEnabled = reportingFrequencyPageFsEnabled
        )(implicitly, user)
      ))
  }

  //TODO: Move this object out to help clean up this file and modularise a little
  object ObligationsMessages {
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
    val oneYearOptOutMessage: String = s"${messages("nextUpdates.optOutOneYear.p.message", "2023", "2024")} ${messages("nextUpdates.optOutOneYear.p.link")}"
    val reportingObligationsLink: String = "Depending on your circumstances, you may be able to view and change your reporting obligations."
  }

  val confirmOptOutLink = "/report-quarterly/income-and-expenses/view/optout/review-confirm-taxyear"
  val reportingFrequencyLink = "/report-quarterly/income-and-expenses/view/reporting-frequency"

  "NextUpdatesOptOut view" when {

    "oneYearOptOutAnnualView" should {

      "have the correct title" in new Setup() {
        oneYearOptOutAnnualView.title() shouldBe ObligationsMessages.title
      }

      "have the correct heading" in new Setup() {
        oneYearOptOutAnnualView.select("h1").text() shouldBe ObligationsMessages.heading
      }

      "have the correct summary heading" in new Setup() {
        oneYearOptOutAnnualView.select("summary").text() shouldBe ObligationsMessages.summary
      }

      "have a summary section for quarterly updates" in new Setup() {
        oneYearOptOutAnnualView.select("details h2").get(0).text() shouldBe ObligationsMessages.summaryQuarterly
      }

      "have the correct details for quarterly updates section" in new Setup() {
        oneYearOptOutAnnualView.getElementById("quarterly-dropdown-line1").text() shouldBe ObligationsMessages.quarterlyLine1
        oneYearOptOutAnnualView.getElementById("quarterly-dropdown-line2").text() shouldBe ObligationsMessages.quarterlyLine2
      }

      "don't show quarterly updates section" in new Setup(quarterlyUpdateContentShow = false) {
        oneYearOptOutAnnualView.select("#quarterly-dropdown-line1").isEmpty shouldBe true
        oneYearOptOutAnnualView.select("#quarterly-dropdown-line2").isEmpty shouldBe true
      }

      "have a summary section for final declarations" in new Setup() {
        oneYearOptOutAnnualView.select("details h2").get(1).text() shouldBe ObligationsMessages.summaryDeclaration
      }

      "have the correct line 1 for final declaration section" in new Setup() {
        oneYearOptOutAnnualView.getElementById("final-declaration-line1").text() shouldBe ObligationsMessages.declarationLine1
      }

      "have an updates accordion" in new Setup() {
        oneYearOptOutAnnualView.select("div .govuk-accordion").size() == 1
      }

      s"have the information ${ObligationsMessages.info}" when {
        "a primary agent or individual" in new Setup() {
          oneYearOptOutAnnualView.select("p:nth-child(6)").text shouldBe ObligationsMessages.info
          oneYearOptOutAnnualView.select("p:nth-child(6) a").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
        }
      }

      s"not have the information ${ObligationsMessages.info}" when {
        "a supporting agent" in new Setup(isSupportingAgent = true) {
          oneYearOptOutAnnualView.body.text() shouldNot include(ObligationsMessages.info)
        }
      }

      s"have the correct TradeName" in new Setup() {

        val section: Elements = oneYearOptOutAnnualView.select(".govuk-accordion__section:nth-of-type(2)")
        val table: Elements = section.select(".govuk-table")

        table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe messages("nextUpdates.quarterly")
        table.select(".govuk-table__cell:nth-of-type(2)").text() shouldBe messages(testTradeName)
      }

      s"have the Submitting updates in software" in new Setup() {
        oneYearOptOutAnnualView.getElementById("updates-software-heading").text() shouldBe ObligationsMessages.updatesInSoftware
        oneYearOptOutAnnualView.getElementById("updates-software-link").text() shouldBe ObligationsMessages.updatesInSoftwareDesc
      }

      s"don't show the Submitting updates in software section" in new Setup(quarterlyUpdateContentShow = false) {
        oneYearOptOutAnnualView.select("#updates-software-heading").isEmpty shouldBe true
        oneYearOptOutAnnualView.select("#updates-software-link").isEmpty shouldBe true
      }

      "have the one year opt out message" in new Setup() {
        oneYearOptOutAnnualView.getElementById("what-the-user-can-do").text() shouldBe ObligationsMessages.reportingObligationsLink
      }
    }
  }

  "NextUpdatesOptOut view" when {

    "Reporting Frequency feature switch is turned ON" should {

      "have the confirm opt out with reporting content and link)" in new Setup(reportingFrequencyPageFsEnabled = true) {
        enable(ReportingFrequencyPage)
        pageDocumentWithWarningWithReportingContent.getElementById("reporting-obligations-link").attr("href") shouldBe reportingFrequencyLink
      }

      "have the single year opt out with reporting content and link" in new Setup(reportingFrequencyPageFsEnabled = true) {
        enable(ReportingFrequencyPage)
        pageDocumentWithWarningWithReportingContent.getElementById("reporting-obligations-link").attr("href") shouldBe reportingFrequencyLink
      }

      "multi year scenario opt out with reporting content and link" in new Setup(reportingFrequencyPageFsEnabled = true) {
        enable(ReportingFrequencyPage)
        pageDocumentMultiYearOptOut.getElementById("reporting-obligations-link").attr("href") shouldBe reportingFrequencyLink
      }
    }
  }

  "NextUpdatesOptOut view" when {

    "Reporting Frequency feature switch is turned OFF" should {

      "Mandated single year have the single year opt out warning link" in new Setup() {
        disable(ReportingFrequencyPage)
        pageDocumentWithWarning.getElementById("reporting-obligations-link").attr("href") shouldBe reportingFrequencyLink
      }

      "Multi year scenarios" should {

        "have the multi year opt out message" in new Setup() {
          disable(ReportingFrequencyPage)
          pageDocumentMultiYearOptOut.getElementById("what-the-user-can-do").text() shouldBe ObligationsMessages.reportingObligationsLink
        }

        "have the multi year opt out message link" in new Setup() {
          disable(ReportingFrequencyPage)
          pageDocumentMultiYearOptOut.getElementById("reporting-obligations-link").attr("href") shouldBe reportingFrequencyLink
        }
      }
    }
  }
}
