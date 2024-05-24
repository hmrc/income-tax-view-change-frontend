/*
 * Copyright 2024 HM Revenue & Customs
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

package views.claimToAdjustPoa

import forms.adjustPoa.EnterPoaAmountForm
import models.claimToAdjustPoa.PoAAmountViewModel
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.Form
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.claimToAdjustPoa.EnterPoAAmountView

class EnterPoAAmountViewSpec extends TestSupport{

  val enterAmountView: EnterPoAAmountView = app.injector.instanceOf[EnterPoAAmountView]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  def msg(key: String) = msgs(s"claimToAdjustPoa.enterPoaAmount.$key")
  implicit val lang: Lang = Lang("GB")

  val cancelUrl = controllers.routes.HomeController.show().url

  val poaViewModelFirstJourney = PoAAmountViewModel(
    poaPreviouslyAdjusted = false,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 5000,
    totalAmountTwo = 5000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000
  )

  val poaViewModelSecondJourney = PoAAmountViewModel(
    poaPreviouslyAdjusted = true,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 4000,
    totalAmountTwo = 4000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000
  )

  class Setup(isAgent: Boolean = false, form: Form[EnterPoaAmountForm] = EnterPoaAmountForm.form, viewModel: PoAAmountViewModel = poaViewModelFirstJourney) {
    val view: Html = enterAmountView(form, viewModel, isAgent, controllers.claimToAdjustPoa.routes.EnterPoAAmountController.submit(isAgent))
    val document: Document = Jsoup.parse(view.toString())
    val groupButton: Elements = document.select("div.govuk-button-group")
    val buttons = groupButton.first().children()
  }

  "The Enter PoA Amount page" should {
    "render the correct constant page content" in new Setup {
      document.title shouldBe msgs("htmlTitle", msg("heading"))

      document.getElementById("caption").text shouldBe
        msgs("claimToAdjustPoa.enterPoaAmount.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)

      document.getElementById("h1").text shouldBe msg("heading")

      document.getElementById("bulletPoints").text() shouldBe {msg("p1") + " " + msg("bullet1") + " " + msg("bullet2")}

      document.getElementById("warning").text shouldBe {"! Warning " + msg("warning")}

      document.getElementById("poa-amount").text shouldBe {msg("howMuch") + " " + msg("howMuchHint") + " £ " +
        msgs("base.continue") + " " + msg("cancel")}

      document.getElementsByClass("govuk-input").size() shouldBe 1

      buttons.get(0).text shouldBe msgs("base.continue")

      buttons.get(1).text shouldBe msg("cancel")
      document.getElementById("cancel").attr("href") shouldBe cancelUrl
    }
    "render the table with only Initial Amount for user on first visit" in new Setup(viewModel = poaViewModelFirstJourney) {
      document.getElementsByClass("govuk-table__head").text() shouldBe msg("initialAmount")
      val tableBody = document.getElementsByClass("govuk-table__body")
      tableBody.select(".govuk-table__header:nth-of-type(1)").text shouldBe msg("firstPayment")
      tableBody.select(".govuk-table__cell:nth-of-type(3)").text shouldBe {"£"+poaViewModelFirstJourney.relevantAmountOne}
       // table.select(".govuk-table__cell:nth-of-type(1)").text() shouldBe messages("nextUpdates.quarterly")

    }
  }

}
