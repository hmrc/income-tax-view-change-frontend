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
import testConstants.BaseTestConstants.{testMtditid, testMtditid2, testNino, testSelfEmploymentId, testSelfEmploymentId2}
import testConstants.IncomeSourceDetailsTestConstants.{businessIncome, businessIncome2}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.language.postfixOps

class BusinessEndDateFormSpec extends AnyWordSpec with Matchers with TestSupport {
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
    incomeSources = businessIncome
  )(fakeRequestNoSession)

  val testUser2: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid2,
    nino = testNino,
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None,
    incomeSources = businessIncome2
  )(fakeRequestNoSession)

  "BusinessEndDate form" should {
    "bind with a valid date" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = DateFormElement(LocalDate.of(2022, 12, 20))
      val completedForm = form.fill(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("20")
      completedForm.data.get("business-end-date.month") shouldBe Some("12")
      completedForm.data.get("business-end-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid date field" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = Map("business-end-date.day" -> "yo", "business-end-date.month" -> "yo", "business-end-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("yo")
      completedForm.data.get("business-end-date.month") shouldBe Some("yo")
      completedForm.data.get("business-end-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("business-end-date",List("incomeSources.cease.BusinessEndDate.error.invalid"),List()))
    }
    "bind with a future date" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))

      val futureYear = dateService.getCurrentTaxYearEnd() + 1
      val formData = Map("business-end-date.day" -> "20", "business-end-date.month" -> "12", "business-end-date.year" -> s"$futureYear")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("20")
      completedForm.data.get("business-end-date.month") shouldBe Some("12")
      completedForm.data.get("business-end-date.year") shouldBe Some(s"$futureYear")
      completedForm.errors shouldBe List(FormError("business-end-date",List("incomeSources.cease.BusinessEndDate.error.future"),List()))
    }
    "bind with a date earlier than the business start date" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))

      val formData = Map("business-end-date.day" -> "27", "business-end-date.month" -> "8", "business-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("27")
      completedForm.data.get("business-end-date.month") shouldBe Some("8")
      completedForm.data.get("business-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("business-end-date",List("incomeSources.cease.BusinessEndDate.error.beforeStartDate"),List()))

    }
    "give the correct error when binding with a date both before business start date and the 6th of April 2015" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser2, Some(testSelfEmploymentId2))

      val formData = Map("business-end-date.day" -> "14", "business-end-date.month" -> "10", "business-end-date.year" -> "2012")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("14")
      completedForm.data.get("business-end-date.month") shouldBe Some("10")
      completedForm.data.get("business-end-date.year") shouldBe Some("2012")
      completedForm.errors shouldBe List(
        FormError("business-end-date",List("incomeSources.cease.BusinessEndDate.error.beforeEarliestDate"),List())
      )
    }
    "bind with a date missing day field" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))

      val formData = Map("business-end-date.day" -> "", "business-end-date.month" -> "12", "business-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("")
      completedForm.data.get("business-end-date.month") shouldBe Some("12")
      completedForm.data.get("business-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.day.required"), List()))
    }
    "bind with a date missing month field" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))

      val formData = Map("business-end-date.day" -> "20", "business-end-date.month" -> "", "business-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("20")
      completedForm.data.get("business-end-date.month") shouldBe Some("")
      completedForm.data.get("business-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.month.required"), List()))
    }
    "bind with a date missing year field" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = Map("business-end-date.day" -> "20", "business-end-date.month" -> "12", "business-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("20")
      completedForm.data.get("business-end-date.month") shouldBe Some("12")
      completedForm.data.get("business-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.year.required"), List()))
    }
    "bind with a date missing day and month fields" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = Map("business-end-date.day" -> "", "business-end-date.month" -> "", "business-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("")
      completedForm.data.get("business-end-date.month") shouldBe Some("")
      completedForm.data.get("business-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.dayAndMonth.required"), List()))
    }
    "bind with a date missing day and year fields" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = Map("business-end-date.day" -> "", "business-end-date.month" -> "12", "business-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("")
      completedForm.data.get("business-end-date.month") shouldBe Some("12")
      completedForm.data.get("business-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.dayAndYear.required"), List()))
    }
    "bind with a date missing month and year fields" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = Map("business-end-date.day" -> "20", "business-end-date.month" -> "", "business-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("20")
      completedForm.data.get("business-end-date.month") shouldBe Some("")
      completedForm.data.get("business-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.monthAndYear.required"), List()))
    }
    "bind with a date missing day, month and year fields" in {
      val form: Form[DateFormElement] = new BusinessEndDateForm(mockDateService).apply(testUser, Some(testSelfEmploymentId))
      val formData = Map("business-end-date.day" -> "", "business-end-date.month" -> "", "business-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("business-end-date.day") shouldBe Some("")
      completedForm.data.get("business-end-date.month") shouldBe Some("")
      completedForm.data.get("business-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("business-end-date", List("dateForm.error.dayMonthAndYear.required"), List()))
    }
  }
}

