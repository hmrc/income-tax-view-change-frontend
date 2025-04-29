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

import _root_.implicits.ImplicitCurrencyFormatter._
import forms.adjustPoa.EnterPoaAmountForm
import models.claimToAdjustPoa.PaymentOnAccountViewModel
import models.core.NormalMode
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.data.Form
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.claimToAdjustPoa.EnterPoaAmountView

class EnterPoaAmountViewSpec extends TestSupport{

  val enterAmountView: EnterPoaAmountView = app.injector.instanceOf[EnterPoaAmountView]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  def msg(key: String) = msgs(s"claimToAdjustPoa.enterPoaAmount.$key")
  implicit val lang: Lang = Lang("GB")

  val cancelUrl: String = controllers.routes.HomeController.show().url

  def poAAmountViewModel(poaPreviouslyAdjusted: Option[Boolean] = Some(false), poaPartiallyPaid: Boolean = false) = PaymentOnAccountViewModel(
    poaOneTransactionId = "poaOne-Id",
    poaTwoTransactionId = "poaTwo-Id",
    previouslyAdjusted = poaPreviouslyAdjusted,
    taxYear = TaxYear.makeTaxYearWithEndYear(2024),
    totalAmountOne = 5000,
    totalAmountTwo = 5000,
    relevantAmountOne = 5000,
    relevantAmountTwo = 5000,
    partiallyPaid = poaPartiallyPaid,
    fullyPaid = false
  )

  val prePopulatedForm = Some(1200).fold(EnterPoaAmountForm.form)(value =>
    EnterPoaAmountForm.form.fill(EnterPoaAmountForm(value)))

  val noInputErrorForm = EnterPoaAmountForm.form.withError(EnterPoaAmountForm.amount, "claimToAdjustPoa.enterPoaAmount.emptyError")
    .fill(EnterPoaAmountForm(0))
  val invalidErrorForm = EnterPoaAmountForm.form.withError(EnterPoaAmountForm.amount, "claimToAdjustPoa.enterPoaAmount.invalidError")
    .fill(EnterPoaAmountForm(-1))
  val sameErrorForm = EnterPoaAmountForm.form.withError(EnterPoaAmountForm.amount, "claimToAdjustPoa.enterPoaAmount.sameError")
    .fill(EnterPoaAmountForm(5000))
  val higherErrorForm = EnterPoaAmountForm.form.withError(EnterPoaAmountForm.amount, "claimToAdjustPoa.enterPoaAmount.higherError")
    .fill(EnterPoaAmountForm(7000))

  class Setup(isAgent: Boolean = false, form: Form[EnterPoaAmountForm] = EnterPoaAmountForm.form, viewModel: PaymentOnAccountViewModel = poAAmountViewModel()) {
    val view: Html = enterAmountView(form, viewModel, isAgent, controllers.claimToAdjustPoa.routes.EnterPoaAmountController.submit(isAgent, NormalMode))
    val document: Document = Jsoup.parse(view.toString())
    val groupButton: Elements = document.select("div.govuk-button-group")
    val buttons = groupButton.first().children()
  }

  "The Enter PoA Amount page" should {
    "render the correct constant page content" in new Setup {
      val captionMsg = msgs("claimToAdjustPoa.enterPoaAmount.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)
      val h1Msg = msg("heading")
      document.title shouldBe msgs("htmlTitle", msg("heading"))

      document.getElementById("caption").text shouldBe captionMsg

      document.getElementById("h1").text shouldBe h1Msg

      document.getElementById("bulletPoints").text() shouldBe {msg("p1") + " " + msg("bullet1") + " " + msg("bullet2")}

      document.getElementById("poa-amount").text shouldBe {msg("howMuch") + " " + msg("howMuchHint") + " £ " +
        msgs("base.continue") + " " + msg("cancel")}

      document.getElementsByClass("govuk-input").size() shouldBe 1

      buttons.get(0).text shouldBe msgs("base.continue")

      buttons.get(1).text shouldBe msg("cancel")
      document.getElementById("cancel").attr("href") shouldBe cancelUrl
    }
    "render the table with only Initial Amount for user on first visit" in new Setup(viewModel = poAAmountViewModel()) {
      document.getElementsByClass("govuk-table__head").text() shouldBe {msg("chargeHeading") + " " + msg("amountPreviousHeading")}
      val tableBody = document.getElementsByClass("govuk-table__body")
      tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__header:nth-of-type(1)").text shouldBe msg("firstPayment")
      tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe poAAmountViewModel().relevantAmountOne.toCurrencyString
      tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__header:nth-of-type(1)").text shouldBe msg("secondPayment")
      tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe poAAmountViewModel().relevantAmountTwo.toCurrencyString
    }
    "render the table with Initial and Adjusted amount for user who has decrease previously" in new Setup(viewModel = poAAmountViewModel(poaPreviouslyAdjusted = Some(true))) {
      document.getElementsByClass("govuk-table__head").text() shouldBe {msg("chargeHeading") + " " + msg("amountPreviousHeading") + " " + msg("adjustedAmount")}
      val tableBody = document.getElementsByClass("govuk-table__body")
      tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__header:nth-of-type(1)").text shouldBe msg("firstPayment")
      tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe poAAmountViewModel(poaPreviouslyAdjusted = Some(true)).relevantAmountOne.toCurrencyString
      tableBody.select(".govuk-table__row:nth-of-type(1)").select(".govuk-table__cell:nth-of-type(2)").text shouldBe poAAmountViewModel(poaPreviouslyAdjusted = Some(true)).totalAmountOne.toCurrencyString
      tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__header:nth-of-type(1)").text shouldBe msg("secondPayment")
      tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__cell:nth-of-type(1)").text shouldBe poAAmountViewModel(poaPreviouslyAdjusted = Some(true)).relevantAmountTwo.toCurrencyString
      tableBody.select(".govuk-table__row:nth-of-type(2)").select(".govuk-table__cell:nth-of-type(2)").text shouldBe poAAmountViewModel(poaPreviouslyAdjusted = Some(true)).totalAmountOne.toCurrencyString
    }
    "render the inset text specific to the first adjustment attempt" in new Setup(viewModel = poAAmountViewModel(poaPartiallyPaid = true)) {
      val expectedText: String = (msg("insetText.h2") + " " + msg("insetText.firstAttempt.para1") + " " + msg("insetText.firstAttempt.para2")).replaceAll("<b>", "").replaceAll("</b>", "")
      document.getElementById("insetText-firstAttempt").text() shouldBe expectedText
    }
    "render the inset text specific to the second adjustment attempt" in
      new Setup(viewModel = poAAmountViewModel(poaPartiallyPaid = true, poaPreviouslyAdjusted = Some(true))) {
        val expectedText: String = (msg("insetText.h2") + " " + msg("insetText.secondAttempt.para1") + " " + msg("insetText.secondAttempt.para2")).replaceAll("<b>", "").replaceAll("</b>", "")
        document.getElementById("insetText-secondAttempt").text() shouldBe expectedText
      }
    "not render any inset text if poAs are unpaid" in new Setup(viewModel = poAAmountViewModel()) {
      document.getElementsByClass("govuk-inset-text").toArray().isEmpty shouldBe true
    }
    "render the correct error message" when {
      "no number input" in new Setup(form = noInputErrorForm){
        document.getElementById("poa-amount-error").text() shouldBe {"Error: " + msg("emptyError")}
      }
      "non-number input" in new Setup(form = invalidErrorForm){
        document.getElementById("poa-amount-error").text() shouldBe {"Error: " + msg("invalidError")}
      }
      "number input same as current poa" in new Setup(form = sameErrorForm){
        document.getElementById("poa-amount-error").text() shouldBe {"Error: " + msg("sameError")}
      }
      "number input higher than relevant amount" in new Setup(form = higherErrorForm){
        document.getElementById("poa-amount-error").text() shouldBe {"Error: " + msg("higherError")}
      }
    }
  }

  "The Change Poa Amount page" should {
    "pre-populate the input field with data" when {
      "the form contains an amount" in new Setup(form = prePopulatedForm){
        document.getElementsByClass("govuk-input").attr("value") shouldBe "1200"
      }
    }
  }

}
