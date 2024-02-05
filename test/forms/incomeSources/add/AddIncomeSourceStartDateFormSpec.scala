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

package forms.incomeSources.add

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.models.DateFormElement
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}
import testUtils.TestSupport

import java.time.LocalDate


class AddIncomeSourceStartDateFormSpec extends AnyWordSpec with Matchers with TestSupport {

  "AddIncomeSourceStartDate form" should {
    "bind with a valid date" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = DateFormElement(LocalDate.of(2022, 12, 20))
      val completedForm = form.fill(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid date field - Self employment" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "yo", "income-source-start-date.month" -> "yo", "income-source-start-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List(s"${SelfEmployment.startDateMessagesPrefix}.error.invalid"), List()))
    }
    "bind with an invalid date field - Foreign property" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(ForeignProperty.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "yo", "income-source-start-date.month" -> "yo", "income-source-start-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List(s"${ForeignProperty.startDateMessagesPrefix}.error.invalid"), List()))
    }
    "bind with an invalid date field - UK Property" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(UkProperty.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "yo", "income-source-start-date.month" -> "yo", "income-source-start-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List(s"${UkProperty.startDateMessagesPrefix}.error.invalid"), List()))
    }
    "bind with a valid future date" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val futureDate = dateService.getCurrentDate().plusDays(6)
      val formData = Map("income-source-start-date.day" -> s"${futureDate.getDayOfMonth}", "income-source-start-date.month" -> s"${futureDate.getMonthValue}", "income-source-start-date.year" -> s"${futureDate.getYear}")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some(s"${futureDate.getDayOfMonth}")
      completedForm.data.get("income-source-start-date.month") shouldBe Some(s"${futureDate.getMonthValue}")
      completedForm.data.get("income-source-start-date.year") shouldBe Some(s"${futureDate.getYear}")
      completedForm.errors shouldBe Nil
    }
    "bind with a invalid future date, date greater then current date plus 6 days " in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val futureDate = dateService.getCurrentDate().plusDays(7)
      val formData = Map("income-source-start-date.day" -> s"${futureDate.getDayOfMonth}", "income-source-start-date.month" -> s"${futureDate.getMonthValue}", "income-source-start-date.year" -> s"${futureDate.getYear}")
      val completedForm = form.bind(formData)
      completedForm.data.get("income-source-start-date.day") shouldBe Some(s"${futureDate.getDayOfMonth}")
      completedForm.data.get("income-source-start-date.month") shouldBe Some(s"${futureDate.getMonthValue}")
      completedForm.data.get("income-source-start-date.year") shouldBe Some(s"${futureDate.getYear}")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List(s"The date your business started trading must be before ${mockImplicitDateFormatter.longDate(futureDate).toLongDate}"), List()))
    }

    "bind with a date missing day field" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "", "income-source-start-date.month" -> "12", "income-source-start-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List("dateForm.error.day.required"), List()))
    }
    "bind with a date missing month field" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "20", "income-source-start-date.month" -> "", "income-source-start-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List("dateForm.error.month.required"), List()))
    }
    "bind with a date missing year field" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "20", "income-source-start-date.month" -> "12", "income-source-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List("dateForm.error.year.required"), List()))
    }
    "bind with a date missing day and month fields" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "", "income-source-start-date.month" -> "", "income-source-start-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List("dateForm.error.dayAndMonth.required"), List()))
    }
    "bind with a date missing day and year fields" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "", "income-source-start-date.month" -> "12", "income-source-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List("dateForm.error.dayAndYear.required"), List()))
    }
    "bind with a date missing month and year fields" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "20", "income-source-start-date.month" -> "", "income-source-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List("dateForm.error.monthAndYear.required"), List()))
    }
    "bind with a date missing day, month and year fields" in {
      val form: Form[DateFormElement] = AddIncomeSourceStartDateForm(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("income-source-start-date.day" -> "", "income-source-start-date.month" -> "", "income-source-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-start-date.day") shouldBe Some("")
      completedForm.data.get("income-source-start-date.month") shouldBe Some("")
      completedForm.data.get("income-source-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-start-date", List(s"${SelfEmployment.startDateMessagesPrefix}.error.required"), List()))
    }
  }
}

