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

package services

import auth.MtdItUser
import com.google.inject.ImplementedBy
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import models.admin.TimeMachineAddYear
import models.incomeSourceDetails.TaxYear

import java.time.LocalDate
import java.time.Month.{APRIL, JANUARY}
import javax.inject.{Inject, Singleton}

@Singleton
class DateService @Inject()(implicit val frontendAppConfig: FrontendAppConfig) extends DateServiceInterface with FeatureSwitching {
  val appConfig: FrontendAppConfig = frontendAppConfig

  def getCurrentDate(implicit user: MtdItUser[_]): LocalDate = {
    if (isEnabled(TimeMachineAddYear)) {
      frontendAppConfig.timeMachineAddYears.map(years =>
        LocalDate.now().plusYears(years)
      ).getOrElse(LocalDate.now())
    } else {
      LocalDate.now()
    }
  }

  def getCurrentTaxYearEnd(implicit user: MtdItUser[_]): Int = {
    val currentDate: LocalDate = getCurrentDate
    if (isBeforeLastDayOfTaxYear) currentDate.getYear else currentDate.getYear + 1
  }

  def getCurrentTaxYearStart(implicit user: MtdItUser[_]): LocalDate = {
    val currentDate: LocalDate = getCurrentDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, APRIL, 6))) LocalDate.of(currentDate.getYear - 1, APRIL, 6)
    else LocalDate.of(currentDate.getYear, APRIL, 6)
  }

  def isBeforeLastDayOfTaxYear(implicit user: MtdItUser[_]): Boolean = {
    val currentDate: LocalDate = getCurrentDate
    val lastDayOfTaxYear = getLastDayOfTaxYear
    currentDate.isBefore(lastDayOfTaxYear)
  }

  def isAfterTaxReturnDeadlineButBeforeTaxYearEnd(implicit user: MtdItUser[_]): Boolean = {
    val currentDate: LocalDate = getCurrentDate
    val lastDayOfTaxReturn = getLastDayOfTaxReturn
    val lastDayOfTaxYear = getLastDayOfTaxYear
    currentDate.isAfter(lastDayOfTaxReturn) && currentDate.isBefore(lastDayOfTaxYear)
  }

  private def getLastDayOfTaxYear(implicit user: MtdItUser[_]): LocalDate = {
    val currentYear: Int = getCurrentDate.getYear
    LocalDate.of(currentYear, APRIL, 6)
  }

  private def getLastDayOfTaxReturn(implicit user: MtdItUser[_]): LocalDate = {
    val currentYear: Int = getCurrentDate.getYear
    LocalDate.of(currentYear, JANUARY, 31)
  }

  def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate = {
    val startDateYear = startDate.getYear
    val accountingPeriodEndDate = LocalDate.of(startDateYear, APRIL, 5)

    if (startDate.isBefore(accountingPeriodEndDate) || startDate.isEqual(accountingPeriodEndDate)) {
      accountingPeriodEndDate
    } else {
      accountingPeriodEndDate.plusYears(1)
    }
  }

  def getCurrentTaxYear(implicit user: MtdItUser[_]): TaxYear = {
    val yearEnd = getCurrentTaxYearEnd
    TaxYear.forYearEnd(yearEnd)
  }
}

@ImplementedBy(classOf[DateService])
trait DateServiceInterface {
  def getCurrentDate(implicit user: MtdItUser[_]): LocalDate

  def getCurrentTaxYear(implicit user: MtdItUser[_]): TaxYear

  def getCurrentTaxYearEnd(implicit user: MtdItUser[_]): Int

  def getCurrentTaxYearStart(implicit user: MtdItUser[_]): LocalDate

  def isBeforeLastDayOfTaxYear(implicit user: MtdItUser[_]): Boolean

  def isAfterTaxReturnDeadlineButBeforeTaxYearEnd(implicit user: MtdItUser[_]): Boolean

  def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate

}
