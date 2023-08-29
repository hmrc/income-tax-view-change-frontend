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

  class Setup(isAgent: Boolean, hasError: Boolean, incomeSourceType: IncomeSourceType, isChange: Boolean = false) {

    val addIncomeSourceStartDateCheck: AddIncomeSourceStartDateCheck = app.injector.instanceOf[AddIncomeSourceStartDateCheck]
    val startDate: String = "2022-06-30"
    val formattedStartDate: String = mockImplicitDateFormatter.longDate(LocalDate.parse(startDate)).toLongDate

    lazy val document: Document = {
      Jsoup.parse(
        contentAsString(
          addIncomeSourceStartDateCheck(
            form = {
              if (hasError) AddIncomeSourceStartDateCheckForm(incomeSourceType.addStartDateCheckMessagesPrefix)
                .withError(FormError("start-date-check", s"${incomeSourceType.addStartDateCheckMessagesPrefix}.error"))
              else AddIncomeSourceStartDateCheckForm(incomeSourceType.addStartDateCheckMessagesPrefix)
            },
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType, isAgent, isChange),
            isAgent = isAgent,
            incomeSourceStartDate = formattedStartDate,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType, isAgent, isChange).url
          )
        )
      )
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
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(UkProperty, isAgent = false, isChange = false).url
    }
    "render the continue button" in new Setup(false, false, UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true, UkProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${UkProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(false, true, UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${UkProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the back url which redirects to Add Income Source Start Date Page" in new Setup(false, false, UkProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(UkProperty, isAgent = false, isChange = true).url
    }
    "render the back url which redirects to Add Income Source Start Date Change page" in new Setup(false, false, UkProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(UkProperty, isAgent = false, isChange = true).url
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
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(UkProperty, isAgent = true, isChange = false).url
    }
    "render the continue button" in new Setup(true, false, UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true, UkProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${UkProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(true, true, UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${UkProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the back url which redirects to Add Income Source Start Date Page" in new Setup(true, false, UkProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(UkProperty, isAgent = true, isChange = true).url
    }
    "render the back url which redirects to Add Income Source Start Date Change page" in new Setup(true, false, UkProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(UkProperty, isAgent = true, isChange = true).url
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
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(ForeignProperty, isAgent = false, isChange = false).url
    }
    "render the continue button" in new Setup(false, false, ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true, ForeignProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${ForeignProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(false, true, ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${ForeignProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the back url which redirects to Add Income Source Start Date Page" in new Setup(false, false, ForeignProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(ForeignProperty, isAgent = false, isChange = true).url
    }
    "render the back url which redirects to Add Income Source Start Date Check page" in new Setup(false, false, ForeignProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(ForeignProperty, isAgent = false, isChange = true).url
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
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(ForeignProperty, isAgent = true, isChange = false).url
    }
    "render the continue button" in new Setup(true, false, ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true, ForeignProperty) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${ForeignProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(true, true, ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${ForeignProperty.addStartDateCheckMessagesPrefix}.error")
    }
    "render the back url which redirects to Add Income Source Start Date Page" in new Setup(true, false, ForeignProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(ForeignProperty, isAgent = true, isChange = true).url
    }
    "render the back url which redirects to Add Income Source Start Date Change Page" in new Setup(true, false, ForeignProperty, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(ForeignProperty, isAgent = true, isChange = true).url
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
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(SelfEmployment, isAgent = false, isChange = false).url
    }
    "render the continue button" in new Setup(false, false, SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true, SelfEmployment) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${SelfEmployment.addStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(false, true, SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${SelfEmployment.addStartDateCheckMessagesPrefix}.error")
    }
    "render the back url which redirects to Add Income Source Start Date Page" in new Setup(false, false, SelfEmployment, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(SelfEmployment, isAgent = false, isChange = true).url
    }
    "render the back url which redirects to Add Income Source Start Date Change Page" in new Setup(false, false, SelfEmployment, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(SelfEmployment, isAgent = false, isChange = true).url
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
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(SelfEmployment, isAgent = true, isChange = false).url
    }
    "render the continue button" in new Setup(true, false, SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true, SelfEmployment) {
      document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"${SelfEmployment.addStartDateCheckMessagesPrefix}.error")
    }
    "render the error summary" in new Setup(true, true, SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"${SelfEmployment.addStartDateCheckMessagesPrefix}.error")
    }
    "render the back url which redirects to Add Income Source Start Date Page" in new Setup(true, false, SelfEmployment, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(SelfEmployment, isAgent = true, isChange = true).url
    }
    "render the back url which redirects to Add Income Source Start Date Change Page" in new Setup(true, false, SelfEmployment, true) {
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(SelfEmployment, isAgent = true, isChange = true).url
    }
  }
}
