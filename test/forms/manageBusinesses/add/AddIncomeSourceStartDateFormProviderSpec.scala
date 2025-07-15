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

package forms.manageBusinesses.add

import enums.IncomeSourceJourney.SelfEmployment
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}
import testUtils.TestSupport

import java.time.LocalDate


class AddIncomeSourceStartDateFormProviderSpec extends AnyWordSpec with Matchers with TestSupport {

  val dateFormErrorPrefix = "dateForm.error"

  "AddIncomeSourceStartDate form" should {
    "bind with a valid date" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = LocalDate.of(2022, 12, 20)
      val completedForm = form.fill(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid date field - Self employment" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List(s"$dateFormErrorPrefix.invalid"), List()))
    }
    "bind with an invalid date field - Foreign property" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List(s"$dateFormErrorPrefix.invalid"), List()))
    }
    "bind with an invalid date field - UK Property" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List(s"$dateFormErrorPrefix.invalid"), List()))
    }
    "bind with a valid future date" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val futureDate = dateService.getCurrentDate.plusDays(6)
      val formData = Map("value.day" -> s"${futureDate.getDayOfMonth}", "value.month" -> s"${futureDate.getMonthValue}", "value.year" -> s"${futureDate.getYear}")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some(s"${futureDate.getDayOfMonth}")
      completedForm.data.get("value.month") shouldBe Some(s"${futureDate.getMonthValue}")
      completedForm.data.get("value.year") shouldBe Some(s"${futureDate.getYear}")
      completedForm.errors shouldBe Nil
    }
    "bind with a invalid future date, date greater then current date plus 6 days " in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val futureDate = dateService.getCurrentDate.plusDays(7)
      val formData = Map("value.day" -> s"${futureDate.getDayOfMonth}", "value.month" -> s"${futureDate.getMonthValue}", "value.year" -> s"${futureDate.getYear}")
      val completedForm = form.bind(formData)
      completedForm.data.get("value.day") shouldBe Some(s"${futureDate.getDayOfMonth}")
      completedForm.data.get("value.month") shouldBe Some(s"${futureDate.getMonthValue}")
      completedForm.data.get("value.year") shouldBe Some(s"${futureDate.getYear}")
      completedForm.errors shouldBe List(FormError("value", List(s"The date your business started trading must be before ${mockImplicitDateFormatter.longDate(futureDate).toLongDate}"), List()))
    }

    "bind with a date missing day field" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "", "value.month" -> "12", "value.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("value", List(s"$dateFormErrorPrefix.required"), List("day")))
    }
    "bind with a date missing month field" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "20", "value.month" -> "", "value.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("value", List(s"$dateFormErrorPrefix.required"), List("month")))
    }
    "bind with a date missing year field" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "20", "value.month" -> "12", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List(s"$dateFormErrorPrefix.required"), List("year")))
    }
    "bind with a date missing day and month fields" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "", "value.month" -> "", "value.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required.two"), List("day", "month")))
    }
    "bind with a date missing day and year fields" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "", "value.month" -> "12", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required.two"), List("day", "year")))
    }
    "bind with a date missing month and year fields" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "20", "value.month" -> "", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required.two"), List("month", "year")))
    }
    "bind with a date missing day, month and year fields" in {
      val form: Form[LocalDate] = new AddIncomeSourceStartDateFormProvider()(SelfEmployment.startDateMessagesPrefix)
      val formData = Map("value.day" -> "", "value.month" -> "", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List(s"${SelfEmployment.startDateMessagesPrefix}.required.all"), List()))
    }
  }
}

