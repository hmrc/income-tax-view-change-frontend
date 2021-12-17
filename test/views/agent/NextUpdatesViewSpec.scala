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

package views.agent

import testConstants.BusinessDetailsTestConstants.{business1, testTradeName}
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import config.FrontendAppConfig
import models.nextUpdates.{NextUpdatesModel, ObligationsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.agent.NextUpdates

class NextUpdatesViewSpec extends TestSupport {

	lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
	val agentNextUpdates: NextUpdates = app.injector.instanceOf[NextUpdates]

	class Setup(currentObligations: ObligationsModel) {
		val pageDocument: Document = Jsoup.parse(contentAsString(agentNextUpdates(currentObligations, "testBackURL")))
	}

	object obligationsMessages {
		val heading: String = "Next updates"
		val title: String = s"$heading - Your client’s Income Tax details - GOV.UK"
		val summary: String = "What are the update types?"
		val summaryQuarterly: String = "Quarterly updates"
		val summaryAnnual: String = "Annual updates"
		val summaryDeclaration: String = "Final declaration"
	}

	lazy val obligationsModel = ObligationsModel(Seq(NextUpdatesModel(
		business1.incomeSourceId.get,
		twoObligationsSuccessModel.obligations
	)))

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

		"have a summary section for annual updates" in new Setup(obligationsModel) {
			pageDocument.select("details h2").get(1).text() shouldBe obligationsMessages.summaryAnnual
		}

		"have a summary section for final declarations" in new Setup(obligationsModel) {
			pageDocument.select("details h2").get(2).text() shouldBe obligationsMessages.summaryDeclaration
		}

		"have an updates accordion" in  new Setup(obligationsModel) {
			pageDocument.select("div .govuk-accordion").size() == 1
		}

		"has a link to view what you owe" in new Setup(obligationsModel) {
			pageDocument.getElementById("back").attr("href") shouldBe "testBackURL"
		}

		s"have the correct TradeName" in new Setup(obligationsModel) {
			val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")

			section.select("tbody tr").size() shouldBe 1
			section.select("tbody tr td:nth-of-type(1)").text() shouldBe "Annual Update"
			section.select("tbody tr td:nth-of-type(2)").text() shouldBe testTradeName
		}

	}
}
