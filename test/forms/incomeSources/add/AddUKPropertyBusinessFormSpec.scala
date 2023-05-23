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
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.data.{Form, FormError}
import testUtils.TestSupport
import java.time.temporal.ChronoUnit.DAYS

import java.time.LocalDate

class AddUKPropertyBusinessFormSpec extends AnyWordSpecLike with TestSupport {

  lazy val form: Form[DateFormElement] = new AddUKPropertyBusinessStartDateForm(dateService, mockImplicitDateFormatter).apply(individualUser, messages)

  def mockFormEntry(day: String, month: String, year: String): Unit = {
    val formData = DateFormElement(LocalDate.of(year.toInt, month.toInt, day.toInt))
    val completedForm = form.fill(formData)
    completedForm.data.get("add-uk-property-business-start-date.day") shouldBe Some(day)
    completedForm.data.get("add-uk-property-business-start-date.month") shouldBe Some(month)
    completedForm.data.get("add-uk-property-business-start-date.year") shouldBe Some(year)
  }

  def mockFormErrors(day: String, month: String, year: String): Unit = {
    (day, month, year) match {
      case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.isEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("incomeSources.add.UKPropertyBusinessStartDate.error.required"), List()))
      case (day, month, year) if day.trim.isEmpty && month.trim.nonEmpty && year.trim.isEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("dateForm.error.dayAndYear.required"), List()))
      case (day, month, year) if day.trim.isEmpty && month.trim.isEmpty && year.trim.nonEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("dateForm.error.dayAndMonth.required"), List()))
      case (day, month, year) if day.trim.nonEmpty && month.trim.isEmpty && year.trim.isEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("dateForm.error.monthAndYear.required"), List()))
      case (day, month, year) if day.trim.isEmpty && month.trim.nonEmpty && year.trim.nonEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("dateForm.error.day.required"), List()))
      case (day, month, year) if day.trim.nonEmpty && month.trim.isEmpty && year.trim.nonEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("dateForm.error.month.required"), List()))
      case (day, month, year) if day.trim.nonEmpty && month.trim.nonEmpty && year.trim.isEmpty =>
        form.errors shouldBe List(FormError("add-uk-property-business-start-date", List("dateForm.error.year.required"), List()))
      case _ =>
        form.errors shouldBe List.empty
    }
  }

  "AddUKPropertyBusinessStartDate form" should {
    "bind with a valid date" in {
      mockFormEntry("1", "10", "2022")
      mockFormErrors("1", "10", "2022")
    }
    "bind with an incomplete date field (missing day)" in {
      mockFormEntry("", "10", "2022")
      mockFormErrors("", "10", "2022")
    }
    "bind with an incomplete date field (missing month)" in {
      mockFormEntry("1", "", "2022")
      mockFormErrors("1", "", "2022")
    }
    "bind with an incomplete date field (missing year)" in {
      mockFormEntry("1", "10", "")
      mockFormErrors("1", "10", "")
    }
    "bind with an incomplete date field (missing day and month)" in {
      mockFormEntry("", "", "2022")
      mockFormErrors("", "", "2022")
    }
    "bind with an incomplete date field (missing day and year)" in {
      mockFormEntry("", "10", "")
      mockFormErrors("", "10", "")
    }
    "bind with an incomplete date field (missing month and year)" in {
      mockFormEntry("1", "", "")
      mockFormErrors("1", "", "")
    }
    "bind with an incomplete date field (missing day, month and year)" in {
      mockFormEntry("", "", "")
      mockFormErrors("", "", "")
    }
    "bind with a future date (more than 7 days in future)" in {
      val currentDate = dateService.getCurrentDate()
      val maximumDate = dateService.getCurrentDate().plusWeeks(1)
      val maximumDateFormatted = mockImplicitDateFormatter.longDate(maximumDate).toLongDate
      val aboveMaximumDate = currentDate.plusWeeks(3)
      val day = aboveMaximumDate.getDayOfMonth.toString
      val month = aboveMaximumDate.getMonthValue.toString
      val year = aboveMaximumDate.getYear.toString

      println((day, month, year))
      val formData = DateFormElement(LocalDate.of(year.toInt, month.toInt, day.toInt))
      val completedForm = form.fill(formData)
//      mockFormEntry(day, month, year)

      println(completedForm.data)
      println(completedForm.value)
      println(completedForm.errors)
      form.errors shouldBe List(FormError("add-uk-property-business-start-date", List(messages("incomeSources.add.UKPropertyBusinessStartDate.error.future", maximumDateFormatted)), List()))
    }
  }
}
