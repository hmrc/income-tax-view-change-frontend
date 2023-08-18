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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
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

  class Setup(isAgent: Boolean, hasError: Boolean = false, incomeSourceType: IncomeSourceType, isUpdate: Boolean = false) extends TestSupport {

    lazy val document: Document = (isAgent, hasError, isUpdate, incomeSourceType) match {
      case (true, false, true, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(ForeignProperty.addStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent, isUpdate),
            isAgent = isAgent,
            backUrl = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.showAgent().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (false, false, true, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(ForeignProperty.addStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent, isUpdate),
            isAgent = isAgent,
            backUrl = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (true, false, _, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(incomeSourceType.startDateMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent = true, isChange = false),
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (false, false, _, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(incomeSourceType.startDateMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent = false, isChange = false),
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (_, true, _, ForeignProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(incomeSourceType.startDateMessagesPrefix).withError(FormError("income-source-start-date", s"${incomeSourceType.startDateMessagesPrefix}.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent = true, isChange = false),
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (true, false, true, UkProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(UkProperty.addStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(UkProperty.key, isAgent, isUpdate),
            isAgent = isAgent,
            backUrl = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (false, false, true, UkProperty) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(UkProperty.addStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent, isUpdate),
            isAgent = isAgent,
            backUrl = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (false, false, _, UkProperty) =>
        val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent = false, isChange = false),
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (true, false, _, UkProperty) =>
        val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent = true, isChange = false),
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (_, true, _, UkProperty) =>
        val messagesPrefix = "incomeSources.add.UKPropertyStartDate"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(UkProperty.key, isAgent = false, isChange = false),
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (true, false, true, SelfEmployment) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(SelfEmployment.addStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(SelfEmployment.key, isAgent, isUpdate),
            isAgent = isAgent,
            backUrl = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (false, false, true, SelfEmployment) =>
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(SelfEmployment.addStartDateCheckMessagesPrefix),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(SelfEmployment.key, isAgent, isUpdate),
            isAgent = isAgent,
            backUrl = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix
          )
        ))
      case (false, false, _, SelfEmployment) =>
        val messagesPrefix = "add-business-start-date"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(SelfEmployment.key, isAgent = false, isChange = false),
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (true, false, _, SelfEmployment) =>
        val messagesPrefix = "add-business-start-date"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(SelfEmployment.key, isAgent = true, isChange = false),
            isAgent = true,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url,
            messagesPrefix = messagesPrefix
          )
        ))
      case (_, true, _, SelfEmployment) =>
        val messagesPrefix = "add-business-start-date"
        Jsoup.parse(contentAsString(
          addIncomeSourceStartDate(
            form = AddIncomeSourceStartDateForm(messagesPrefix).withError(FormError("income-source-start-date", s"$messagesPrefix.error.required")),
            postAction = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(SelfEmployment.key, isAgent = false, isChange = false),
            isAgent = false,
            backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
            messagesPrefix = messagesPrefix
          )
        ))
    }
  }

  "AddIncomeSourceStartDateView - Foreign Property - Individual" should {
    "render the heading" in new Setup(isAgent = false, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.heading")
    }
    "render the hint" in new Setup(isAgent = false, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.foreignProperty.startDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = false, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link which redirects to Add Income Source page" in new Setup(isAgent = false, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the back link which redirects to Check Income Source Details page" in new Setup(isAgent = false, hasError = false, incomeSourceType = ForeignProperty, isUpdate = true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = false, hasError = true, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = false, hasError = true, incomeSourceType = ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - UK Property - Individual" should {
    "render the heading" in new Setup(isAgent = false, hasError = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.heading")
    }
    "render the hint" in new Setup(isAgent = false, hasError = false, incomeSourceType = UkProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.UKPropertyStartDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = false, hasError = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link which redirects to Add Income Source page" in new Setup(isAgent = false, hasError = false, incomeSourceType = UkProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the back link which redirects to Check Income Source Details page" in new Setup(isAgent = false, hasError = false, incomeSourceType = UkProperty, isUpdate = true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, hasError = false, incomeSourceType = UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = false, hasError = true, incomeSourceType = UkProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = false, hasError = true, incomeSourceType = UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - Sole Trader Business - Individual" should {
    "render the heading" in new Setup(isAgent = false, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("add-business-start-date.heading")
    }
    "render the hint" in new Setup(isAgent = false, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("add-business-start-date.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = false, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link which redirects to Add Income Source page" in new Setup(isAgent = false, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the back link which redirects to Check Income Source Details page" in new Setup(isAgent = false, hasError = false, incomeSourceType = SelfEmployment, isUpdate = true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
    }
    "render the continue button" in new Setup(isAgent = false, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = false, hasError = true, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("add-business-start-date.error.required")
    }
    "render the error summary" in new Setup(isAgent = false, hasError = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-start-date.error.required")
    }
  }


  "AddIncomeSourceStartDateView - Foreign Property - Agent" should {
    "render the heading" in new Setup(isAgent = true, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.heading")
    }
    "render the hint" in new Setup(isAgent = true, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.foreignProperty.startDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = true, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link which redirects to Add Income Source page" in new Setup(isAgent = true, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the back link which redirects to Check Income Source Details page" in new Setup(isAgent = true, hasError = false, incomeSourceType = ForeignProperty, isUpdate = true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.showAgent().url
    }
    "render the continue button" in new Setup(isAgent = true, hasError = false, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = true, hasError = true, incomeSourceType = ForeignProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = true, hasError = true, incomeSourceType = ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - UK Property - Agent" should {
    "render the heading" in new Setup(isAgent = true, hasError = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.heading")
    }
    "render the hint" in new Setup(isAgent = true, hasError = false, incomeSourceType = UkProperty) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("incomeSources.add.UKPropertyStartDate.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = true, hasError = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link which redirects to Add Income Source page" in new Setup(isAgent = true, hasError = false, incomeSourceType = UkProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the back link which redirects to Check Income Source Details page" in new Setup(isAgent = true, hasError = false, incomeSourceType = UkProperty, isUpdate = true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url
    }
    "render the continue button" in new Setup(isAgent = true, hasError = false, incomeSourceType = UkProperty) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = true, hasError = true, incomeSourceType = UkProperty) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
    "render the error summary" in new Setup(isAgent = true, hasError = true, incomeSourceType = UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.UKPropertyStartDate.error.required")
    }
  }

  "AddIncomeSourceStartDateView - Sole Trader Business - Agent" should {
    "render the heading" in new Setup(isAgent = true, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("add-business-start-date.heading")
    }
    "render the hint" in new Setup(isAgent = true, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-start-date-hint").text() shouldBe s"${messages("add-business-start-date.hint")} ${messages("dateForm.hint")}"
    }
    "render the date form" in new Setup(isAgent = true, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link which redirects to Add Income Source page" in new Setup(isAgent = true, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the back link which redirects to Check Income Source Details page" in new Setup(isAgent = true, hasError = false, incomeSourceType = SelfEmployment, isUpdate = true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
    }
    "render the continue button" in new Setup(isAgent = true, hasError = false, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(isAgent = true, hasError = true, incomeSourceType = SelfEmployment) {
      document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("add-business-start-date.error.required")
    }
    "render the error summary" in new Setup(isAgent = true, hasError = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("add-business-start-date.error.required")
    }
  }
}