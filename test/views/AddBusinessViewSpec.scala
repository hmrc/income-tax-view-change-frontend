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

import forms.BusinessNameForm
import forms.BusinessNameForm.invalidName
import forms.utils.SessionKeys
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.AddBusiness


class AddBusinessViewSpec extends ViewSpec {

  object AddBusinessNameMessages {
    val heading: String =  messages("add-business-name.heading")
    val paragraph1: String = messages("add-business-name.p1")
    val paragraph2: String = messages("add-business-name.p2")
    val errorBusinessNameEmpty: String =  messages("add-business-name.form.error.required")
    val errorBusinessNameLength: String = messages("add-business-name.form.error.maxLength")
    val errorBusinessNameChar: String = messages("add-business-name.form.error.invalidNameFormat")
    val continue: String = messages("base.continue")
    val errorPrefix: String = messages("base.error-prefix")
  }

  val enterBusinessName: AddBusiness = app.injector.instanceOf[AddBusiness]

  val pageWithoutError: Html = enterBusinessName(BusinessNameForm.form, testCall)

  def pageWithError(error: String = BusinessNameForm.businessNameEmptyError): Html = {
    val modifiedForm = BusinessNameForm.form.withError(SessionKeys.businessName, error)
      .fill(BusinessNameForm(invalidName))
    enterBusinessName(modifiedForm, testCall)
  }
  "The add business name page" when {
    "there is no error on the page" should {
      "have the correct heading" in new Setup(pageWithoutError) {
        layoutContent hasPageHeading AddBusinessNameMessages.heading
      }
      "have a form with the correct attributes" in new Setup(pageWithoutError) {
        layoutContent.hasFormWith(testCall.method, testCall.url)
      }
      "have an input with associated hint and label" in new Setup(pageWithoutError) {
        val form: Element = layoutContent.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = layoutContent.selectHead(".govuk-hint")

        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading


        label.attr("for") shouldBe input.attr("id")
        input.attr("id") shouldBe SessionKeys.businessName
        input.attr("name") shouldBe SessionKeys.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint"}
      "have a continue button" in new Setup(pageWithoutError) {
        val button: Element = layoutContent.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }

    "there is an input error on the page" should {
      List(
        BusinessNameForm.businessNameEmptyError -> AddBusinessNameMessages.errorBusinessNameEmpty,
        BusinessNameForm.businessNameLengthIncorrect -> AddBusinessNameMessages.errorBusinessNameLength,
        BusinessNameForm.businessNameInvalidChar -> AddBusinessNameMessages.errorBusinessNameChar
      ) foreach { case (errorKey, errorMessage) =>
        s"for the error '$errorMessage'" should {

          "have the error message display with the input described by it" in new Setup(pageWithError(errorKey)) {
            val form: Element = layoutContent.selectHead("form")
            form.selectHead("div").attr("class").contains("govuk-form-group--error") shouldBe true


            val error: Element = form.selectHead("span")
            val input: Element = form.selectHead("input")

            error.attr("id") shouldBe s"${SessionKeys.businessName}-error"
            error.text shouldBe s"${AddBusinessNameMessages.errorPrefix} $errorMessage"
            val errorPrefix: Element = error.selectHead("span > span")
            errorPrefix.attr("class") shouldBe "govuk-visually-hidden"
            errorPrefix.text shouldBe AddBusinessNameMessages.errorPrefix

            input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint ${SessionKeys.businessName}-error"
          }
        }
      }
    }
  }
}
