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

import _root_.models.incomeSourceDetails.TaxYear
import forms.manageBusinesses.add.{BusinessNameForm, BusinessTradeForm}
import org.scalacheck.Properties
import services.DateServiceInterface
import testUtils.TestSupport

import java.time.LocalDate
import java.time.Month.APRIL

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

    override def isWithin30Days(date: LocalDate): Boolean = false
  }

  val businessNameForm = (optValue: Option[String]) => BusinessNameForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map("business-name" -> value))
  )

  val businessTradeForm = (optValue: Option[String]) => BusinessTradeForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(value => Map("business-trade" -> value))
  )

}