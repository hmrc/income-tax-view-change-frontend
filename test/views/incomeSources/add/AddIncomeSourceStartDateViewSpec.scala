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

import auth.MtdItUser
import forms.incomeSources.add.AddIncomeSourceStartDateForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.test.Helpers.contentAsString
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.add.AddIncomeSourceStartDate

class AddIncomeSourceStartDateViewSpec extends TestSupport {

  val addIncomeSourceStartDate: AddIncomeSourceStartDate = app.injector.instanceOf[AddIncomeSourceStartDate]

  val testUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None,
    incomeSources = noIncomeDetails
  )(fakeRequestCeaseUKPropertyDeclarationComplete)

  class Setup(isAgent: Boolean, error: Boolean = false, incomeSourceType: IncomeSourceType) extends TestSupport {

    lazy val document: Document = (isAgent, incomeSourceType, error) match {
      case (true, ForeignProperty, false) =>
        val messagesPrefix = "incomeSources.add.foreignProperty.startDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (false, ForeignProperty, false) =>
        val messagesPrefix = "incomeSources.add.foreignProperty.startDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (_, ForeignProperty, true) =>
        val messagesPrefix = "incomeSources.add.foreignProperty.startDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (false, UKProperty, false) =>
        val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (true, UKProperty, false) =>
        val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (_, UKProperty, true) =>
        val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (false, SoleTraderBusiness, false) =>
        val messagesPrefix = "add-business-start-date"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (true, SoleTraderBusiness, false) =>
        val messagesPrefix = "add-business-start-date"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusinessAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (_, SoleTraderBusiness, true) =>
        val messagesPrefix = "add-business-start-date"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
    }
  }

  "AddIncomeSourceStartDateView - Foreign Property - Individual" should {
    "render the heading" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.heading")
    }
    "render the hint" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.foreignProperty.startDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - UK Property - Individual" should {
    "render the heading" in new Setup(isAgent = false, error = false, incomeSourceType = UKProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.heading")
    }
    "render the hint" in new Setup(isAgent = false, error = false, incomeSourceType = UKProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.UKPropertyStartDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = false, error = false, incomeSourceType = UKProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, error = false, incomeSourceType = UKProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, error = false, incomeSourceType = UKProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = false, error = true, incomeSourceType = UKProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = false, error = true, incomeSourceType = UKProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - Sole Trader Business - Individual" should {
    "render the heading" in new Setup(isAgent = false, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("add-business-start-date.heading")
    }
    "render the hint" in new Setup(isAgent = false, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("add-business-start-date.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = false, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = false, error = true, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("add-business-start-date.error.required")
    }
    "render the error summary" in new Setup(isAgent = false, error = true, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-start-date.error.required")
    }
  }


  "AddIncomeSourceStartDateView - Foreign Property - Agent" should {
    "render the heading" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.heading")
    }
    "render the hint" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.foreignProperty.startDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the continue button" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = true, error = true, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = true, error = true, incomeSourceType = ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - UK Property - Agent" should {
    "render the heading" in new Setup(isAgent = true, error = false, incomeSourceType = UKProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.heading")
    }
    "render the hint" in new Setup(isAgent = true, error = false, incomeSourceType = UKProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.UKPropertyStartDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = true, error = false, incomeSourceType = UKProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, error = false, incomeSourceType = UKProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the continue button" in new Setup(isAgent = true, error = false, incomeSourceType = UKProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = true, error = true, incomeSourceType = UKProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = true, error = true, incomeSourceType = UKProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - Sole Trader Business - Agent" should {
    "render the heading" in new Setup(isAgent = true, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("add-business-start-date.heading")
    }
    "render the hint" in new Setup(isAgent = true, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("add-business-start-date.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = true, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the continue button" in new Setup(isAgent = true, error = false, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = true, error = true, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("add-business-start-date.error.required")
    }
    "render the error summary" in new Setup(isAgent = true, error = true, incomeSourceType = SoleTraderBusiness) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-start-date.error.required")
    }
  }

  private sealed trait IncomeSourceType
  private case object UKProperty extends IncomeSourceType
  private case object ForeignProperty extends IncomeSourceType
  private case object SoleTraderBusiness extends IncomeSourceType
}