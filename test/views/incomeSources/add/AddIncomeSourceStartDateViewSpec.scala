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
import forms.incomeSources.add.AddIncomeSourceStartDateForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.api.mvc.Call
import play.mvc.Http.HeaderNames.{SERVER, UPGRADE}
import play.test.Helpers.contentAsString
import testUtils.TestSupport
import views.html.incomeSources.add.AddIncomeSourceStartDate

class AddIncomeSourceStartDateViewSpec extends TestSupport {

  class Setup(isAgent: Boolean, hasError: Boolean = false, incomeSourceType: IncomeSourceType, isChange: Boolean = false) extends TestSupport {

    val addIncomeSourceStartDate: AddIncomeSourceStartDate = app.injector.instanceOf[AddIncomeSourceStartDate]

    val document: Document = {
      Jsoup.parse(
        contentAsString(
          addIncomeSourceStartDate(
            form = {
              if(hasError) AddIncomeSourceStartDateForm(incomeSourceType.startDateMessagesPrefix)
                .withError(FormError("income-source-start-date", s"${incomeSourceType.startDateMessagesPrefix}.error.required"))
              else AddIncomeSourceStartDateForm(incomeSourceType.startDateMessagesPrefix)
            },
            postAction = Call("", ""),
            isAgent = isAgent,
            messagesPrefix = incomeSourceType.startDateMessagesPrefix,
            backUrl = {
              if(isChange) getBackUrlChange(isAgent, incomeSourceType)
              else getBackUrl(isAgent, incomeSourceType)
            }
          )
        )
      )
    }
  }

  def executeTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: AddIncomeSourceStartDateView - $incomeSourceType" should {
      "render the heading" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe
          messages(s"${incomeSourceType.startDateMessagesPrefix}.heading")
      }
      "render the hint" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementById("income-source-start-date-hint").text() shouldBe
          s"${messages(s"${incomeSourceType.startDateMessagesPrefix}.hint")} ${messages("dateForm.hint")}"
      }
      "render the date form" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
        document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
        document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
        document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
      }
      "render the back link which with correct redirect for normal journey" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementById("back").text() shouldBe messages("base.back")
        document.getElementById("back").attr("href") shouldBe getBackUrl(isAgent, incomeSourceType)
      }
      "render the back link which with correct redirect for change journey" in new Setup(
        isAgent, hasError = false, incomeSourceType, isChange = true) {
        document.getElementById("back").text() shouldBe messages("base.back")
        document.getElementById("back").attr("href") shouldBe getBackUrlChange(isAgent, incomeSourceType)
      }
      "render the continue button" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementById("continue-button").text() shouldBe messages("base.continue")
      }
      "render the error message" in new Setup(isAgent, hasError = true, incomeSourceType) {
        document.getElementById("income-source-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
          messages(s"${incomeSourceType.startDateMessagesPrefix}.error.required")
      }
      "render the error summary" in new Setup(isAgent, hasError = true, incomeSourceType) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe
          messages(s"${incomeSourceType.startDateMessagesPrefix}.error.required")
      }
    }
  }

  def getBackUrl(isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, UkProperty) => controllers.incomeSources.add.routes.AddIncomeSourceController.show()
      case (true, UkProperty) => controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent()
      case (false, ForeignProperty) => controllers.incomeSources.add.routes.AddIncomeSourceController.show()
      case (true, ForeignProperty) => controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent()
      case (false, SelfEmployment) => controllers.incomeSources.add.routes.AddBusinessNameController.show()
      case (true, SelfEmployment) => controllers.incomeSources.add.routes.AddBusinessNameController.showAgent()
    }).url
  }

  def getBackUrlChange(isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, UkProperty) => controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(UkProperty)
      case (true, UkProperty) => controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
      case (false, ForeignProperty) => controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
      case (true, ForeignProperty) => controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
      case (false, SelfEmployment) => controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (true, SelfEmployment) => controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  for {
    isAgent <- Seq(false, true)
    incomeSourceType <- Seq(UkProperty, ForeignProperty, SelfEmployment)
  } yield {
    executeTest(isAgent, incomeSourceType)
  }
}