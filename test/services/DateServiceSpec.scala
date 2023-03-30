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

import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import testUtils.TestSupport

import java.time.LocalDate

class DateServiceSpec extends TestSupport with FeatureSwitching {

  object TestDateService extends DateService()

  def fixture(date: String = "2018-03-29") = new {
    val mockedTestDateService = new DateService() {
      override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = LocalDate.parse(date)
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is on" should {
    "return the next year date if the timeMachineAddYears 1" in {
      enable(TimeMachineAddYear)
      TestDateService.getCurrentDate should equal(LocalDate.now.plusYears(1))
    }
    "return the mocked current date" in {
      val f = fixture()
      enable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is off" should {
    "return the current date" in {
      disable(TimeMachineAddYear)
      TestDateService.getCurrentDate should equal(LocalDate.now)
    }

    "return mocked current date: 2018-03-29" in {
      val f = fixture()
      disable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
      f.mockedTestDateService.isDayBeforeTaxYearLastDay shouldBe true
    }

    "return mocked current date: 2018-04-06" in {
      val f = fixture("2018-04-06")
      disable(TimeMachineAddYear)
      f.mockedTestDateService.isDayBeforeTaxYearLastDay shouldBe false
    }

    "return mocked current date: 2019-04-05" in {
      val f = fixture("2019-04-05")
      disable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate should equal(LocalDate.parse("2019-04-05"))
      f.mockedTestDateService.isDayBeforeTaxYearLastDay shouldBe true
    }

  }

  "The getCurrentTaxYearEnd" should {
    "return the current tax year" in {
      disable(TimeMachineAddYear)
      val expectedYear = if (TestDateService.isDayBeforeTaxYearLastDay(isEnabled(TimeMachineAddYear))) LocalDate.now.getYear
      else LocalDate.now.plusYears(1).getYear
      TestDateService.getCurrentTaxYearEnd shouldBe expectedYear
    }

    "return next tax year when time machine is enabled" in {
      enable(TimeMachineAddYear)
      val expectedYear = if (TestDateService.isDayBeforeTaxYearLastDay(isEnabled(TimeMachineAddYear))) LocalDate.now.plusYears(1).getYear
      else LocalDate.now.plusYears(2).getYear
      TestDateService.getCurrentTaxYearEnd shouldBe expectedYear
    }

  }
}
