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

import testConstants.BaseTestConstants.testMtdItUser
import testConstants.BusinessDetailsTestConstants.{business1, testTradeName}
import testConstants.NextUpdatesTestConstants.{crystallisedObligation, twoObligationsSuccessModel}
import models.nextUpdates.{NextUpdatesModel, ObligationsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class  NextUpdatesHelperSpec extends TestSupport {

  class Setup(currentObligations: ObligationsModel) {
    val nextUpdatesHelper = app.injector.instanceOf[NextUpdatesHelper]

    val html: HtmlFormat.Appendable = nextUpdatesHelper(currentObligations)(implicitly, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  lazy val obligationsModel = ObligationsModel(Seq(NextUpdatesModel(
    business1.incomeSourceId.get,
    twoObligationsSuccessModel.obligations
  )))

  lazy val crystallisedObligationsModel = ObligationsModel(Seq(NextUpdatesModel(
    business1.incomeSourceId.get,
    List(crystallisedObligation)
  )))

  object Messages {
    val quarterlyUpdateDue: String = messages("nextUpdates.section.heading.updates", "1 July 2017", "30 September 2017")
    val nonQuarterlyUpdateDue: String = messages("nextUpdates.section.heading.taxYear", "1 October 2017", "30 October 2018")
  }

  "Next updates helper" should {

    "display the correct number of accordion sections" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section").size() shouldBe 2
    }

    "display the earliest due date first" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section:nth-of-type(1) h2").text() shouldBe "30 October 2017"
    }

    "display the updates under the first deadline" in new Setup(obligationsModel) {
      val section = pageDocument.select(".govuk-accordion__section:nth-of-type(1)")

      section.select("dl").size() shouldBe 1
      section.select("dl dt").text() shouldBe "Quarterly update"
      section.select("dl dd").text() shouldBe messages(testTradeName)
    }

    "display the later due date" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section:nth-of-type(2) h2").text() shouldBe "31 October 2017"
    }

    "display the updates under the second deadline" in new Setup(obligationsModel) {
      val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")

      section.select("dl").size() shouldBe 1
      section.select("dl dt").text() shouldBe messages("nextUpdates.eops")
      section.select("dl dd").text() shouldBe messages(testTradeName)
    }

    "display the correct due date text for a quarterly date" in new Setup(obligationsModel) {
      pageDocument.select(".govuk-accordion__section:nth-of-type(1) .govuk-accordion__section-summary").text() shouldBe Messages.quarterlyUpdateDue
    }

    "display the correct due date text for a non-quarterly date" in new Setup(crystallisedObligationsModel) {
      pageDocument.select(".govuk-accordion__section:nth-of-type(1) .govuk-accordion__section-summary").text() shouldBe Messages.nonQuarterlyUpdateDue
    }
  }
}
