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
import models.obligations.*
import models.reportingObligations.optOut.NextUpdatesQuarterlyReportingContentChecks
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.twirl.api.Html
import services.reportingObligations.optOut.OptOutProposition
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.NextUpdatesTestConstants
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import testUtils.TestSupport
import viewUtils.NextUpdatesViewUtils
import views.html.components.link
import views.html.nextUpdates.NextUpdatesOptOutView

import java.time.LocalDate

class NextUpdatesOptOutViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  def nextUpdatesView: NextUpdatesOptOutView = app.injector.instanceOf[NextUpdatesOptOutView]

  val linkComponent: link = app.injector.instanceOf[link]

  class Setup(quarterlyUpdateContentShow: Boolean = true,
              isSupportingAgent: Boolean = false,
              reportingFrequencyPageFsEnabled: Boolean = true,
              optInOptOutContentR17Enabled: Boolean = false) {

    val currentYear: TaxYear = TaxYear(2025, 2026)

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


    val optOutProposition: OptOutProposition = OptOutProposition.createOptOutProposition(
      currentYear = currentYear,
      previousYearCrystallised = false,
      previousYearItsaStatus = Annual,
      currentYearItsaStatus = Annual,
      nextYearItsaStatus = Annual
    )

    def nextUpdatesViewUtils: NextUpdatesViewUtils = new NextUpdatesViewUtils(linkComponent)

    def whatTheUserCanDoContentMulti: Option[Html] =
      nextUpdatesViewUtils.whatTheUserCanDo(isSupportingAgent)(user, implicitly)

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
          whatTheUserCanDo = whatTheUserCanDoContentMulti,
          optInOptOutContentR17Enabled = optInOptOutContentR17Enabled,
          taxYearStatusesCyNy = (optOutProposition.currentTaxYear.status, optOutProposition.nextTaxYear.status)
        )(implicitly, user)
      ))
  }

    "NextUpdatesOptOut view" when {
      "The reporting frequency FS is turned ON" should {
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
