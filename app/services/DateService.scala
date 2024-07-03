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
import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.TaxYear
import services.admin.FeatureSwitchServiceImpl

import java.time.LocalDate
import java.time.Month.{APRIL, JANUARY}
import javax.inject.{Inject, Singleton}

@Singleton
class DateService @Inject()(
                            featureSwitchService: FeatureSwitchServiceImpl
                           )(implicit val frontendAppConfig: FrontendAppConfig,
                           ) extends FeatureSwitching with DateServiceInterface {
  val appConfig: FrontendAppConfig = frontendAppConfig

  def getCurrentDate: LocalDate = {
    if (featureSwitchService.isTimeMachineEnabled) {
      LocalDate.now().plusDays(1)
    } else {
      LocalDate.now()
    }
  }

  def getCurrentTaxYearEnd: Int = {
    val currentDate: LocalDate = getCurrentDate
    if (isBeforeLastDayOfTaxYear) currentDate.getYear else currentDate.getYear + 1
  }

  def getCurrentTaxYearStart: LocalDate = {
    val currentDate: LocalDate = getCurrentDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, APRIL, 6))) LocalDate.of(currentDate.getYear - 1, APRIL, 6)
    else LocalDate.of(currentDate.getYear, APRIL, 6)
  }

  def isBeforeLastDayOfTaxYear: Boolean = {
    val currentDate: LocalDate = getCurrentDate
    val lastDayOfTaxYear = getLastDayOfTaxYear
    currentDate.isBefore(lastDayOfTaxYear)
  }

  def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean = {
    val currentDate: LocalDate = getCurrentDate
    val lastDayOfTaxReturn = getLastDayOfTaxReturn
    val lastDayOfTaxYear = getLastDayOfTaxYear
    currentDate.isAfter(lastDayOfTaxReturn) && currentDate.isBefore(lastDayOfTaxYear)
  }

  private def getLastDayOfTaxYear: LocalDate = {
    val currentYear: Int = getCurrentDate.getYear
    LocalDate.of(currentYear, APRIL, 6)
  }

  private def getLastDayOfTaxReturn: LocalDate = {
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

  def getCurrentTaxYear: TaxYear = {
    val yearEnd = getCurrentTaxYearEnd
    TaxYear.forYearEnd(yearEnd)
  }
}

@ImplementedBy(classOf[DateService])
trait DateServiceInterface {
  def getCurrentDate: LocalDate

  def getCurrentTaxYear: TaxYear

  def getCurrentTaxYearEnd: Int

  def getCurrentTaxYearStart: LocalDate

  def isBeforeLastDayOfTaxYear: Boolean

  def isAfterTaxReturnDeadlineButBeforeTaxYearEnd: Boolean

  def getAccountingPeriodEndDate(startDate: LocalDate): LocalDate

}
