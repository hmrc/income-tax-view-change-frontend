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
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.{Inject, Singleton}

@Singleton
class DateService @Inject()(implicit val frontendAppConfig: FrontendAppConfig) extends DateServiceInterface with FeatureSwitching {

  override lazy val appConfig: FrontendAppConfig = implicitly

  def getCurrentDate: LocalDate = {
    if (isEnabled(TimeMachineAddYear)) {
      frontendAppConfig
        .timeMachineAddYears.map(LocalDate.now().plusYears(_))
        .getOrElse(LocalDate.now())
    } else {
      LocalDate.now()
    }
  }

  // with respect to the current calendar year
  private def TAX_YEAR_LAST_DAY: LocalDate = LocalDate.of(getCurrentDate.getYear, APRIL, 6)

  def isDayBeforeTaxYearLastDay: Boolean = {
    val currentDate = getCurrentDate
    currentDate.isBefore(TAX_YEAR_LAST_DAY)
  }

  def getCurrentTaxYearEnd: Int = {
    val currentDate = getCurrentDate
    if (isDayBeforeTaxYearLastDay) currentDate.getYear else currentDate.getYear + 1
  }
}

@ImplementedBy(classOf[DateService])
trait DateServiceInterface {
  def getCurrentDate: LocalDate

  def getCurrentTaxYearEnd: Int

  def isDayBeforeTaxYearLastDay: Boolean
}
