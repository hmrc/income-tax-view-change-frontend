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
import models.obligations._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.NextUpdatesTestConstants
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import testUtils.TestSupport
import views.html.nextUpdates.NextUpdates

import java.time.LocalDate

class NextUpdatesViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val nextUpdatesView: NextUpdates = app.injector.instanceOf[NextUpdates]

  class Setup(currentObligations: NextUpdatesViewModel, isSupportingAgent: Boolean = false) {
    val pageDocument: Document = Jsoup.parse(contentAsString(nextUpdatesView(currentObligations, "testBackURL", isSupportingAgent = isSupportingAgent)))
  }

  lazy val obligationsModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
    business1.incomeSourceId,
    twoObligationsSuccessModel.obligations
  ))).obligationsByDate(isR17ContentEnabled = false).map { case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)
  })

  "Next Updates page" should {

    "have the correct title" in new Setup(obligationsModel) {
      pageDocument.title() shouldBe NextUpdatesTestConstants.title
    }

    "have the correct heading" in new Setup(obligationsModel) {
      pageDocument.select("h1").text() shouldBe NextUpdatesTestConstants.heading
    }

    "have the correct summary heading" in new Setup(obligationsModel) {
      pageDocument.select("summary").text() shouldBe NextUpdatesTestConstants.summary
    }

    "have a summary section for quarterly updates" in new Setup(obligationsModel) {
      pageDocument.select("details h2").get(0).text() shouldBe NextUpdatesTestConstants.summaryQuarterly
    }

    "have the correct line 1 for quarterly updates section" in new Setup(obligationsModel) {
      pageDocument.getElementById("quarterly-dropdown-line1").text() shouldBe NextUpdatesTestConstants.quarterlyLine1
    }

    "have the correct line 2 for quarterly updates section" in new Setup(obligationsModel) {
      pageDocument.getElementById("quarterly-dropdown-line2").text() shouldBe NextUpdatesTestConstants.quarterlyLine2
    }

    "have a summary section for final declarations" in new Setup(obligationsModel) {
      pageDocument.select("details h2").get(1).text() shouldBe NextUpdatesTestConstants.summaryDeclaration
    }

    "have the correct line 1 for final declaration section" in new Setup(obligationsModel) {
      pageDocument.getElementById("final-declaration-line1").text() shouldBe NextUpdatesTestConstants.declarationLine1
    }

    "have an updates accordion" in new Setup(obligationsModel) {
      pageDocument.select("div .govuk-accordion").size() == 1
    }

    s"have the information ${NextUpdatesTestConstants.info}" when {
      "a primary agent or individual" in new Setup(obligationsModel) {
        pageDocument.select("p:nth-child(6)").text shouldBe NextUpdatesTestConstants.info
        pageDocument.select("p:nth-child(6) a").attr("href") shouldBe controllers.routes.TaxYearsController.showTaxYears().url
      }
    }

    s"not have the information ${NextUpdatesTestConstants.info}" when {
      "a supporting agent" in new Setup(obligationsModel, true) {
        pageDocument.body.text() shouldNot include(NextUpdatesTestConstants.info)
      }
    }

    s"have the correct TradeName" in new Setup(obligationsModel) {
      val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")

      val table = section.select(".govuk-table")

      table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe NextUpdatesTestConstants.quarterly
      table.select(".govuk-table__cell:nth-of-type(2)").text() shouldBe NextUpdatesTestConstants.businessIncome
    }
  }
}
