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

import com.google.inject.ImplementedBy
import config.FrontendAppConfig

import java.time.{Clock, Duration, LocalDate, ZoneOffset}
import java.time.Month.APRIL
import javax.inject.{Inject, Singleton}

@Singleton
class DateService @Inject()(implicit val frontendAppConfig: FrontendAppConfig) extends DateServiceInterface {

  // TODO: instantiate clock depending on the configuration settings
  private val timeMachineIsOn: Boolean = frontendAppConfig.timeMachineAddYears.nonEmpty

  lazy val clock: Clock = if (timeMachineIsOn) {
    Clock.offset(Clock.systemDefaultZone.withZone(ZoneOffset.UTC), Duration.ofDays(100))
  } else {
    Clock.systemDefaultZone.withZone(ZoneOffset.UTC)
  }

  def getCurrentDate: LocalDate = {
    if (timeMachineIsOn) {
      LocalDate.now(clock)
    } else {
      LocalDate.now(clock)
    }
  }

  def getCurrentTaxYearEnd: Int = {
    val currentDate = getCurrentDate
    if (isBeforeLastDayOfTaxYear) currentDate.getYear else currentDate.getYear + 1
  }

  def getCurrentTaxYearStart: LocalDate = {
    val currentDate = getCurrentDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, APRIL, 6))) LocalDate.of(currentDate.getYear - 1, APRIL, 6)
    else LocalDate.of(currentDate.getYear, APRIL, 6)
  }

  def isBeforeLastDayOfTaxYear: Boolean = {
    val currentDate = getCurrentDate
    val lastDayOfTaxYear = getLastDayOfTaxYear
    currentDate.isBefore(lastDayOfTaxYear)
  }

  private def getLastDayOfTaxYear: LocalDate = {
    val currentYear = getCurrentDate.getYear
    LocalDate.of(currentYear, APRIL, 6)
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
}

@ImplementedBy(classOf[DateService])
trait DateServiceInterface {
  def getCurrentDate: LocalDate

  def getCurrentTaxYearEnd: Int

  def getCurrentTaxYearStart: LocalDate

  def isBeforeLastDayOfTaxYear: Boolean

  def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate
}
