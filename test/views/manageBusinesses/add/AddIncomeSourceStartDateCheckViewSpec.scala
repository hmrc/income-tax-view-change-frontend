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
import forms.incomeSources.add.AddIncomeSourceStartDateCheckForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.AddIncomeSourceStartDateCheck

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
            postAction = Call("", ""),
            isAgent = isAgent,
            incomeSourceStartDate = formattedStartDate,
            backUrl = getBackUrl(isAgent, isChange, incomeSourceType)
          )
        )
      )
    }
  }

  def executeTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: AddIncomeSourceStartDateCheckView - $incomeSourceType" should {
      "render the heading" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementsByClass("govuk-fieldset__legend--l").text() shouldBe messages("radioForm.checkDate.heading")
      }
      "render the date entered in Add Income Source Start Date page" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementById("start-date-check-hint").text shouldBe formattedStartDate
      }
      "render the radio form" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("radioForm.yes")
        document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("radioForm.no")
        document.getElementsByClass("govuk-radios").size() shouldBe 1
      }
      "render the back link with the correct URL for Normal journey" in new Setup(isAgent, hasError = false, incomeSourceType, isChange = false) {
        document.getElementById("back").text() shouldBe messages("base.back")
        document.getElementById("back").attr("href") shouldBe getBackUrl(isAgent, isChange = false, incomeSourceType)
      }
      "render the back link with the correct URL for Change journey" in new Setup(isAgent, hasError = false, incomeSourceType, isChange = true) {
        document.getElementById("back").text() shouldBe messages("base.back")
        document.getElementById("back").attr("href") shouldBe getBackUrl(isAgent, isChange = true, incomeSourceType)
      }
      "render the continue button" in new Setup(isAgent, hasError = false, incomeSourceType) {
        document.getElementById("continue-button").text() shouldBe messages("base.continue")
      }
      "render the input error" in new Setup(isAgent, hasError = true, incomeSourceType) {
        document.getElementById("start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
          messages(s"${incomeSourceType.addStartDateCheckMessagesPrefix}.error")
      }
      "render the error summary" in new Setup(isAgent, hasError = true, incomeSourceType) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe
          messages(s"${incomeSourceType.addStartDateCheckMessagesPrefix}.error")
      }
    }
  }

  def getBackUrl(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): String = {
    controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController
      .show(isAgent, isChange, incomeSourceType).url
  }

  for {
    isAgent <- Seq(false, true)
    incomeSourceType <- Seq(UkProperty, ForeignProperty, SelfEmployment)
  } yield {
    executeTest(isAgent, incomeSourceType)
  }
}
