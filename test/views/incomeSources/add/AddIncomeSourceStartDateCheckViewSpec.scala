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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import services.DateService
import testUtils.TestSupport
import views.html.incomeSources.add.AddIncomeSourceStartDateCheck

import java.time.LocalDate

class AddIncomeSourceStartDateCheckViewSpec extends TestSupport {

  val addIncomeSourceStartDateCheck: AddIncomeSourceStartDateCheck = app.injector.instanceOf[AddIncomeSourceStartDateCheck]


  class Setup(isAgent: Boolean, error: Boolean, incomeSourceType: IncomeSourceType) {

    val mockDateService: DateService = app.injector.instanceOf[DateService]
    val startDate: String = "2022-06-30"
    val formattedStartDate: String = mockImplicitDateFormatter.longDate(LocalDate.parse(startDate)).toLongDate

    lazy val document: Document = (isAgent, error, incomeSourceType) match {
      case (false, false, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (false, true, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix)
              .withError(FormError("start-date-check", s"${ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (true, false, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (true, true, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix)
              .withError(FormError("start-date-check", s"${ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (false, false, UkProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(UkProperty.addIncomeSourceStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (false, true, UkProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(UkProperty.addIncomeSourceStartDateCheckMessagesPrefix)
              .withError(FormError("start-date-check", s"${UkProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (true, false, UkProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(UkProperty.addIncomeSourceStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (true, true, UkProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(UkProperty.addIncomeSourceStartDateCheckMessagesPrefix)
              .withError(FormError("start-date-check", s"${UkProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKPropertyAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (false, false, SelfEmployment) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (false, true, SelfEmployment) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix)
              .withError(FormError("start-date-check", s"${SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix}.error")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (true, false, SelfEmployment) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusinessAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
      case (true, true, SelfEmployment) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDateCheck(
            form = AddIncomeSourceStartDateCheckForm(SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix)
              .withError(FormError("start-date-check", s"${SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix}.error")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusinessAgent,
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url,
            incomeSourceStartDate = formattedStartDate
          )
        ))
    }
  }

  "AddIncomeSourceStartDateCheck page - Individual - UK Property" should {
    "render the heading" in new Setup(false, false, UkProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add Income Source Start Date page" in new Setup(false, false, UkProperty) {
      document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(false, false, UkProperty) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(false, false, UkProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url
    }
    "render the continue button" in new Setup(false, false, UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true, UkProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${UkProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(false, true, UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${UkProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the back url" in new Setup(false, true, UkProperty) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKProperty.url
    }
  }

  "AddIncomeSourceStartDateCheck page - Agent - UK Property" should {
    "render the heading" in new Setup(true, false, UkProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add Income Source Start Date page" in new Setup(true, false, UkProperty) {
      document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(true, false, UkProperty) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(true, false, UkProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url
    }
    "render the continue button" in new Setup(true, false, UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true, UkProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${UkProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(true, true, UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${UkProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the back url" in new Setup(true, true, UkProperty) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showUKPropertyAgent.url
    }
  }

  "AddIncomeSourceStartDateCheck page - Individual - Foreign Property" should {
    "render the heading" in new Setup(false, false, ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add Income Source Start Date page" in new Setup(false, false, ForeignProperty) {
      document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(false, false, ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(false, false, ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty.url
    }
    "render the continue button" in new Setup(false, false, ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true, ForeignProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(false, true, ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the back url" in new Setup(false, true, ForeignProperty) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignProperty.url
    }
  }

  "AddIncomeSourceStartDateCheck page - Agent - Foreign Property" should {
    "render the heading" in new Setup(true, false, ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add Income Source Start Date page" in new Setup(true, false, ForeignProperty) {
      document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(true, false, ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(true, false, ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url
    }
    "render the continue button" in new Setup(true, false, ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true, ForeignProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(true, true, ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${ForeignProperty.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the back url" in new Setup(true, true, ForeignProperty) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url
    }
  }

  "AddIncomeSourceStartDateCheck page - Individual - Sole Trader Business" should {
    "render the heading" in new Setup(false, false, SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add Income Source Start Date page" in new Setup(false, false, SelfEmployment) {
      document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(false, false, SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(false, false, SelfEmployment) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url
    }
    "render the continue button" in new Setup(false, false, SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true, SelfEmployment) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(false, true, SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the back url" in new Setup(false, true, SelfEmployment) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusiness.url
    }
  }

  "AddIncomeSourceStartDateCheck page - Agent - Sole Trader Business" should {
    "render the heading" in new Setup(true, false, SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
    }
    "render the date entered in Add Income Source Start Date page" in new Setup(true, false, SelfEmployment) {
      document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
    }
    "render the radio form" in new Setup(true, false, SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(true, false, SelfEmployment) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url
    }
    "render the continue button" in new Setup(true, false, SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true, SelfEmployment) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(true, true, SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${SelfEmployment.addIncomeSourceStartDateCheckMessagesPrefix}.error")
    }
    "render the back url" in new Setup(true, true, SelfEmployment) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showSoleTraderBusinessAgent.url
    }
  }
}
