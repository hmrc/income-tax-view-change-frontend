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

package views.agent

import forms.agent.ClientsUTRForm
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.EnterClientsUTR

class EnterClientsUTRViewSpec extends ViewSpec {

  object EnterClientsUTRMessages {
    val title = "What is your client’s Unique Taxpayer Reference? - Your client’s Income Tax details - GOV.UK"
    val titleWithError = s"Error: $title"
    val heading = "What is your client’s Unique Taxpayer Reference?"
    val info = "This is the 10-digit number they received when they registered for Self Assessment. For example, 1234567890."
    val errorEmptyUTR = "Enter your client’s Unique Taxpayer Reference"
    val errorWrongLength = "Your client’s Unique Taxpayer Reference must be 10 digits long"
    val errorNonNumeric = "A Unique Taxpayer Reference must only contain numbers"
    val continue = "Continue"
    val errorPrefix = "Error:"
  }

  val enterClientsUTR: EnterClientsUTR = app.injector.instanceOf[EnterClientsUTR]

  val pageWithoutError: Html = enterClientsUTR(ClientsUTRForm.form, testCall)

  def pageWithError(error: String = ClientsUTRForm.utrEmptyError): Html = enterClientsUTR(ClientsUTRForm.form.withError(ClientsUTRForm.utr, error), testCall)

  "The enter clients utr page" when {
    "there is no error on the page" should {
      "have the correct title" in new Setup(pageWithoutError) {
        document.title shouldBe EnterClientsUTRMessages.title
      }
      "have the correct heading" in new Setup(pageWithoutError) {
        layoutContent hasPageHeading EnterClientsUTRMessages.heading
      }
      "have a form with the correct attributes" in new Setup(pageWithoutError) {
        layoutContent.hasFormWith(testCall.method, testCall.url)
      }
      "have an input with associated hint and label" in new Setup(pageWithoutError) {
        val form: Element = layoutContent.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = layoutContent.selectHead(".govuk-hint")

        val input: Element = form.selectHead("input")

        label.text shouldBe EnterClientsUTRMessages.heading
        hint.text shouldBe EnterClientsUTRMessages.info


        label.attr("for") shouldBe input.attr("id")
        input.attr("id") shouldBe ClientsUTRForm.utr
        input.attr("name") shouldBe ClientsUTRForm.utr
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe hint.attr("id")
        hint.attr("id") shouldBe s"${ClientsUTRForm.utr}-hint"
      }
      "have a continue button" in new Setup(pageWithoutError) {
        val button: Element = layoutContent.selectHead("form").selectHead("button")
        button.attr("type") shouldBe "submit"
        button.text shouldBe EnterClientsUTRMessages.continue
      }
    }
    "there is an error on the page" should {
      List(
        ClientsUTRForm.utrEmptyError -> EnterClientsUTRMessages.errorEmptyUTR,
        ClientsUTRForm.utrNonNumeric -> EnterClientsUTRMessages.errorNonNumeric,
        ClientsUTRForm.utrLengthIncorrect -> EnterClientsUTRMessages.errorWrongLength
      ) foreach { case (errorKey, errorMessage) =>
        s"for the error '$errorMessage'" should {
          "have the correct error title" in new Setup(pageWithError(errorKey)) {
            document.title shouldBe EnterClientsUTRMessages.titleWithError
          }
          "have an error summary" in new Setup(pageWithError(errorKey)) {
            layoutContent.hasErrorSummary(ClientsUTRForm.utr -> errorMessage)
          }

          "have the error message display with the input described by it" in new Setup(pageWithError(errorKey)) {
            val form: Element = layoutContent.selectHead("form")
            form.selectHead("div").attr("class").contains("govuk-form-group--error") shouldBe true

            val error: Element = form.selectHead("span")
            val input: Element = form.selectHead("input")

            error.attr("id") shouldBe s"${ClientsUTRForm.utr}-error"
            error.text shouldBe s"${EnterClientsUTRMessages.errorPrefix} $errorMessage"
            val errorPrefix: Element = error.selectHead("span > span")
            errorPrefix.attr("class") shouldBe "govuk-visually-hidden"
            errorPrefix.text shouldBe EnterClientsUTRMessages.errorPrefix

            input.attr("aria-describedby") shouldBe s"${ClientsUTRForm.utr}-hint ${ClientsUTRForm.utr}-error"
          }
        }
      }
    }
  }
}
