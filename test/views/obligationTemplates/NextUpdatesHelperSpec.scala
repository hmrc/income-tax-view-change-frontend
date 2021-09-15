/*
 * Copyright 2021 HM Revenue & Customs
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

package views.obligationTemplates

import assets.BaseTestConstants.testMtdItUser
import assets.BusinessDetailsTestConstants.business1
import assets.NextUpdatesTestConstants.{crystallisedObligation, crystallisedObligationTwo, twoObligationsSuccessModel}
import implicits.ImplicitDateFormatterImpl
import models.nextUpdates.{ObligationsModel, NextUpdatesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.helpers.injected.obligations.NextUpdatesHelper

class NextUpdatesHelperSpec extends TestSupport {

	lazy val nextUpdatesHelper = app.injector.instanceOf[NextUpdatesHelper]

	class Setup(currentObligations: ObligationsModel) {
		val html: HtmlFormat.Appendable = nextUpdatesHelper(currentObligations)(implicitly, testMtdItUser)
		val pageDocument: Document = Jsoup.parse(contentAsString(html)(defaultTimeout))
	}

	lazy val obligationsModel = ObligationsModel(Seq(NextUpdatesModel(
		business1.incomeSourceId,
		twoObligationsSuccessModel.obligations
	)))

	lazy val crystallisedObligationsModel = ObligationsModel(Seq(NextUpdatesModel(
		business1.incomeSourceId,
		List(crystallisedObligation)
	)))

	object Messages {
		val quarterlyUpdateDue = "Update for: 1 July 2017 to 30 September 2017"
		val nonQuarterlyUpdateDue = "Tax year: 1 October 2017 to 30 October 2018"
	}

	"Next updates helper" should {

		"display the correct number of accordion sections" in new Setup(obligationsModel) {
			pageDocument.select(".govuk-accordion__section").size() shouldBe 2
		}

		"display the earliest due date first" in new Setup(obligationsModel) {
			pageDocument.select(".govuk-accordion__section:nth-of-type(1) h3").text() shouldBe "30 October 2017"
		}

		"display the updates under the first deadline" in new Setup(obligationsModel) {
			val section = pageDocument.select(".govuk-accordion__section:nth-of-type(1)")

			section.select("tbody tr").size() shouldBe 1
			section.select("tbody tr td:nth-of-type(1)").text() shouldBe "Quarterly"
			section.select("tbody tr td:nth-of-type(2)").text() shouldBe "business"
		}

		"display the later due date" in new Setup(obligationsModel) {
			pageDocument.select(".govuk-accordion__section:nth-of-type(2) h3").text() shouldBe "31 October 2017"
		}

		"display the updates under the second deadline" in new Setup(obligationsModel) {
			val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")

			section.select("tbody tr").size() shouldBe 1
			section.select("tbody tr td:nth-of-type(1)").text() shouldBe "EOPS"
			section.select("tbody tr td:nth-of-type(2)").text() shouldBe "business"
		}

		"display the correct due date text for a quarterly date" in new Setup(obligationsModel) {
			pageDocument.select(".govuk-accordion__section:nth-of-type(1) .govuk-accordion__section-summary").text() shouldBe Messages.quarterlyUpdateDue
		}

		"display the correct due date text for a non-quarterly date" in new Setup(crystallisedObligationsModel) {
			pageDocument.select(".govuk-accordion__section:nth-of-type(1) .govuk-accordion__section-summary").text() shouldBe Messages.nonQuarterlyUpdateDue
		}
	}
}
