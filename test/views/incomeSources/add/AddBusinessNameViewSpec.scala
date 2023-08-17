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

package views.incomeSources.add

import forms.BusinessNameForm
import forms.BusinessNameForm.invalidName
import forms.IncomeSourcesFormsSpec.businessNameForm
import forms.incomeSources.add.CheckUKPropertyStartDateForm
import forms.utils.SessionKeys
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.{Html, HtmlFormat}
import services.DateService
import testUtils.ViewSpec
import views.html.incomeSources.add.{AddBusinessName, CheckUKPropertyStartDate}

import java.time.LocalDate

class AddBusinessNameViewSpec extends ViewSpec {

  val addBusinessName: AddBusinessName = app.injector.instanceOf[AddBusinessName]
  class Setup(isAgent: Boolean, error: Boolean = false, isChange: Boolean)(errorKey: String) {
    val addBusinessNameForm: Form[BusinessNameForm] = BusinessNameForm.form

    val testBusinessName: String = "Test Business"
    val postAction: Call = {
      if (isChange) {
        if (isAgent) {
          controllers.incomeSources.add.routes.AddBusinessNameController.submitChangeAgent()
        } else {
          controllers.incomeSources.add.routes.AddBusinessNameController.submitChange()
        }
      } else {
        if (isAgent) controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent() else
          controllers.incomeSources.add.routes.AddBusinessNameController.submit()
      }
    }

    val backUrl: String = {
      if (isChange) {
        if (isAgent) {
          controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
        } else {
          controllers.incomeSources.add.routes.CheckBusinessDetailsController.show.url
        }
      } else {
        if (isAgent) controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent.url else
          controllers.incomeSources.add.routes.AddIncomeSourceController.show.url
      }
    }

    lazy val view: HtmlFormat.Appendable = {
      addBusinessName(
        addBusinessNameForm,
        isAgent,
        postAction,
        backUrl)(messages, implicitly)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = {
      addBusinessName(
        addBusinessNameForm.withError(FormError("addBusinessName",
          "add-business-name.form.error.required")),
        isAgent,
        postAction,
        backUrl,
      )(messages, implicitly)
    }

    lazy val changeView: HtmlFormat.Appendable = {
      addBusinessName(
        addBusinessNameForm,
        isAgent,
        postAction,
        backUrl)(messages, implicitly)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  object AddBusinessNameMessages {
    val heading: String = messages("add-business-name.heading")
    val paragraph1: String = messages("add-business-name.p1")
    val paragraph2: String = messages("add-business-name.p2")
    val errorBusinessNameEmpty: String = messages("add-business-name.form.error.required")
    val errorBusinessNameLength: String = messages("add-business-name.form.error.maxLength")
    val errorBusinessNameChar: String = messages("add-business-name.form.error.invalidNameFormat")
    val continue: String = messages("base.continue")
    val errorPrefix: String = messages("base.error-prefix")
  }


  val testChangeCall: Call = Call("POST", "/test-change-url")

  "AddBusinessNameView - ADD - Individual" when {
    "there is no error on the add page" should {
      "have the correct heading" in new Setup(false, false, false)("") {
        document hasPageHeading AddBusinessNameMessages.heading
      }
      "render the back link with the correct URL" in new Setup(false, false, false)("") {
        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
      }
      "have a form with the correct attributes" in new Setup(false, false, false)("") {
        document.hasFormWith(testCall.method, postAction.url)
        print(postAction.url)
        println("LOOOOK")
      }
      "have an input with associated hint and label" in new Setup(false, false, false)("") {
        val form: Element = document.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = document.selectHead(".govuk-hint")

        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading
        label.attr("for") shouldBe input.attr("id")
        input.attr("id") shouldBe SessionKeys.businessName
        input.attr("name") shouldBe SessionKeys.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint"
        input.attr("value") shouldBe("")

      }
      "have a continue button" in new Setup(false, false, false)("") {
        val button: Element = document.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }

    "there is an input error on the add page" should {
      List(
        BusinessNameForm.businessNameEmptyError -> AddBusinessNameMessages.errorBusinessNameEmpty,
        BusinessNameForm.businessNameLengthIncorrect -> AddBusinessNameMessages.errorBusinessNameLength,
        BusinessNameForm.businessNameInvalidChar -> AddBusinessNameMessages.errorBusinessNameChar
      ) foreach { case (errorKey, errorMessage) =>
        s"for the error '$errorMessage'" should {

          "have the error message display with the input described by it" in new Setup(false, true, false)(errorKey) {
            val form: Element = document.selectHead("form")
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

  "AddBusinessNameView - CHANGE - Individual" when {
    "there is no error on the change page" should {
      "have the correct heading" in new Setup(false, false, true)("") {
        document hasPageHeading AddBusinessNameMessages.heading
      }
      "render the back link with the correct URL" in new Setup(false, false, true)(""){

        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
      }
      "have a form with the correct attributes" in new Setup(false, false, true)("") {

        document.hasFormWith(testChangeCall.method, postAction.url)
        print(postAction.url)
        println("LOOOOK")
      }
      "have an input with associated hint and label" in new Setup(false, false, true)("") {
        val form: Element = document.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = document.selectHead(".govuk-hint")
        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading

        label.attr("for") shouldBe input.attr("id")
        input.attr("id") shouldBe SessionKeys.businessName
        input.attr("name") shouldBe SessionKeys.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint"
        input.attr("value") shouldBe(testBusinessName)
      }
      "have a continue button" in new Setup(false, false, true)("") {
        val button: Element = document.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }

    "there is an input error on the change page" should {
      List(
        BusinessNameForm.businessNameEmptyError -> AddBusinessNameMessages.errorBusinessNameEmpty,
        BusinessNameForm.businessNameLengthIncorrect -> AddBusinessNameMessages.errorBusinessNameLength,
        BusinessNameForm.businessNameInvalidChar -> AddBusinessNameMessages.errorBusinessNameChar
      ) foreach { case (errorKey, errorMessage) =>
        s"for the error '$errorMessage'" should {

          "have the error message display with the input described by it" in new Setup(false, true, true)(errorKey) {
            val form: Element = document.selectHead("form")
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
//
//  "AddBusinessNameView - ADD - Agent" when {
//    "there is no error on the add page" should {
//      "have the correct heading" in new Setup(true, false, false)("") {
//        document hasPageHeading AddBusinessNameMessages.heading
//      }
//      "render the back link with the correct URL" in new Setup(true, false, false)("") {
//        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
//        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
//      }
//      "have a form with the correct attributes" in new Setup(true, false, false)("") {
//        document.hasFormWith(testCall.method, postAction.url)
//      }
//      "have an input with associated hint and label" in new Setup(true, false, false)("") {
//        val form: Element = document.selectHead("form")
//        val label: Element = form.selectHead("label")
//        val hint: Element = document.selectHead(".govuk-hint")
//
//        val input: Element = form.selectHead("input")
//
//        label.text shouldBe AddBusinessNameMessages.heading
//        label.attr("for") shouldBe input.attr("id")
//        input.attr("id") shouldBe SessionKeys.businessName
//        input.attr("name") shouldBe SessionKeys.businessName
//        input.attr("type") shouldBe "text"
//        input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint"
//        input.attr("value") shouldBe ("")
//
//      }
//      "have a continue button" in new Setup(true, false, false)("") {
//        val button: Element = document.selectHead("form").selectHead("button")
//        button.text shouldBe AddBusinessNameMessages.continue
//      }
//    }
//
//    "there is an input error on the add page" should {
//      List(
//        BusinessNameForm.businessNameEmptyError -> AddBusinessNameMessages.errorBusinessNameEmpty,
//        BusinessNameForm.businessNameLengthIncorrect -> AddBusinessNameMessages.errorBusinessNameLength,
//        BusinessNameForm.businessNameInvalidChar -> AddBusinessNameMessages.errorBusinessNameChar
//      ) foreach { case (errorKey, errorMessage) =>
//        s"for the error '$errorMessage'" should {
//
//          "have the error message display with the input described by it" in new Setup(true, true, false)(errorKey) {
//            val form: Element = document.selectHead("form")
//            form.selectHead("div").attr("class").contains("govuk-form-group--error") shouldBe true
//
//            val error: Element = form.selectHead("span")
//            val input: Element = form.selectHead("input")
//
//            error.attr("id") shouldBe s"${SessionKeys.businessName}-error"
//            error.text shouldBe s"${AddBusinessNameMessages.errorPrefix} $errorMessage"
//            val errorPrefix: Element = error.selectHead("span > span")
//            errorPrefix.attr("class") shouldBe "govuk-visually-hidden"
//            errorPrefix.text shouldBe AddBusinessNameMessages.errorPrefix
//
//            input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint ${SessionKeys.businessName}-error"
//          }
//        }
//      }
//    }
//  }
//
//  "AddBusinessNameView - CHANGE - Agent" when {
//    "there is no error on the change page" should {
//      "have the correct heading" in new Setup(true, false, true)("") {
//        document hasPageHeading AddBusinessNameMessages.heading
//      }
//      "render the back link with the correct URL" in new Setup(true, false, true)("") {
//        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
//        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
//      }
//      "have a form with the correct attributes" in new Setup(true, false, true)("") {
//        document.hasFormWith(testChangeCall.method, postAction.url)
//      }
//      "have an input with associated hint and label" in new Setup(true, false, true)("") {
//        val form: Element = document.selectHead("form")
//        val label: Element = form.selectHead("label")
//        val hint: Element = document.selectHead(".govuk-hint")
//        val input: Element = form.selectHead("input")
//
//        label.text shouldBe AddBusinessNameMessages.heading
//
//        label.attr("for") shouldBe input.attr("id")
//        input.attr("id") shouldBe SessionKeys.businessName
//        input.attr("name") shouldBe SessionKeys.businessName
//        input.attr("type") shouldBe "text"
//        input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint"
//        input.attr("value") shouldBe (testBusinessName)
//      }
//      "have a continue button" in new Setup(true, false, true)("") {
//        val button: Element = document.selectHead("form").selectHead("button")
//        button.text shouldBe AddBusinessNameMessages.continue
//      }
//    }
//
//    "there is an input error on the change page" should {
//      List(
//        BusinessNameForm.businessNameEmptyError -> AddBusinessNameMessages.errorBusinessNameEmpty,
//        BusinessNameForm.businessNameLengthIncorrect -> AddBusinessNameMessages.errorBusinessNameLength,
//        BusinessNameForm.businessNameInvalidChar -> AddBusinessNameMessages.errorBusinessNameChar
//      ) foreach { case (errorKey, errorMessage) =>
//        s"for the error '$errorMessage'" should {
//
//          "have the error message display with the input described by it" in new Setup(true, true, true)(errorKey) {
//            val form: Element = document.selectHead("form")
//            form.selectHead("div").attr("class").contains("govuk-form-group--error") shouldBe true
//
//            document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(errorKey)
//
//            println(s"error key -  $errorKey")
//            println(s"error message -  $errorMessage")
//
//            val error: Element = form.selectHead("span")
//            val input: Element = form.selectHead("input")
//
//            error.attr("id") shouldBe s"${SessionKeys.businessName}-error"
//            error.text shouldBe s"${AddBusinessNameMessages.errorPrefix} $errorMessage"
//            val errorPrefix: Element = error.selectHead("span > span")
//            errorPrefix.attr("class") shouldBe "govuk-visually-hidden"
//            errorPrefix.text shouldBe AddBusinessNameMessages.errorPrefix
//
//            input.attr("aria-describedby") shouldBe s"${SessionKeys.businessName}-hint ${SessionKeys.businessName}-error"
//          }
//        }
//      }
//    }
//  }
}
