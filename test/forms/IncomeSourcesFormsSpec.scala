/*
 * Copyright 2024 HM Revenue & Customs
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

import forms.incomeSources.add.{BusinessNameForm, BusinessTradeForm}
import forms.incomeSources.cease.UKPropertyEndDateForm
import generators.IncomeSourceGens.{Day, businessNameGenerator, businessTradeGenerator, dateGenerator}
import implicits.ImplicitDateFormatter
import org.scalacheck.Prop.{forAll, propBoolean}
import org.scalacheck.Properties
import services.DateServiceInterface
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils
import _root_.models.incomeSourceDetails.TaxYear
import java.time.LocalDate
import java.time.Month.{APRIL, JANUARY}

object IncomeSourcesFormsSpec extends Properties("incomeSourcesForms.validation") with TestSupport {

  private val currentDate: LocalDate = LocalDate.of(2075, 1, 1)

  val testDateService = new DateServiceInterface {

    override def getCurrentDate: LocalDate = currentDate

    override def getCurrentTaxYear: TaxYear = TaxYear.forYearEnd(currentDate.getYear)

    override def getCurrentTaxYearEnd: Int = currentDate.getYear

    override def getCurrentTaxYearStart: LocalDate = currentDate

    override def isBeforeLastDayOfTaxYear: Boolean = false

    override def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean = false

    override def getAccountingPeriodEndDate(startDate: LocalDate):LocalDate = {
      val startDateYear = startDate.getYear
      val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

      if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
        accountingPeriodEndDate
      } else {
        accountingPeriodEndDate.plusYears(1)
      }
    }

    override protected def now(): LocalDate = currentDate
  }

  val ukPropertyFormFactory = new UKPropertyEndDateForm(testDateService)
  val ukPropertyForm = ukPropertyFormFactory(individualUser)

  val businessNameForm = (optValue: Option[String]) => BusinessNameForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map("business-name" -> value))
  )

  val businessTradeForm = (optValue: Option[String]) => BusinessTradeForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map("business-trade" -> value))
  )

  val ukPropertyFormUnderTest = (date: Day) => ukPropertyForm.bind(
    Map("uk-property-end-date.day" -> date.day,
      "uk-property-end-date.month" -> date.month,
      "uk-property-end-date.year" -> date.year)
  )

//  property("businessName") = forAll(businessNameGenerator) { (charsList: List[Char]) =>
//    (charsList.length > 0 && charsList.length <= BusinessNameForm.MAX_LENGTH) ==> {
//      val businessName = charsList.mkString("")
//      businessNameForm(Some(businessName)).errors.isEmpty
//    }
//  }
//
//  property("businessTrade") = forAll(businessTradeGenerator) { (charsList: List[Char]) =>
//    val businessTrade = charsList.mkString("").trim
//    (businessTrade.length > 2) ==> {
//      businessTradeForm(Some(businessTrade)).errors.isEmpty
//    }
//  }
//
//  property("ukPropertyEndDate") = forAll(dateGenerator(currentDate)) { date =>
//    ukPropertyFormUnderTest(date).errors.isEmpty
//  }
}