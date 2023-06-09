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

package forms

import forms.incomeSources.add.{BusinessStartDateForm, BusinessTradeForm}
import forms.incomeSources.cease.UKPropertyEndDateForm
import generators.IncomeSourceGens.{Day, businessNameGenerator, businessTradeGenerator, dateGenerator}
import implicits.ImplicitDateFormatter
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties
import services.DateServiceInterface
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils

import java.time.LocalDate
import java.time.Month.{APRIL, JANUARY}

object IncomeSourcesFormsSpec extends Properties("incomeSourcesForms.validation") with TestSupport {

  private val currentDate: LocalDate = LocalDate.of(2075, 1, 1)

  val testDateService = new DateServiceInterface {

    override def getCurrentDate(isTimeMachineEnabled: Boolean): LocalDate = currentDate

    override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean): Int = currentDate.getYear

    override def isBeforeLastDayOfTaxYear(isTimeMachineEnabled: Boolean): Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate): String = {
      val startDateYear = startDate.getYear
      val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

      if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
        accountingPeriodEndDate.toString
      } else {
        accountingPeriodEndDate.plusYears(1).toString
      }
    }
  }
  val testDateFormatter = new ImplicitDateFormatter {
    override implicit val languageUtils: LanguageUtils = languageUtils
  }

  val ukPropertyFormFactory = new UKPropertyEndDateForm(testDateService)
  val ukPropertyForm = ukPropertyFormFactory(individualUser)

  val businessNameForm = (optValue: Option[String]) => BusinessNameForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map(BusinessNameForm.bnf -> value))
  )

  val businessTradeForm = (optValue: Option[String]) => BusinessTradeForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map("addBusinessTrade" -> value))
  )

  val ukPropertyFormUnderTest = (date: Day) => ukPropertyForm.bind(
    Map("uk-property-end-date.day" -> date.day,
      "uk-property-end-date.month" -> date.month,
      "uk-property-end-date.year" -> date.year)
  )

  val businessStartDateCheckForm = (date: Day) => {
    BusinessStartDateForm()(messages, testDateService, mockImplicitDateFormatter).bind(
      Map("add-business-start-date.day" -> date.day,
        "add-business-start-date.month" -> date.month,
        "add-business-start-date.year" -> date.year)
    )
  }

  property("businessName") = forAll(businessNameGenerator) { (charsList: List[Char]) =>
    (charsList.length > 0 && charsList.length <= BusinessNameForm.businessNameLength) ==> {
      val businessName = charsList.mkString("")
      businessNameForm(Some(businessName)).errors.isEmpty
    }
  }

  property("businessTrade") = forAll(businessTradeGenerator) { (charsList: List[Char]) =>
    val businessTrade = charsList.mkString("").trim
    (businessTrade.length > 2) ==> {
      businessTradeForm(Some(businessTrade)).errors.isEmpty
    }
  }

  property("ukPropertyEndDate") = forAll(dateGenerator(currentDate)) { date =>
    ukPropertyFormUnderTest(date).errors.isEmpty
  }

  property("businessStartDate") = forAll(dateGenerator(currentDate)) { date =>
    businessStartDateCheckForm(date).errors.isEmpty
  }

}