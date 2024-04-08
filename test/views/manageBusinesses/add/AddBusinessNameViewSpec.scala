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
import forms.incomeSources.add.BusinessNameForm
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.ViewSpec
import views.html.manageBusinesses.add.AddBusinessName

class AddBusinessNameViewSpec extends ViewSpec {

  val addBusinessName: AddBusinessName = app.injector.instanceOf[AddBusinessName]

  class TestSetup(isAgent: Boolean, error: Boolean = false, isChange: Boolean) {
    val addBusinessNameForm: Form[BusinessNameForm] = BusinessNameForm.form
    val changeBusinessNameForm: Form[BusinessNameForm] = BusinessNameForm.form.fill(BusinessNameForm("Test Business"))
    val testBusinessName: String = "Test Business"
    val testChangeCall: Call = Call("POST", "/test-change-url")

    val postAction: Call = {
      if (isChange) {
        if (isAgent) {
          controllers.manageBusinesses.add.routes.AddBusinessNameController.submit(isAgent = true, isChange = true)
        } else {
          controllers.manageBusinesses.add.routes.AddBusinessNameController.submit(isAgent = false, isChange = true)
        }
      } else {
        if (isAgent) controllers.manageBusinesses.add.routes.AddBusinessNameController.submit(isAgent = true, isChange = false) else
          controllers.manageBusinesses.add.routes.AddBusinessNameController.submit(isAgent = false, isChange = false)
      }
    }

    val backUrl: String = {
      if (isChange) {
        if (isAgent) {
          controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url
        } else {
          controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
        }
      } else {
        if (isAgent) controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent().url else
          controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url
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
        addBusinessNameForm.withError(FormError(BusinessNameForm.businessName,
          "add-business-name.form.error.required")),
        isAgent,
        postAction,
        backUrl)(messages, implicitly)
    }

    lazy val changeView: HtmlFormat.Appendable = {
      addBusinessName(
        changeBusinessNameForm,
        isAgent,
        postAction,
        backUrl)(messages, implicitly)
    }

    lazy val changeViewWithError: HtmlFormat.Appendable = {
      addBusinessName(
        changeBusinessNameForm.withError(FormError(BusinessNameForm.businessName,
          "add-business-name.form.error.required")),
        isAgent,
        postAction,
        backUrl)(messages, implicitly)
    }

    lazy val document: Document = {
      if (isChange) {
        if (error) {
          Jsoup.parse(contentAsString(changeViewWithError))
        } else {
          Jsoup.parse(contentAsString(changeView))
        }
      } else {
        if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else
          Jsoup.parse(contentAsString(view))
      }
    }
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

  "AddBusinessNameView - ADD - Individual" when {
    "there is no error on the add page" should {
      "have the correct heading" in new TestSetup(false, false, false) {
        document.getElementsByClass("govuk-caption-l").text() shouldBe messages("incomeSources.add.sole-trader")
        document hasPageHeading AddBusinessNameMessages.heading
      }
      "render the back link with the correct URL" in new TestSetup(false, false, false) {
        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
      }
      "have a form with the correct attributes" in new TestSetup(false, false, false) {
        document.hasFormWith(testCall.method, postAction.url)
      }
      "have an input with associated hint and label" in new TestSetup(false, false, false) {
        val form: Element = document.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = document.selectHead(".govuk-hint")

        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading
        label.attr("for") shouldBe input.attr("id")
        hint.text contains AddBusinessNameMessages.paragraph1
        input.attr("id") shouldBe BusinessNameForm.businessName
        input.attr("name") shouldBe BusinessNameForm.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${BusinessNameForm.businessName}-hint"
        input.attr("value") shouldBe ("")

      }
      "have a continue button" in new TestSetup(false, false, false) {
        val button: Element = document.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }
    "there is an error on the page" should {
      "render the error summary" in new TestSetup(false, true, false) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-name.form.error.required")
      }
      "render the error message" in new TestSetup(false, true, false) {
        document.getElementById("business-name-error").text() shouldBe
          s"${messages("base.error-prefix")} ${messages("add-business-name.form.error.required")}"
      }
    }
  }

  "AddBusinessNameView - CHANGE - Individual" when {
    "there is no error on the change page" should {
      "have the correct heading" in new TestSetup(false, false, true) {
        document.getElementsByClass("govuk-caption-l").text() shouldBe messages("incomeSources.add.sole-trader")
        document hasPageHeading AddBusinessNameMessages.heading
      }
      "render the back link with the correct URL" in new TestSetup(false, false, true) {

        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
      }
      "have a form with the correct attributes" in new TestSetup(false, false, true) {
        document.hasFormWith(testChangeCall.method, postAction.url)
      }
      "have an input with associated hint and label" in new TestSetup(false, false, true) {
        val form: Element = document.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = document.selectHead(".govuk-hint")
        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading
        label.attr("for") shouldBe input.attr("id")
        hint.text contains AddBusinessNameMessages.paragraph1
        input.attr("id") shouldBe BusinessNameForm.businessName
        input.attr("name") shouldBe BusinessNameForm.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${BusinessNameForm.businessName}-hint"
        input.attr("value") shouldBe (testBusinessName)
      }
      "have a continue button" in new TestSetup(false, false, true) {
        val button: Element = document.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }
    "there is an error on the page" should {
      "render the error summary" in new TestSetup(false, true, true) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-name.form.error.required")
      }
      "render the error message" in new TestSetup(false, true, true) {
        document.getElementById("business-name-error").text() shouldBe
          s"${messages("base.error-prefix")} ${messages("add-business-name.form.error.required")}"
      }
    }
  }

  "AddBusinessNameView - ADD - Agent" when {
    "there is no error on the add page" should {
      "have the correct heading" in new TestSetup(true, false, false) {
        document.getElementsByClass("govuk-caption-l").text() shouldBe messages("incomeSources.add.sole-trader")
        document hasPageHeading AddBusinessNameMessages.heading
      }
      "render the back link with the correct URL" in new TestSetup(true, false, false) {
        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
      }
      "have a form with the correct attributes" in new TestSetup(true, false, false) {
        document.hasFormWith(testCall.method, postAction.url)
      }
      "have an input with associated hint and label" in new TestSetup(true, false, false) {
        val form: Element = document.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = document.selectHead(".govuk-hint")

        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading
        label.attr("for") shouldBe input.attr("id")
        hint.text contains AddBusinessNameMessages.paragraph1
        input.attr("id") shouldBe BusinessNameForm.businessName
        input.attr("name") shouldBe BusinessNameForm.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${BusinessNameForm.businessName}-hint"
        input.attr("value") shouldBe ("")

      }
      "have a continue button" in new TestSetup(true, false, false) {
        val button: Element = document.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }
    "there is an error on the page" should {
      "render the error summary" in new TestSetup(true, true, false) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-name.form.error.required")
      }
      "render the error message" in new TestSetup(true, true, false) {
        document.getElementById("business-name-error").text() shouldBe
          s"${messages("base.error-prefix")} ${messages("add-business-name.form.error.required")}"
      }
    }
  }

  "AddBusinessNameView - CHANGE - Agent" when {
    "there is no error on the change page" should {
      "have the correct heading" in new TestSetup(true, false, true) {
        document.getElementsByClass("govuk-caption-l").text() shouldBe messages("incomeSources.add.sole-trader")
        document hasPageHeading AddBusinessNameMessages.heading
      }
      "render the back link with the correct URL" in new TestSetup(true, false, true) {
        document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
        document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl
      }
      "have a form with the correct attributes" in new TestSetup(true, false, true) {
        document.hasFormWith(testChangeCall.method, postAction.url)
      }
      "have an input with associated hint and label" in new TestSetup(true, false, true) {
        val form: Element = document.selectHead("form")
        val label: Element = form.selectHead("label")
        val hint: Element = document.selectHead(".govuk-hint")
        val input: Element = form.selectHead("input")

        label.text shouldBe AddBusinessNameMessages.heading
        label.attr("for") shouldBe input.attr("id")
        hint.text contains AddBusinessNameMessages.paragraph1
        input.attr("id") shouldBe BusinessNameForm.businessName
        input.attr("name") shouldBe BusinessNameForm.businessName
        input.attr("type") shouldBe "text"
        input.attr("aria-describedby") shouldBe s"${BusinessNameForm.businessName}-hint"
        input.attr("value") shouldBe (testBusinessName)
      }
      "have a continue button" in new TestSetup(true, false, true) {
        val button: Element = document.selectHead("form").selectHead("button")
        button.text shouldBe AddBusinessNameMessages.continue
      }
    }
    "there is an error on the page" should {
      "render the error summary" in new TestSetup(true, true, false) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-name.form.error.required")
      }
      "render the error message" in new TestSetup(true, true, false) {
        document.getElementById("business-name-error").text() shouldBe
          s"${messages("base.error-prefix")} ${messages("add-business-name.form.error.required")}"
      }
    }
  }
}
