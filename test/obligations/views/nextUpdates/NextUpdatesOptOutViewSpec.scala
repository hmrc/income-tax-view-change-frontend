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

package obligations.views.nextUpdates

import common.auth.MtdItUser
import common.config.FrontendAppConfig
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus.Annual
import common.models.obligations.{GroupedObligationsModel, ObligationWithIncomeType, ObligationsModel, SingleObligationModel, StatusFulfilled}
import common.testUtils.TestSupport
import common.models.admin.ReturnsFrontend
import obligations.models.reportingObligations.optOut.NextUpdatesQuarterlyReportingContentChecks
import obligations.models.*
import obligations.services.reportingObligations.optOut.OptOutProposition
import shared.testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import obligations.views.html.nextUpdates.NextUpdatesOptOutView
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import obligations.testConstants.BusinessDetailsTestConstants.business1
import common.views.html.components.link
import shared.testConstants.NextUpdatesTestConstants
import obligations.controllers.reportingObligations.routes as reportingObligationsRoutes

import java.time.LocalDate

class NextUpdatesOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def nextUpdatesView: NextUpdatesOptOutView = app.injector.instanceOf[NextUpdatesOptOutView]

  val linkComponent: link = app.injector.instanceOf[link]

  class Setup(quarterlyUpdateContentShow: Boolean = true,
              isSupportingAgent: Boolean = false, includeMissedDeadlines: Boolean = false) {

    val currentYear: TaxYear = TaxYear(2025, 2026)

    val user: MtdItUser[_] =
      getIndividualUser(FakeRequest())

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


    val optOutProposition: OptOutProposition = OptOutProposition.createOptOutProposition(
      currentYear = currentYear,
      previousYearCrystallised = false,
      previousYearItsaStatus = Annual,
      currentYearItsaStatus = Annual,
      nextYearItsaStatus = Annual
    )

    val missedDeadlinesTestYear: LocalDate = LocalDate.of(2025, 10, 23)
    lazy val obligationsModelMissedDeadlines: NextUpdatesViewModel =
      NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
        business1.incomeSourceId,
        twoObligationsSuccessModel.obligations
      ))).obligationsByDate(false)(user).map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
        DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
      },missedDeadlines = Seq(DeadlineViewModel(QuarterlyObligation,
        standardAndCalendar = false,
        missedDeadlinesTestYear,
        Seq(ObligationWithIncomeType("uk-property", SingleObligationModel(start = missedDeadlinesTestYear, end = missedDeadlinesTestYear, due = missedDeadlinesTestYear, obligationType = "Quarterly", dateReceived = None, periodKey = StatusFulfilled.toString, status = StatusFulfilled))),
        Seq.empty)), isFinancialsEnabled = true)

    lazy val obligationsModel: NextUpdatesViewModel =
      NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
        business1.incomeSourceId,
        twoObligationsSuccessModel.obligations
      ))).obligationsByDate(false)(user).map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
        DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
      }, isFinancialsEnabled = true)

    def nextUpdatesDocument: Document =
      Jsoup.parse(contentAsString(
        nextUpdatesView(
          viewModel = if(includeMissedDeadlines) obligationsModelMissedDeadlines else obligationsModel,
          checks,
          optOutProposition = optOutProposition,
          "testBackURL",
          isSupportingAgent = isSupportingAgent,
          taxYearStatusesCyNy = (optOutProposition.currentTaxYear.status, optOutProposition.nextTaxYear.status),
          isReturnsEnabled = isEnabled(ReturnsFrontend),
          penaltyAndAppealEnabled = true
        )(implicitly, user)
      ))
  }

    "NextUpdatesOptOut view" when {

      "The user has missed deadlines" should {
        "have full 'whatTheUserCanDo' section" in new Setup(includeMissedDeadlines = true) {
          nextUpdatesDocument.getElementById("what-the-user-can-do-1").text() shouldBe "You must complete your outstanding quarterly updates for the 2025 to 2026 tax year."
          nextUpdatesDocument.getElementById("what-the-user-can-do-2").text() shouldBe "Quarterly updates are cumulative. This means your latest outstanding update may include information from earlier missed periods."
          nextUpdatesDocument.getElementById("what-the-user-can-do-3").text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations."
          nextUpdatesDocument.getElementById("reporting-frequency-link").attr("href") shouldBe reportingObligationsRoutes.ReportingFrequencyPageController.show(false).url
        }
      }
      "The user does NOT have missed deadlines" should {
        "have full 'whatTheUserCanDo' section" in new Setup() {
          nextUpdatesDocument.select("what-the-user-can-do-2").size() shouldBe 0
          nextUpdatesDocument.getElementById("what-the-user-can-do-2").text() shouldBe "Quarterly updates are cumulative. This means your latest outstanding update may include information from earlier missed periods."
          nextUpdatesDocument.getElementById("what-the-user-can-do-3").text() shouldBe "Depending on your circumstances, you may be able to view and change your reporting obligations."
          nextUpdatesDocument.getElementById("reporting-frequency-link").attr("href") shouldBe reportingObligationsRoutes.ReportingFrequencyPageController.show(false).url
        }
      }

      "The reporting frequency FS is turned ON" should {
        "have the correct title" in new Setup() {
          nextUpdatesDocument.title() shouldBe NextUpdatesTestConstants.title
        }

        "have the correct heading" in new Setup() {
          nextUpdatesDocument.select("h1").text() shouldBe NextUpdatesTestConstants.heading
        }

        "not have the summary heading" in new Setup() {
          nextUpdatesDocument.select("summary").isEmpty shouldBe true
        }

        "not have a summary section for quarterly updates" in new Setup() {
          nextUpdatesDocument.select("details h2").isEmpty shouldBe true
        }

        "not have the details for quarterly updates" in new Setup() {
          Option(nextUpdatesDocument.getElementById("quarterly-dropdown-line1")) shouldBe None
          Option(nextUpdatesDocument.getElementById("quarterly-dropdown-line2")) shouldBe None
        }

        s"not have the Submitting updates in software" in new Setup() {
          Option(nextUpdatesDocument.getElementById("updates-software-heading")) shouldBe None
          Option(nextUpdatesDocument.getElementById("updates-software-link")) shouldBe None
        }
      }
    }

}
