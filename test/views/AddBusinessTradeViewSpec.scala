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

package views

import forms.incomeSources.add.BusinessTradeForm
import forms.utils.SessionKeys
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.AddBusinessTrade

class AddBusinessTradeViewSpec extends ViewSpec {

  object AddBusinessTradeMessages {
    val heading: String = messages("add-business-trade.heading")
    val p1: String = messages("add-business-trade.p1")
    val tradeEmptyError: String = messages("add-business-trade.form.error.empty")
    val tradeShortError: String = messages("add-business-trade.form.error.short")
    val tradeLongError: String = messages("add-business-trade.form.error.long")
    val tradeInvalidCharError: String = messages("add-business-trade.form.error.invalid")
    val tradeSameNameError: String = messages("add-business-trade.form.error.same-name")
    val continue: String = messages("base.continue")
    val errorPrefix: String = messages("base.error-prefix")
  }

  val backUrl: String = controllers.routes.AddBusinessStartDate.show().url
  val agentBackUrl: String = controllers.routes.AddBusinessStartDate.showAgent().url

  val enterBusinessTrade: AddBusinessTrade = app.injector.instanceOf[AddBusinessTrade]

  val pageWithoutError: Html = enterBusinessTrade(BusinessTradeForm.form, testCall, false, backUrl, false)

  def pageWithError(error: String = BusinessTradeForm.tradeEmptyError): Html = {
    val modifiedForm = BusinessTradeForm.form.withError(SessionKeys.businessTrade, error)
      .fill(BusinessTradeForm("??Invalid Name??"))
    enterBusinessTrade(modifiedForm, testCall, false, backUrl, false)
  }

  "The add business trade page" when {
    "There are no errors to display" should {
      "Display the correct heading" in new Setup(pageWithoutError) {
        layoutContent hasPageHeading AddBusinessTradeMessages.heading
      }
      "have a form with the correct attributes" in new Setup(pageWithoutError) {
        layoutContent.hasFormWith(testCall.method, testCall.url)
      }
      "have an input with associated hint and label" in new Setup(pageWithoutError) {
        val form: Element = layoutContent.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = layoutContent.selectHead(".govuk-hint")


        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessTradeMessages.heading


        label.attr("for") shouldBe input.attr("id")
        input.attr("id") shouldBe SessionKeys.businessTrade
        input.attr("name") shouldBe SessionKeys.businessTrade
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${SessionKeys.businessTrade}-hint"
      }
      "have a continue button" in new Setup(pageWithoutError) {
        val button: Element = layoutContent.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessTradeMessages.continue
      }
    }

    "there is an input error on the page" should {
      List(
        BusinessTradeForm.tradeEmptyError -> AddBusinessTradeMessages.tradeEmptyError,
        BusinessTradeForm.tradeShortError -> AddBusinessTradeMessages.tradeShortError,
        BusinessTradeForm.tradeLongError -> AddBusinessTradeMessages.tradeLongError,
        BusinessTradeForm.tradeInvalidCharError -> AddBusinessTradeMessages.tradeInvalidCharError,
        BusinessTradeForm.tradeSameNameError -> AddBusinessTradeMessages.tradeSameNameError
      ) foreach { case (errorKey, errorMessage) =>
        s"for the error '$errorMessage'" should {

          "have the error message display with the input described by it" in new Setup(pageWithError(errorKey)) {
            val form: Element = layoutContent.selectHead("form")
            form.selectHead("div").attr("class").contains("govuk-form-group--error") shouldBe true


            val error: Element = form.selectHead("span")
            val input: Element = form.selectHead("input")

            error.attr("id") shouldBe s"${SessionKeys.businessTrade}-error-2"
            error.text shouldBe s"${AddBusinessTradeMessages.errorPrefix} $errorMessage"
            val errorPrefix: Element = error.selectHead("span > span")
            errorPrefix.attr("class") shouldBe "govuk-visually-hidden"
            errorPrefix.text shouldBe AddBusinessTradeMessages.errorPrefix

            input.attr("aria-describedby") shouldBe s"${SessionKeys.businessTrade}-hint ${SessionKeys.businessTrade}-error"
          }
        }
      }
    }
  }

}
