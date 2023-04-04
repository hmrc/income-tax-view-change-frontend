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

import config.FrontendAppConfig

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.{Inject, Singleton}

@Singleton
class DateService @Inject()(implicit val frontendAppConfig: FrontendAppConfig) extends DateServiceInterface{

  def getCurrentDate(isTimeMachineEnabled: Boolean): LocalDate = {
    if (isTimeMachineEnabled) {
      frontendAppConfig
        .timeMachineAddYears.map(LocalDate.now().plusYears(_))
        .getOrElse(LocalDate.now())
    } else {
      LocalDate.now()
    }
  }

  // with respect to the current calendar year
  private def TAX_YEAR_LAST_DAY(isTimeMachineEnabled: Boolean): LocalDate = LocalDate.of(getCurrentDate(isTimeMachineEnabled).getYear, APRIL, 6)

  def isDayBeforeTaxYearLastDay(isTimeMachineEnabled: Boolean) : Boolean = {
    val currentDate = getCurrentDate(isTimeMachineEnabled)
    currentDate.isBefore(TAX_YEAR_LAST_DAY(isTimeMachineEnabled))
  }

  def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean): Int = {
    val currentDate = getCurrentDate(isTimeMachineEnabled)
    if (isDayBeforeTaxYearLastDay(isTimeMachineEnabled)) currentDate.getYear else currentDate.getYear + 1
  }
}

trait DateServiceInterface {
  def getCurrentDate(isTimeMachineEnabled: Boolean): LocalDate

  def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean = false): Int

  def isDayBeforeTaxYearLastDay(isTimeMachineEnabled: Boolean): Boolean
}
