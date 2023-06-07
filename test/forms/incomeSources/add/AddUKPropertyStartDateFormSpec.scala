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

import forms.models.DateFormElement
import implicits.ImplicitDateFormatterImpl
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.{Form, FormError}
import testUtils.TestSupport

import java.time.temporal.ChronoUnit.DAYS
import java.time.LocalDate

class AddUKPropertyStartDateFormSpec extends AnyWordSpecLike with TestSupport {

  val day = "20"
  val month = "10"
  val year = "2022"
  lazy val form: Form[DateFormElement] = AddUKPropertyStartDateForm.apply()(mockImplicitDateFormatter, dateService, messages)

  private def assertFormErrors(formData: Map[String, String], expectedErrors: Seq[FormError]): Unit = {
    val completedForm = form.bind(formData)
    completedForm.errors should contain theSameElementsAs expectedErrors
  }

  private def assertFormData(formData: Map[String, String], expectedData: DateFormElement): Unit = {
    val completedForm = form.bind(formData)
    completedForm("add-uk-property-start-date.day").value shouldEqual Some(expectedData.date.getDayOfMonth.toString)
    completedForm("add-uk-property-start-date.month").value shouldEqual Some(expectedData.date.getMonthValue.toString)
    completedForm("add-uk-property-start-date.year").value shouldEqual Some(expectedData.date.getYear.toString)
    completedForm.value shouldEqual Some(expectedData)
  }

  "AddUKPropertyStartDate form" should {
    "bind with a valid date" in {
      val formData = Map(
        "add-uk-property-start-date.day" -> day,
        "add-uk-property-start-date.month" -> month,
        "add-uk-property-start-date.year" -> year
      )
      val date = LocalDate.of(year.toInt, month.toInt, day.toInt)
      val expectedData = DateFormElement(date)

      assertFormData(formData, expectedData)
    }

    "bind with an incomplete date field (missing day)" in {
      val formData = Map(
        "add-uk-property-start-date.month" -> month,
        "add-uk-property-start-date.year" -> year
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("dateForm.error.day.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with an incomplete date field (missing month)" in {
      val formData = Map(
        "add-uk-property-start-date.day" -> day,
        "add-uk-property-start-date.year" -> year
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("dateForm.error.month.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with an incomplete date field (missing year)" in {
      val formData = Map(
        "add-uk-property-start-date.day" -> day,
        "add-uk-property-start-date.month" -> month
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("dateForm.error.year.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with an incomplete date field (missing day and month)" in {
      val formData = Map(
        "add-uk-property-start-date.year" -> year
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("dateForm.error.dayAndMonth.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with an incomplete date field (missing day and year)" in {
      val formData = Map(
        "add-uk-property-start-date.month" -> month
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("dateForm.error.dayAndYear.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with an incomplete date field (missing month and year)" in {
      val formData = Map(
        "add-uk-property-start-date.day" -> day
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("dateForm.error.monthAndYear.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with an incomplete date field (missing day, month, and year)" in {
      val formData = Map(
        "add-uk-property-start-date.day" -> "",
        "add-uk-property-start-date.month" -> "",
        "add-uk-property-start-date.year" -> ""
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("incomeSources.add.UKPropertyStartDate.error.required"), List()))

      assertFormErrors(formData, expectedErrors)
    }

    "bind with a future date (less than 7 days in the future)" in {
      val currentDate = LocalDate.now()
      val currentDayPlusSixDays = currentDate.plusDays(6).getDayOfMonth
      val currentMonth = currentDate.getMonthValue
      val currentYear = currentDate.getYear

      val formData = Map(
        "add-uk-property-start-date.day" -> currentDayPlusSixDays.toString,
        "add-uk-property-start-date.month" -> currentMonth.toString,
        "add-uk-property-start-date.year" -> currentYear.toString
      )

      assertFormData(formData, DateFormElement(LocalDate.of(currentYear, currentMonth, currentDayPlusSixDays)))
    }

    "bind with an invalid date" in {
      val formData = Map(
        "add-uk-property-start-date.day" -> "i",
        "add-uk-property-start-date.month" -> "am",
        "add-uk-property-start-date.year" -> "NaN"
      )
      val expectedErrors = Seq(FormError("add-uk-property-start-date",
        List("incomeSources.add.UKPropertyStartDate.error.invalid"), List()))

      assertFormErrors(formData, expectedErrors)
    }
  }
}
