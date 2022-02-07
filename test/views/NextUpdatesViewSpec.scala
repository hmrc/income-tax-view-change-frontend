/*
 * Copyright 2022 HM Revenue & Customs
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

package views

import testConstants.BusinessDetailsTestConstants.{business1, testTradeName}
import testConstants.NextUpdatesTestConstants.twoObligationsSuccessModel
import config.FrontendAppConfig
import models.nextUpdates.{NextUpdatesModel, ObligationsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.NextUpdates

class NextUpdatesViewSpec extends TestSupport {

	lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
	val nextUpdatesView: NextUpdates = app.injector.instanceOf[NextUpdates]

	class Setup(currentObligations: ObligationsModel) {
		val pageDocument: Document = Jsoup.parse(contentAsString(nextUpdatesView(currentObligations, "testBackURL")))
	}

	object obligationsMessages {
		val heading: String = "Next updates"
		val title: String = s"$heading - Business Tax account - GOV.UK"
		val summary: String = "What are the update types?"
		val summaryQuarterly: String = "Quarterly updates"
		val quarterlyLine1: String = "A quarterly update is a record of all your business income in a 3 month period."
		val quarterlyLine2: String = "Using your record-keeping software, you must send 4 quarterly updates in a year for each source of income."
		val annualLine1: String = "In an annual update you need to declare that the 4 quarterly updates you have sent are correct. You can also change any previous errors."
		val annualLine2: String = "Using your record-keeping software, you need to send one annual update for each source of income at the end of each accounting period."
		val declarationLine1: String = "Your final declaration confirms that the annual updates you have sent are correct and that you have submitted every source of income and expenses true to your knowledge. This is done using your record-keeping software."
		val summaryAnnual: String = "Annual updates"
		val summaryDeclaration: String = "Final declaration"
		val info: String = "To view previously submitted updates visit the tax years page."
	}

	lazy val obligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel(
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

		"have the correct line 1 for quarterly updates section" in new Setup(obligationsModel) {
			pageDocument.getElementById("quarterly-dropdown-line1").text() shouldBe obligationsMessages.quarterlyLine1
		}

		"have the correct line 2 for quarterly updates section" in new Setup(obligationsModel) {
			pageDocument.getElementById("quarterly-dropdown-line2").text() shouldBe obligationsMessages.quarterlyLine2
		}

		"have a summary section for annual updates" in new Setup(obligationsModel) {
			pageDocument.select("details h2").get(1).text() shouldBe obligationsMessages.summaryAnnual
		}

		"have the correct line 1 for annual updates section" in new Setup(obligationsModel) {
			pageDocument.getElementById("annual-dropdown-line1").text() shouldBe obligationsMessages.annualLine1
		}

		"have the correct line 2 for annual updates section" in new Setup(obligationsModel) {
			pageDocument.getElementById("annual-dropdown-line2").text() shouldBe obligationsMessages.annualLine2
		}

		"have a summary section for final declarations" in new Setup(obligationsModel) {
			pageDocument.select("details h2").get(2).text() shouldBe obligationsMessages.summaryDeclaration
		}

		"have the correct line 1 for final declaration section" in new Setup(obligationsModel) {
			pageDocument.getElementById("final-declaration-line1").text() shouldBe obligationsMessages.declarationLine1
		}

		"have an updates accordion" in  new Setup(obligationsModel) {
			pageDocument.select("div .govuk-accordion").size() == 1
		}

		s"have the information ${obligationsMessages.info}" in new Setup(obligationsModel) {
			pageDocument.select("p:nth-child(6)").text shouldBe obligationsMessages.info
			pageDocument.select("p:nth-child(6) a").attr("href") shouldBe controllers.routes.TaxYearsController.viewTaxYears().url
		}

		s"have the correct TradeName" in new Setup(obligationsModel) {
			val section = pageDocument.select(".govuk-accordion__section:nth-of-type(2)")
			println(Console.BLUE + section)

			section.select("dl").size() shouldBe 1
			section.select("dl dt").text() shouldBe "Annual Update"
			section.select("dl dd").text() shouldBe testTradeName
		}
	}
}
