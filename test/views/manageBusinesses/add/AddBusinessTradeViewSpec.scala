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

package views.manageBusinesses.add

import enums.IncomeSourceJourney.SelfEmployment
import forms.manageBusinesses.add.BusinessTradeForm
import models.core.NormalMode
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.manageBusinesses.add.AddBusinessTrade

class AddBusinessTradeViewSpec extends ViewSpec {

  object AddBusinessTradeMessages {
    val title: String = messages("incomeSources.add.sole-trader")
    val heading: String = messages("add-trade.heading")
    val p1: String = messages("add-trade.trade-info-1")
    val p2: String = messages("add-trade.trade-info-2")
    val tradeEmptyError: String = messages("add-trade.form.error.empty")
    val tradeShortError: String = messages("add-trade.form.error.short")
    val tradeLongError: String = messages("add-trade.form.error.long")
    val tradeInvalidCharError: String = messages("add-trade.form.error.invalid")
    val tradeSameNameError: String = messages("add-trade.form.error.same-name")
    val continue: String = messages("base.continue")
    val errorPrefix: String = messages("base.error-prefix")
  }

  val backUrl: String = {
    controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, mode = NormalMode).url
  }
  val agentBackUrl: String = {
    controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, mode = NormalMode).url
  }

  val addBusinessTradeView: AddBusinessTrade = app.injector.instanceOf[AddBusinessTrade]

  val pageWithoutError: Html = addBusinessTradeView(BusinessTradeForm.form, testCall, isAgent = false, backUrl)
  val changePageWithoutError: Html = addBusinessTradeView(BusinessTradeForm.form.fill(BusinessTradeForm("Oops wrong trade")),
    testCall, isAgent = false, backUrl)

  def pageWithError(error: String = BusinessTradeForm.tradeEmptyError): Html = {
    val modifiedForm = BusinessTradeForm.form.withError(BusinessTradeForm.businessTrade, error)
      .fill(BusinessTradeForm("??Invalid Name??"))
    addBusinessTradeView(modifiedForm, testCall, isAgent = false, backUrl)
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
        val input: Element = form.selectHead("input")

        layoutContent.getElementById("caption").text() shouldBe AddBusinessTradeMessages.title
        label.text shouldBe AddBusinessTradeMessages.heading
        label.attr("for") shouldBe input.attr("id")
        input.attr("id") shouldBe BusinessTradeForm.businessTrade
        input.attr("name") shouldBe BusinessTradeForm.businessTrade
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${BusinessTradeForm.businessTrade}-hint"
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

          "render the error summary" in new Setup(pageWithError(errorKey)) {
            layoutContent.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
            layoutContent.getElementsByClass("govuk-error-summary__body").text() shouldBe errorMessage
          }

          "render the error message" in new Setup(pageWithError(errorKey)) {
            layoutContent.getElementById("business-trade-error").text() shouldBe s"${AddBusinessTradeMessages.errorPrefix} $errorMessage"
          }
        }
      }
    }
    "pre-populate the previously saved business trade on change route" in new Setup(changePageWithoutError) {
      document.getElementById("business-trade").attr("value") shouldBe "Oops wrong trade"
    }
  }

}
