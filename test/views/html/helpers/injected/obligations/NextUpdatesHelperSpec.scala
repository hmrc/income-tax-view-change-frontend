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

package views.html.helpers.injected.obligations

import models.itsaStatus.ITSAStatus
import models.obligations._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testMtdItUser
import testConstants.BusinessDetailsTestConstants.{business1, testTradeName}
import testConstants.NextUpdatesTestConstants.{crystallisedObligation, twoObligationsSuccessModel}
import testUtils.TestSupport

import java.time.LocalDate

class  NextUpdatesHelperSpec extends TestSupport {

  class Setup(currentObligations: NextUpdatesViewModel) {
    val nextUpdatesHelper = app.injector.instanceOf[NextUpdatesHelper]

    val html: HtmlFormat.Appendable = nextUpdatesHelper(currentObligations)(implicitly, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  lazy val obligationsModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
    business1.incomeSourceId,
    twoObligationsSuccessModel.obligations
  ))).obligationsByDate(isR17ContentEnabled = true, Some(ITSAStatus.Voluntary)).map{case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)})

  lazy val crystallisedObligationsModel: NextUpdatesViewModel = NextUpdatesViewModel(ObligationsModel(Seq(GroupedObligationsModel(
    business1.incomeSourceId,
    List(crystallisedObligation)
  ))).obligationsByDate(isR17ContentEnabled = true, Some(ITSAStatus.Voluntary)).map{case (date: LocalDate, obligations: Seq[ObligationWithIncomeType]) =>
    DeadlineViewModel(QuarterlyObligation, standardAndCalendar = false, date, obligations, Seq.empty)})

  "Next updates helper" should {

    "display the correct number of accordion sections" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section").size() shouldBe 2
    }

    "display the earliest due date first" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section:nth-of-type(1) h2").text() shouldBe "30 October 2017"
    }

    "display the updates under the first deadline" in new Setup(obligationsModel) {
      val section = pageDocument.select(".govuk-accordion__section:nth-of-type(1)")
      val heading = section.select(".govuk-accordion__section-summary")

      heading.text() shouldBe "Quarterly update"

      val table = section.select(".govuk-table")
      table.select(".govuk-table__caption").text() should  fullyMatch regex """Quarterly\speriod\sfrom\s1\sJul\s2017\sto\s30\s([Sept|Sep]+)+\s2017"""

      table.select(".govuk-table__head").text() shouldBe "Update type Income source"

      table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe "Quarterly update"
      table.select(".govuk-table__cell:nth-of-type(2)").text() shouldBe messages(testTradeName)
    }

    "display the later due date" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section:nth-of-type(2) h2").text() shouldBe "31 October 2017"
    }

    "display the updates under the second deadline" in new Setup(obligationsModel) {
      val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")
      val heading = section.select(".govuk-accordion__section-summary")

      heading.text() shouldBe messages("nextUpdates.quarterly")

      val table = section.select(".govuk-table")

      table.select(".govuk-table__caption").text() shouldBe "Quarterly period from 6 Apr 2017 to 5 Apr 2018"

      table.select(".govuk-table__head").text() shouldBe "Update type Income source"

      table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe messages("nextUpdates.quarterly")
      table.select(".govuk-table__cell:nth-of-type(2)").text() shouldBe messages(testTradeName)
    }

    "display the correct due date text for a quarterly date" in new Setup(obligationsModel) {
      pageDocument.getElementById("accordion-with-summary-sections-heading-1").text() shouldBe "30 October 2017"
    }

    "display the correct due date text for a non-quarterly date" in new Setup(crystallisedObligationsModel) {
      pageDocument.getElementById("accordion-with-summary-sections-heading-1").text() shouldBe "31 October 2017"
    }
  }
}

