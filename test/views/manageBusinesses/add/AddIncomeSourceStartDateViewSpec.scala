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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.AddIncomeSourceStartDateFormProvider
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.test.Helpers.contentAsString
import testUtils.TestSupport
import views.html.manageBusinesses.add.AddIncomeSourceStartDate

import java.time.LocalDate

class AddIncomeSourceStartDateViewSpec extends TestSupport {

  class Setup(isAgent: Boolean, formType: String = "Standard", incomeSourceType: IncomeSourceType, isChange: Boolean = false) {

    val addIncomeSourceStartDate: AddIncomeSourceStartDate = app.injector.instanceOf[AddIncomeSourceStartDate]
    lazy val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider().apply(incomeSourceType.startDateMessagesPrefix)
    lazy val errorFormWithRequiredError: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(incomeSourceType.startDateMessagesPrefix)
      .withError(FormError("income-source-start-date", s"${incomeSourceType.startDateMessagesPrefix}.required", "day"))
    lazy val errorFormWithInvalidDateError: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(incomeSourceType.startDateMessagesPrefix)
      .withError(FormError("income-source-start-date", s"${incomeSourceType.startDateMessagesPrefix}.error.invalid"))

    val document: Document = {
      Jsoup.parse(
        contentAsString(
          addIncomeSourceStartDate(
            form = {
              formType match {
                case "Standard" => form
                case "StartDateRequired" => errorFormWithRequiredError
                case "InvalidDate" => errorFormWithInvalidDateError
              }
            },
            postAction = Call("", ""),
            isAgent = isAgent,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix,
            backUrl = {
              if(isChange) getBackUrlChange(isAgent, incomeSourceType)
              else getBackUrl(isAgent, incomeSourceType)
            },
            incomeSourceType = incomeSourceType
          )
        )
      )
    }
  }

  def executeTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: AddIncomeSourceStartDateView - $incomeSourceType" should {
      "render the heading" in new Setup(isAgent, "Standard", incomeSourceType) {
        val heading = "Business start date"

        document.getElementsByClass("govuk-heading-l").first().text() shouldBe heading
      }

      "render the description for the business start date" in new Setup(isAgent, "Standard", incomeSourceType) {
        val descriptionText = incomeSourceType match {
          case SelfEmployment => "This is the date we’ll use to calculate your Class 2 National Insurance charge, if appropriate."
          case UkProperty => "This is the date you first received rental income from this UK property business, such as letting or renting out a property or land."
          case ForeignProperty => "This is the date you first received rental income from this foreign property business, such as letting or renting out a property or land."
        }

        document.getElementById("business-start-date-description-1").text() shouldBe "The date your business started trading can be today, in the past or up to 7 days in the future."
        document.getElementById("business-start-date-description-2").text() shouldBe descriptionText
      }
      "render the hint" in new Setup(isAgent, "Standard", incomeSourceType) {
        document.getElementById("value-hint").text() shouldBe "For example, 27 3 2020"
      }
      "render the date form" in new Setup(isAgent, "Standard", incomeSourceType) {
        document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
        document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
        document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
        document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
      }
      "render the back link which with correct redirect for normal journey" in new Setup(isAgent, "Standard", incomeSourceType) {
        document.getElementById("back-fallback").text() shouldBe "Back"
        document.getElementById("back-fallback").attr("href") shouldBe getBackUrl(isAgent, incomeSourceType)
      }
      "render the back link which with correct redirect for change journey" in new Setup(
        isAgent, "Standard", incomeSourceType, isChange = true) {
        document.getElementById("back-fallback").text() shouldBe "Back"
        document.getElementById("back-fallback").attr("href") shouldBe getBackUrlChange(isAgent, incomeSourceType)
      }
      "render the continue button" in new Setup(isAgent, "Standard", incomeSourceType) {
        document.getElementById("continue-button").text() shouldBe "Continue"
      }
      "render the error summary with required error" in new Setup(isAgent, "StartDateRequired", incomeSourceType) {
        document.getElementsByClass("govuk-list govuk-error-summary__list").text().contains("The date must include a") shouldBe true
      }
      "render the error summary with invalid error" in new Setup(isAgent, "InvalidDate", incomeSourceType) {
        document.getElementById("error-summary").text() shouldBe "There is a problem" + " " +
          "The date must be a real date"
      }
    }
  }

  def getBackUrl(isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, UkProperty) => controllers.manageBusinesses.add.routes.AddIncomeSourceController.show()
      case (true, UkProperty) => controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent()
      case (false, ForeignProperty) => controllers.manageBusinesses.add.routes.AddIncomeSourceController.show()
      case (true, ForeignProperty) => controllers.manageBusinesses.add.routes.AddIncomeSourceController.showAgent()
      case (false, SelfEmployment) => controllers.manageBusinesses.add.routes.AddBusinessNameController.show(mode = NormalMode)
      case (true, SelfEmployment) => controllers.manageBusinesses.add.routes.AddBusinessNameController.showAgent(mode = NormalMode)
    }).url
  }

  def getBackUrlChange(isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, UkProperty) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(UkProperty)
      case (true, UkProperty) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
      case (false, ForeignProperty) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
      case (true, ForeignProperty) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
      case (false, SelfEmployment) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (true, SelfEmployment) => controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  def getCaption(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => messages("incomeSources.add.sole-trader")
      case UkProperty => messages("incomeSources.add.uk-property")
      case ForeignProperty => messages("incomeSources.add.foreign-property")
    }
  }

  for {
    isAgent <- Seq(false, true)
    incomeSourceType <- Seq(UkProperty, ForeignProperty, SelfEmployment)
  } yield {
    executeTest(isAgent, incomeSourceType)
  }
}