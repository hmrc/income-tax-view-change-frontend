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
import authV2.AuthActionsTestData.getMinimalMTDITUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.admin.IncomeSourcesNewJourney
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Form, FormError}
import services.DateService
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class CeaseIncomeSourceEndDateFormProviderSpec extends AnyWordSpec with Matchers with TestSupport {
  val mockDateService: DateService = app.injector.instanceOf[DateService]
  val ceaseEndDateForm: CeaseIncomeSourceEndDateFormProvider = app.injector.instanceOf[CeaseIncomeSourceEndDateFormProvider]

  val testUser: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), ukPlusForeignPropertyWithSoleTraderIncomeSource, false, fakeRequestNoSession)

  val testUser2: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), businessIncome2, false, fakeRequestNoSession)

  val testUser3: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), ukForeignSoleTraderIncomeSourceBeforeEarliestStartDate, false, fakeRequestNoSession)

  val testUser4: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), ukForeignSoleTraderIncomeSourceBeforeContextualTaxYear, false, fakeRequestNoSession)

  def setupTestId(incomeSourceType: IncomeSourceType): Option[String] = {
    if (incomeSourceType == SelfEmployment) {
      Some(testSelfEmploymentId)
    } else {
      None
    }
  }

  def setupTestUser(incomeSourceType: IncomeSourceType): MtdItUser[_] = {
    if (incomeSourceType == SelfEmployment) {
      testUser2
    } else {
      testUser
    }
  }

  def setupBindFutureDateTest(incomeSourceType: IncomeSourceType): Unit = {
    val form: Form[LocalDate] = ceaseEndDateForm(incomeSourceType, setupTestId(incomeSourceType), newIncomeSourceJourney = false)(dateService = mockDateService, user = testUser, messages = messages)
    val futureYear = dateService.getCurrentTaxYearEnd + 1
    val formData = Map("value.day" -> "20", "value.month" -> "12", "value.year" -> s"$futureYear")
    val completedForm = form.bind(formData)

    completedForm.data.get("value.day") shouldBe Some("20")
    completedForm.data.get("value.month") shouldBe Some("12")
    completedForm.data.get("value.year") shouldBe Some(s"$futureYear")
    completedForm.errors shouldBe List(FormError("value", List(s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.future"), List()))
  }

  def setupBindBeforeStartDateTest(incomeSourceType: IncomeSourceType): Unit = {
    val form: Form[LocalDate] = ceaseEndDateForm(incomeSourceType, setupTestId(incomeSourceType), false)(dateService = mockDateService, user = testUser, messages = messages)

    val formData = Map("value.day" -> "27", "value.month" -> "8", "value.year" -> "2016")
    val completedForm = form.bind(formData)

    completedForm.data.get("value.day") shouldBe Some("27")
    completedForm.data.get("value.month") shouldBe Some("8")
    completedForm.data.get("value.year") shouldBe Some("2016")
    completedForm.errors shouldBe List(FormError("value", List(s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeStartDate"), List()))
  }

  def setupBindBeforeContextualTaxYearTest(incomeSourceType: IncomeSourceType): Unit = {
    val form: Form[LocalDate] = ceaseEndDateForm(incomeSourceType, setupTestId(incomeSourceType), false)(dateService = mockDateService, user = testUser4, messages = messages)
    val formData = Map("value.day" -> "4", "value.month" -> "4", "value.year" -> "2023")
    val completedForm = form.bind(formData)

    completedForm.data.get("value.day") shouldBe Some("4")
    completedForm.data.get("value.month") shouldBe Some("4")
    completedForm.data.get("value.year") shouldBe Some("2023")
    completedForm.errors shouldBe List(FormError("value", List(s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeStartDate"), List()))
  }

  "IncomeSourceEndDate form" should {
    "bind with a valid date" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = LocalDate.of(2022, 12, 20)
      val completedForm = form.fill(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid date field - Self employment" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List("incomeSources.cease.endDate.selfEmployment.error.invalid"), List()))
    }
    "bind with an invalid date field - Foreign property" in {
      val form: Form[LocalDate] = ceaseEndDateForm(ForeignProperty, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List("incomeSources.cease.endDate.foreignProperty.error.invalid"), List()))
    }
    "bind with an invalid date field - UK Property" in {
      val form: Form[LocalDate] = ceaseEndDateForm(UkProperty, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List("incomeSources.cease.endDate.ukProperty.error.invalid"), List()))
    }
    "bind with an invalid date field with new journey FS enabled" in {
      enable(IncomeSourcesNewJourney)
      val form: Form[LocalDate] = ceaseEndDateForm(UkProperty, Some(testSelfEmploymentId), true)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "yo", "value.month" -> "yo", "value.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("yo")
      completedForm.data.get("value.month") shouldBe Some("yo")
      completedForm.data.get("value.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.invalid"), List()))
    }
    "bind with a future date - Self Employment" in {
      setupBindFutureDateTest(SelfEmployment)
    }
    "bind with a future date - UK Property" in {
      setupBindFutureDateTest(UkProperty)
    }
    "bind with a future date - Foreign Property" in {
      setupBindFutureDateTest(ForeignProperty)
    }
    "bind with a date earlier than the business start date - Self Employment" in {
      setupBindBeforeStartDateTest(SelfEmployment)
    }
    "bind with a date earlier than the business start date - UK Property" in {
      setupBindBeforeStartDateTest(UkProperty)
    }
    "bind with a date earlier than the business start date - Foreign Property" in {
      setupBindBeforeStartDateTest(ForeignProperty)
    }
    "bind with a date earlier than the contextual tax year - Self Employment" in {
      setupBindBeforeContextualTaxYearTest(SelfEmployment)
    }
    "bind with a date earlier than the contextual tax year - UK Property" in {
      setupBindBeforeContextualTaxYearTest(UkProperty)
    }
    "bind with a date earlier than the contextual tax year - Foreign Property" in {
      setupBindBeforeContextualTaxYearTest(ForeignProperty)
    }
    "give the correct error when binding with a date both before business start date and the 6th of April 2015 - Self Employment" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, setupTestId(SelfEmployment), false)(dateService = mockDateService, user = testUser2, messages = messages)
      val formData = Map("value.day" -> "14", "value.month" -> "10", "value.year" -> "2012")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("14")
      completedForm.data.get("value.month") shouldBe Some("10")
      completedForm.data.get("value.year") shouldBe Some("2012")
      completedForm.errors shouldBe List(
        FormError("value", List(s"incomeSources.cease.endDate.${SelfEmployment.messagesCamel}.beforeEarliestDate"), List())
      )
    }
    "give the correct error when binding with a date before the 6th of April 2015 - Self Employment" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, setupTestId(SelfEmployment), false)(dateService = mockDateService, user = testUser2, messages = messages)
      val formData = Map("value.day" -> "27", "value.month" -> "08", "value.year" -> "2014")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("27")
      completedForm.data.get("value.month") shouldBe Some("08")
      completedForm.data.get("value.year") shouldBe Some("2014")
      completedForm.errors shouldBe List(
        FormError("value", List(s"incomeSources.cease.endDate.${SelfEmployment.messagesCamel}.beforeEarliestDate"), List())
      )
    }
    "bind with a date missing day field" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "", "value.month" -> "12", "value.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required"), List("day")))
    }
    "bind with a date missing month field" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "20", "value.month" -> "", "value.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required"), List("month")))
    }
    "bind with a date missing year field" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "20", "value.month" -> "12", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required"), List("year")))
    }
    "bind with a date missing day and month fields" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "", "value.month" -> "", "value.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required.two"), List("day", "month")))
    }
    "bind with a date missing day and year fields" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "", "value.month" -> "12", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("12")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required.two"), List("day", "year")))
    }
    "bind with a date missing month and year fields" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "20", "value.month" -> "", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("20")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.required.two"), List("month", "year")))
    }
    "bind with a date missing day, month and year fields" in {
      val form: Form[LocalDate] = ceaseEndDateForm(SelfEmployment, Some(testSelfEmploymentId), false)(dateService = mockDateService, user = testUser, messages = messages)
      val formData = Map("value.day" -> "", "value.month" -> "", "value.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("value.day") shouldBe Some("")
      completedForm.data.get("value.month") shouldBe Some("")
      completedForm.data.get("value.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("value", List("dateForm.error.dayMonthAndYear.required.se"), List()))
    }
  }
}
