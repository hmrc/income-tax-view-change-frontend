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

  def beforeAril: Boolean = {
    val currentDate = getCurrentDate
    currentDate.isBefore(LocalDate.of(currentDate.getYear, APRIL, 6))
  }

  def getCurrentTaxYearEnd: Int = {
    val currentDate = getCurrentDate
    if (beforeAril) currentDate.getYear else currentDate.getYear + 1
  }
}

trait DateServiceInterface {
  def getCurrentDate: LocalDate

  def getCurrentTaxYearEnd: Int
}
