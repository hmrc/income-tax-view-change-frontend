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

package views.optIn

import config.FrontendAppConfig
import forms.optIn.ChooseTaxYearForm
import models.incomeSourceDetails.TaxYear
import models.optin.ChooseTaxYearViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.ChooseTaxYearView

class ChooseTaxYearViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val optInChooseTaxYearView: ChooseTaxYearView = app.injector.instanceOf[ChooseTaxYearView]

  val forYearEnd = 2023
  val taxYear: TaxYear = TaxYear.forYearEnd(forYearEnd)
  val availableOptOutTaxYear: List[TaxYear] = List(taxYear, taxYear.nextYear)
  val availableOptOutTaxYearsList: List[String] = availableOptOutTaxYear.map(_.toString)

  class Setup(isAgent: Boolean = true) {
    val cancelURL = if (isAgent) controllers.routes.NextUpdatesController.showAgent().url else controllers.routes.NextUpdatesController.show().url
    val model = ChooseTaxYearViewModel(availableOptOutTaxYear, cancelURL = cancelURL, isAgent = isAgent)
    val pageDocument: Document = Jsoup.parse(contentAsString(optInChooseTaxYearView(ChooseTaxYearForm(availableOptOutTaxYearsList), model)))
  }

  object optInChooseTaxYear {
    val heading: String = messages("optin.chooseOptInTaxYear.heading")
    val title: String = messages("htmlTitle", heading)
    val desc1: String = messages("optin.chooseOptInTaxYear.desc1")
    val whichTaxYear: String = messages("optin.chooseOptInTaxYear.whichTaxYear")
    val continueButton: String = messages("optout.chooseOptOutTaxYear.continue")
    val cancelButton: String = messages("optin.confirmOptIn.cancel")
    val cancelButtonHref: String = controllers.routes.NextUpdatesController.show().url
    val agentCancelButtonHref: String = controllers.routes.NextUpdatesController.showAgent().url
  }

  "Opt-out confirm page" should {

    "have the correct title" in new Setup(false) {
      pageDocument.title() shouldBe optInChooseTaxYear.title
    }

    "have the correct heading" in new Setup(false) {
      pageDocument.select("h1").text() shouldBe optInChooseTaxYear.heading
    }

    "have the correct summary heading and page contents" in new Setup(false) {
      pageDocument.getElementById("description1").text() shouldBe optInChooseTaxYear.desc1
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe optInChooseTaxYear.whichTaxYear
      pageDocument.getElementById("continue-button").text() shouldBe optInChooseTaxYear.continueButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe optInChooseTaxYear.cancelButtonHref
    }

    "have the correct summary heading and page contents for Agents" in new Setup(true) {
      pageDocument.getElementById("description1").text() shouldBe optInChooseTaxYear.desc1
      pageDocument.getElementById("whichTaxYear").child(0).text() shouldBe optInChooseTaxYear.whichTaxYear
      pageDocument.getElementById("continue-button").text() shouldBe optInChooseTaxYear.continueButton
      pageDocument.getElementById("cancel-button").text() shouldBe optInChooseTaxYear.cancelButton
      pageDocument.getElementById("cancel-button").attr("href") shouldBe optInChooseTaxYear.agentCancelButtonHref
    }
  }
}
