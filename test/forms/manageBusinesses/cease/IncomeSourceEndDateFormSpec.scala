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

package forms.manageBusinesses.cease

import auth.MtdItUser
import authV2.AuthActionsTestData.getMinimalMTDITUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.models.DateFormElement
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

class IncomeSourceEndDateFormSpec extends AnyWordSpec with Matchers with TestSupport {
  val mockDateService: DateService = app.injector.instanceOf[DateService]

  val testUser: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), ukPlusForeignPropertyWithSoleTraderIncomeSource, false, fakeRequestNoSession)

  val testUser2: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), businessIncome2, false, fakeRequestNoSession)

  val testUser3: MtdItUser[_] = getMinimalMTDITUser(Some(Individual), ukForeignSoleTraderIncomeSourceBeforeContextualTaxYear, false, fakeRequestNoSession)

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
    val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(incomeSourceType, setupTestId(incomeSourceType), false)(testUser)
    val futureYear = dateService.getCurrentTaxYearEnd + 1
    val formData = Map("income-source-end-date.day" -> "20", "income-source-end-date.month" -> "12", "income-source-end-date.year" -> s"$futureYear")
    val completedForm = form.bind(formData)

    completedForm.data.get("income-source-end-date.day") shouldBe Some("20")
    completedForm.data.get("income-source-end-date.month") shouldBe Some("12")
    completedForm.data.get("income-source-end-date.year") shouldBe Some(s"$futureYear")
    completedForm.errors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.future"), List()))
  }

  def setupBindBeforeStartDateTest(incomeSourceType: IncomeSourceType): Unit = {
    val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(incomeSourceType, setupTestId(incomeSourceType), false)(testUser)
    val formData = Map("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "8", "income-source-end-date.year" -> "2016")
    val completedForm = form.bind(formData)

    completedForm.data.get("income-source-end-date.day") shouldBe Some("27")
    completedForm.data.get("income-source-end-date.month") shouldBe Some("8")
    completedForm.data.get("income-source-end-date.year") shouldBe Some("2016")
    completedForm.errors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${incomeSourceType.messagesCamel}.beforeStartDate"), List()))
  }

  def setupBindContextualTaxYear(incomeSourceType: IncomeSourceType, dateDMYYYY:(Int, Int, Int)): Seq[FormError] = {
    val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(incomeSourceType, setupTestId(incomeSourceType), newIncomeSourceJourney = false)(testUser3)
    val formData = Map(
      "income-source-end-date.day" -> s"${dateDMYYYY._1}",
      "income-source-end-date.month" -> s"${dateDMYYYY._2}",
      "income-source-end-date.year" -> s"${dateDMYYYY._3}"
    )
    val completedForm = form.bind(formData)

    completedForm.data.get("income-source-end-date.day") shouldBe Some(s"${dateDMYYYY._1}")
    completedForm.data.get("income-source-end-date.month") shouldBe Some(s"${dateDMYYYY._2}")
    completedForm.data.get("income-source-end-date.year") shouldBe Some(s"${dateDMYYYY._3}")
    completedForm.errors
  }

  "IncomeSourceEndDate form" should {
    "bind with a valid date" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = DateFormElement(LocalDate.of(2022, 12, 20))
      val completedForm = form.fill(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("2022")
      completedForm.errors shouldBe List.empty
    }
    "bind with an invalid date field - Self employment" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "yo", "income-source-end-date.month" -> "yo", "income-source-end-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("incomeSources.cease.endDate.selfEmployment.error.invalid"), List()))
    }
    "bind with an invalid date field - Foreign property" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(ForeignProperty, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "yo", "income-source-end-date.month" -> "yo", "income-source-end-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("incomeSources.cease.endDate.foreignProperty.error.invalid"), List()))
    }
    "bind with an invalid date field - UK Property" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(UkProperty, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "yo", "income-source-end-date.month" -> "yo", "income-source-end-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("incomeSources.cease.endDate.ukProperty.error.invalid"), List()))
    }
    "bind with an invalid date field with new journey FS enabled" in {
      enable(IncomeSourcesNewJourney)
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(UkProperty, Some(testSelfEmploymentId), true)(testUser)
      val formData = Map("income-source-end-date.day" -> "yo", "income-source-end-date.month" -> "yo", "income-source-end-date.year" -> "supp")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("yo")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("supp")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.invalid"), List()))
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
      val formErrors = setupBindContextualTaxYear(SelfEmployment, dateDMYYYY = (5, 4, 2023))
      formErrors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${SelfEmployment.messagesCamel}.beforeStartDate"), List()))
    }
    "bind with a date on the contextual tax year - Self Employment" in {
      val formErrors = setupBindContextualTaxYear(SelfEmployment, dateDMYYYY = (6, 4, 2023))
      formErrors shouldBe List.empty
    }
    "bind with a date after the contextual tax year - Self Employment" in {
      val formErrors = setupBindContextualTaxYear(SelfEmployment, dateDMYYYY = (12, 5, mockDateService.getCurrentTaxYearEnd + 1))
      formErrors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${SelfEmployment.messagesCamel}.future"), List()))
    }

    "bind with a date earlier than the contextual tax year - UK Property" in {
      val formErrors = setupBindContextualTaxYear(UkProperty, dateDMYYYY = (5, 4, 2023))
      formErrors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${UkProperty.messagesCamel}.beforeStartDate"), List()))
    }
    "bind with a date on the contextual tax year - UK Property" in {
      val formErrors = setupBindContextualTaxYear(UkProperty, dateDMYYYY = (6, 4, 2023))
      formErrors shouldBe List.empty
    }
    "bind with a date after the contextual tax year - UK Property" in {
      val formErrors = setupBindContextualTaxYear(UkProperty, dateDMYYYY = (6, 4, mockDateService.getCurrentTaxYearEnd+1))
      formErrors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${UkProperty.messagesCamel}.future"), List()))
    }

    "bind with a date earlier than the contextual tax year - Foreign Property" in {
      val formErrors = setupBindContextualTaxYear(ForeignProperty, dateDMYYYY = (5, 4, 2023))
      formErrors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${ForeignProperty.messagesCamel}.beforeStartDate"), List()))
    }
    "bind with a date on the contextual tax year - Foreign Property" in {
      val formErrors = setupBindContextualTaxYear(ForeignProperty, dateDMYYYY = (6, 4, 2023))
      formErrors shouldBe List()
    }
    "bind with a date after the contextual tax year - Foreign Property" in {
      val formErrors = setupBindContextualTaxYear(ForeignProperty, dateDMYYYY = (6, 4, mockDateService.getCurrentTaxYearEnd+1))
      formErrors shouldBe List(FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${ForeignProperty.messagesCamel}.future"), List()))
    }

    "give the correct error when binding with a date both before business start date and the 6th of April 2015 - Self Employment" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, setupTestId(SelfEmployment), false)(testUser2)
      val formData = Map("income-source-end-date.day" -> "14", "income-source-end-date.month" -> "10", "income-source-end-date.year" -> "2012")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("14")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("10")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("2012")
      completedForm.errors shouldBe List(
        FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${SelfEmployment.messagesCamel}.beforeEarliestDate"), List())
      )
    }
    "give the correct error when binding with a date before the 6th of April 2015 - Self Employment" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, setupTestId(SelfEmployment), false)(testUser2)
      val formData = Map("income-source-end-date.day" -> "27", "income-source-end-date.month" -> "08", "income-source-end-date.year" -> "2014")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("27")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("08")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("2014")
      completedForm.errors shouldBe List(
        FormError("income-source-end-date", List(s"incomeSources.cease.endDate.${SelfEmployment.messagesCamel}.beforeEarliestDate"), List())
      )
    }
    "bind with a date missing day field" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "", "income-source-end-date.month" -> "12", "income-source-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.day.required"), List()))
    }
    "bind with a date missing month field" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "20", "income-source-end-date.month" -> "", "income-source-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.month.required"), List()))
    }
    "bind with a date missing year field" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "20", "income-source-end-date.month" -> "12", "income-source-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.year.required"), List()))
    }
    "bind with a date missing day and month fields" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "", "income-source-end-date.month" -> "", "income-source-end-date.year" -> "2016")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("2016")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.dayAndMonth.required"), List()))
    }
    "bind with a date missing day and year fields" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "", "income-source-end-date.month" -> "12", "income-source-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("12")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.dayAndYear.required"), List()))
    }
    "bind with a date missing month and year fields" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "20", "income-source-end-date.month" -> "", "income-source-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("20")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.monthAndYear.required"), List()))
    }
    "Enter the date your self-employed business stopped" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(SelfEmployment, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "", "income-source-end-date.month" -> "", "income-source-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.dayMonthAndYear.required.se"), List()))
    }
    "Enter the date your UK property business stopped" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(UkProperty, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "", "income-source-end-date.month" -> "", "income-source-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.dayMonthAndYear.required.uk"), List()))
    }
    "Enter the date your foreign property business stopped" in {
      val form: Form[DateFormElement] = new IncomeSourceEndDateForm(mockDateService).apply(ForeignProperty, Some(testSelfEmploymentId), false)(testUser)
      val formData = Map("income-source-end-date.day" -> "", "income-source-end-date.month" -> "", "income-source-end-date.year" -> "")
      val completedForm = form.bind(formData)

      completedForm.data.get("income-source-end-date.day") shouldBe Some("")
      completedForm.data.get("income-source-end-date.month") shouldBe Some("")
      completedForm.data.get("income-source-end-date.year") shouldBe Some("")
      completedForm.errors shouldBe List(FormError("income-source-end-date", List("dateForm.error.dayMonthAndYear.required.fp"), List()))
    }

  }
}

