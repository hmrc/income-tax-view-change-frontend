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
import models.itsaStatus.ITSAStatus.Annual
import models.obligations._
import models.optout.NextUpdatesQuarterlyReportingContentChecks
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.optout.OptOutProposition
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.NextUpdatesTestConstants
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import testUtils.TestSupport
import views.html.components.link
import views.html.nextUpdates.NextUpdatesOptOut

import java.time.LocalDate

class NextUpdatesOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def nextUpdatesView: NextUpdatesOptOut = app.injector.instanceOf[NextUpdatesOptOut]

  val linkComponent: link = app.injector.instanceOf[link]

  class Setup(quarterlyUpdateContentShow: Boolean = true,
              isSupportingAgent: Boolean = false,
              reportingFrequencyPageFsEnabled: Boolean = true,
              optInOptOutContentR17Enabled: Boolean = false) {

    val currentYear = TaxYear(2025, 2026)

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


    val optOutProposition = OptOutProposition.createOptOutProposition(
      currentYear = currentYear,
      previousYearCrystallised = false,
      previousYearItsaStatus = Annual,
      currentYearItsaStatus = Annual,
      nextYearItsaStatus = Annual
    )

    lazy val obligationsModel: NextUpdatesViewModel =
      NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
        business1.incomeSourceId,
        twoObligationsSuccessModel.obligations
      ))).obligationsByDate(isR17ContentEnabled = true)(user).map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
        DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
      })

    def nextUpdatesDocument: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          obligationsModel,
          checks,
          optOutProposition = optOutProposition,
          "testBackURL",
          isSupportingAgent = isSupportingAgent,
          reportingFrequencyLink = controllers.routes.ReportingFrequencyPageController.show(false).url,
          reportingFrequencyEnabled = reportingFrequencyPageFsEnabled,
          optInOptOutContentR17Enabled = optInOptOutContentR17Enabled
        )(implicitly, user)
      ))

    val confirmOptOutLink = "/report-quarterly/income-and-expenses/view/optout/review-confirm-taxyear"
    val reportingFrequencyLink = "/report-quarterly/income-and-expenses/view/reporting-frequency"
  }

    "NextUpdatesOptOut view" when {

      "The reporting frequency FS is turned ON & OptInOptOutContentR17 turned is turned OFF" should {

        "have the correct title" in new Setup() {
          nextUpdatesDocument.title() shouldBe NextUpdatesTestConstants.title
        }

        "have the correct heading" in new Setup() {
          nextUpdatesDocument.select("h1").text() shouldBe NextUpdatesTestConstants.heading
        }

        "have the correct summary heading" in new Setup() {
          nextUpdatesDocument.select("summary").text() shouldBe NextUpdatesTestConstants.summary
        }

        "have a summary section for quarterly updates" in new Setup() {
          nextUpdatesDocument.select("details h2").get(0).text() shouldBe NextUpdatesTestConstants.summaryQuarterly
        }

        "have the correct details for quarterly updates section" in new Setup() {
          nextUpdatesDocument.getElementById("quarterly-dropdown-line1").text() shouldBe NextUpdatesTestConstants.quarterlyLine1
          nextUpdatesDocument.getElementById("quarterly-dropdown-line2").text() shouldBe NextUpdatesTestConstants.quarterlyLine2
        }

        "don't show quarterly updates section" in new Setup(quarterlyUpdateContentShow = false) {
          nextUpdatesDocument.select("#quarterly-dropdown-line1").isEmpty shouldBe true
          nextUpdatesDocument.select("#quarterly-dropdown-line2").isEmpty shouldBe true
        }

        "have a summary section for final declarations" in new Setup() {
          nextUpdatesDocument.select("details h2").get(1).text() shouldBe NextUpdatesTestConstants.summaryDeclaration
        }

        "have the correct line 1 for final declaration section" in new Setup() {
          nextUpdatesDocument.getElementById("final-declaration-line1").text() shouldBe NextUpdatesTestConstants.declarationLine1
        }

        "have an updates accordion" in new Setup() {
          nextUpdatesDocument.select("div .govuk-accordion").size() == 1
        }

        s"have the information ${NextUpdatesTestConstants.info}" when {
          "a primary agent or individual" in new Setup() {
            nextUpdatesDocument.select("p:nth-child(6)").text shouldBe NextUpdatesTestConstants.info
            nextUpdatesDocument.select("p:nth-child(6) a").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
          }
        }

        s"not have the information ${NextUpdatesTestConstants.info}" when {
          "a supporting agent" in new Setup(isSupportingAgent = true) {
            nextUpdatesDocument.body.text() shouldNot include(NextUpdatesTestConstants.info)
          }
        }

        s"have the correct TradeName" in new Setup() {
          val section = nextUpdatesDocument.select(".govuk-accordion__section:nth-of-type(2)")
          val table = section.select(".govuk-table")

          table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe NextUpdatesTestConstants.quarterly
          table.select(".govuk-table__cell:nth-of-type(2)").text() shouldBe NextUpdatesTestConstants.businessIncome
        }

        s"have the Submitting updates in software" in new Setup() {
          nextUpdatesDocument.getElementById("updates-software-heading").text() shouldBe NextUpdatesTestConstants.updatesInSoftware
          nextUpdatesDocument.getElementById("updates-software-link").text() shouldBe NextUpdatesTestConstants.updatesInSoftwareDesc
        }

        s"don't show the Submitting updates in software section" in new Setup(quarterlyUpdateContentShow = false) {
          nextUpdatesDocument.select("#updates-software-heading").isEmpty shouldBe true
          nextUpdatesDocument.select("#updates-software-link").isEmpty shouldBe true
        }

        "have the reporting obligations message" in new Setup() {
          nextUpdatesDocument.getElementById("what-the-user-can-do").text() shouldBe NextUpdatesTestConstants.reportingObligationsLink
        }

        "have the reporting obligations link to the correct page" in new Setup(reportingFrequencyPageFsEnabled = true) {
          nextUpdatesDocument.getElementById("reporting-obligations-link").attr("href") shouldBe reportingFrequencyLink
        }
      }

      "The reporting frequency FS is turned OFF" should {

        "not have the reporting obligations message" in new Setup(reportingFrequencyPageFsEnabled = false) {
          Option(nextUpdatesDocument.getElementById("what-the-user-can-do")) shouldBe None
        }

        "not have the reporting obligations link" in new Setup(reportingFrequencyPageFsEnabled = false) {
          Option(nextUpdatesDocument.getElementById("reporting-obligations-link")) shouldBe None
        }
      }

      "The reporting frequency FS is turned ON & OptInOptOutContentR17 turned is turned ON" should {
        "have the correct title" in new Setup(optInOptOutContentR17Enabled = true) {
          nextUpdatesDocument.title() shouldBe NextUpdatesTestConstants.title
        }

        "have the correct heading" in new Setup(optInOptOutContentR17Enabled = true) {
          nextUpdatesDocument.select("h1").text() shouldBe NextUpdatesTestConstants.heading
        }

        "not have the summary heading" in new Setup(optInOptOutContentR17Enabled = true) {
          nextUpdatesDocument.select("summary").isEmpty shouldBe true
        }

        "not have a summary section for quarterly updates" in new Setup(optInOptOutContentR17Enabled = true) {
          nextUpdatesDocument.select("details h2").isEmpty shouldBe true
        }

        "not have the details for quarterly updates" in new Setup(optInOptOutContentR17Enabled = true) {
          Option(nextUpdatesDocument.getElementById("quarterly-dropdown-line1")) shouldBe None
          Option(nextUpdatesDocument.getElementById("quarterly-dropdown-line2")) shouldBe None
        }

        s"not have the Submitting updates in software" in new Setup(optInOptOutContentR17Enabled = true) {
          Option(nextUpdatesDocument.getElementById("updates-software-heading")) shouldBe None
          Option(nextUpdatesDocument.getElementById("updates-software-link")) shouldBe None
        }
      }
    }

}
