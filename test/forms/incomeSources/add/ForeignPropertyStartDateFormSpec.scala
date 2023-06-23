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

import auth.MtdItUser
import forms.models.DateFormElement
import implicits.ImplicitDateFormatter
import play.api.data.{Form, FormError}
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class ForeignPropertyStartDateFormSpec extends TestSupport with ImplicitDateFormatter {

  val mockDateService: DateService = app.injector.instanceOf[DateService]

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
  )(fakeRequestNoSession)

  lazy val form: Form[DateFormElement] = new ForeignPropertyStartDateForm(mockDateService)(languageUtils).apply(testUser, messages)

  "ForeignPropertyStartDate form" should {
    "bind with a valid date" in {
      val formData = DateFormElement(LocalDate.of(2022, 12, 20))
      val completedForm = form.fill(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("20")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }

    "bind with an incomplete date field" in {
      val formData = Map("foreign-property-start-date.day" -> "", "foreign-property-start-date.month" -> "", "foreign-property-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.empty"), List()))
    }
    "bind with an missing Month Year field" in {
      val formData = Map("foreign-property-start-date.day" -> "2", "foreign-property-start-date.month" -> "", "foreign-property-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.missingMonthYear"), List()))
    }

    "bind with an missing Day Year field" in {
      val formData = Map("foreign-property-start-date.day" -> "", "foreign-property-start-date.month" -> "2", "foreign-property-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.missingDayYear"), List()))
    }

    "bind with an missing Day Month field" in {
      val formData = Map("foreign-property-start-date.day" -> "", "foreign-property-start-date.month" -> "", "foreign-property-start-date.year" -> "2022")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.missingDayMonth"), List()))
    }

    "bind with an missing Year field" in {
      val formData = Map("foreign-property-start-date.day" -> "2", "foreign-property-start-date.month" -> "2", "foreign-property-start-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.missingYear"), List()))
    }

    "bind with an missing Month field" in {
      val formData = Map("foreign-property-start-date.day" -> "2", "foreign-property-start-date.month" -> "", "foreign-property-start-date.year" -> "2022")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.missingMonth"), List()))
    }

    "bind with an missing Day field" in {
      val formData = Map("foreign-property-start-date.day" -> "", "foreign-property-start-date.month" -> "2", "foreign-property-start-date.year" -> "2022")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.missingDay"), List()))
    }

    "bind with an invalid date field" in {
      val formData = Map("foreign-property-start-date.day" -> "hi", "foreign-property-start-date.month" -> "im", "foreign-property-start-date.year" -> "fake")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("hi")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("im")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("fake")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.invalid"), List()))
    }

    "bind with an unreal date field" in {
      val formData = Map("foreign-property-start-date.day" -> "29", "foreign-property-start-date.month" -> "2", "foreign-property-start-date.year" -> "2023")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some("29")
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some("2")
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some("2023")
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.invalid"), List()))
    }

    "bind with a future date" in {
      val now = dateService.getCurrentDate()
      val futureDate = now.plusDays(8)
      val day = futureDate.getDayOfMonth.toString
      val month = futureDate.getMonthValue.toString
      val year = futureDate.getYear.toString
      val allowedTillDate = dateService.getCurrentDate().plusWeeks(1).toLongDate
      val formData = Map("foreign-property-start-date.day" -> day, "foreign-property-start-date.month" -> month, "foreign-property-start-date.year" -> year)
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-start-date.day") shouldBe Some(day)
      completedForm.data.get("foreign-property-start-date.month") shouldBe Some(month)
      completedForm.data.get("foreign-property-start-date.year") shouldBe Some(year)
      completedForm.errors shouldBe List(FormError("foreign-property-start-date", List("incomeSources.add.foreignProperty.startDate.error.future"), List(allowedTillDate)))
    }

  }
}


