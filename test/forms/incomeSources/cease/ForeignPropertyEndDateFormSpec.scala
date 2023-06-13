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

package forms.incomeSources.cease

import auth.MtdItUser
import forms.models.DateFormElement
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceDetailsTestConstants.foreignPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.language.postfixOps


class ForeignPropertyEndDateFormSpec extends AnyWordSpec with Matchers with TestSupport {
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
    incomeSources = foreignPropertyIncome
  )(fakeRequestCeaseForeignPropertyDeclarationComplete)

  lazy val form: Form[DateFormElement] = new ForeignPropertyEndDateForm(mockDateService).apply(testUser)

  "ForeignPropertyEndDate form" should {
    "bind with a valid date" in {
      val formData = DateFormElement(LocalDate.of(2022, 12, 20))
      val completedForm = form.fill(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("20")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }

    "bind with an incomplete date field" in {
      val formData = Map("foreign-property-end-date.day" -> "", "foreign-property-end-date.month" -> "", "foreign-property-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date", List("incomeSources.cease.ForeignPropertyEndDate.error.incomplete"), List()))
    }

    "bind with an incomplete date field contains empty day value" in {
      val formData = Map("foreign-property-end-date.day" -> "", "foreign-property-end-date.month" -> "12", "foreign-property-end-date.year" -> "2022")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",List("incomeSources.cease.ForeignPropertyEndDate.error.incompleteDay"),List()))
    }

    "bind with an incomplete date field contains empty month value" in {
      val formData = Map("foreign-property-end-date.day" -> "1", "foreign-property-end-date.month" -> "", "foreign-property-end-date.year" -> "2022")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("1")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",
        List("incomeSources.cease.ForeignPropertyEndDate.error.incompleteMonth"), List()))
    }

    "bind with an incomplete date field contains empty year value" in {
      val formData = Map("foreign-property-end-date.day" -> "1", "foreign-property-end-date.month" -> "12", "foreign-property-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("1")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",
        List("incomeSources.cease.ForeignPropertyEndDate.error.incompleteYear"), List()))
    }

    "bind with an incomplete date field contains empty day & month value" in {
      val formData = Map("foreign-property-end-date.day" -> "", "foreign-property-end-date.month" -> "", "foreign-property-end-date.year" -> "2022")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",
        List("incomeSources.cease.ForeignPropertyEndDate.error.incompleteDayMonth"), List()))
    }

    "bind with an incomplete date field contains empty day & year value" in {
      val formData = Map("foreign-property-end-date.day" -> "", "foreign-property-end-date.month" -> "12", "foreign-property-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",
        List("incomeSources.cease.ForeignPropertyEndDate.error.incompleteDayYear"), List()))
    }

    "bind with an incomplete date field contains empty month & year value" in {
      val formData = Map("foreign-property-end-date.day" -> "1", "foreign-property-end-date.month" -> "", "foreign-property-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("1")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",
        List("incomeSources.cease.ForeignPropertyEndDate.error.incompleteMonthYear"), List()))
    }

    "bind with an invalid date field" in {
      val formData = Map("foreign-property-end-date.day" -> "hi", "foreign-property-end-date.month" -> "im", "foreign-property-end-date.year" -> "fake")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("hi")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("im")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("fake")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",List("incomeSources.cease.ForeignPropertyEndDate.error.invalid"),List()))
    }
    "bind with a future date" in {
      val futureYear = dateService.getCurrentTaxYearEnd() + 1
      val formData = Map("foreign-property-end-date.day" -> "20", "foreign-property-end-date.month" -> "12", "foreign-property-end-date.year" -> s"$futureYear")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("20")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some(s"$futureYear")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",List("incomeSources.cease.ForeignPropertyEndDate.error.future"),List()))
    }
    "bind with a date earlier than the Foreign property start date" in {
      val formData = Map("foreign-property-end-date.day" -> "20", "foreign-property-end-date.month" -> "12", "foreign-property-end-date.year" -> "1900")
      val completedForm = form.bind(formData)

      completedForm.data.get("foreign-property-end-date.day") shouldBe Some("20")
      completedForm.data.get("foreign-property-end-date.month") shouldBe Some("12")
      completedForm.data.get("foreign-property-end-date.year") shouldBe Some("1900")
      completedForm.errors shouldBe List(FormError("foreign-property-end-date",List("incomeSources.cease.ForeignPropertyEndDate.error.beforeStartDate"),List()))
    }
  }
}
