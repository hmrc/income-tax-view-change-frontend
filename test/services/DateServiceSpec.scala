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
import scala.language.reflectiveCalls

import java.time.LocalDate

class DateServiceSpec extends TestSupport with FeatureSwitching {

  object TestDateService extends DateService()

  def fixture(date: String) = new {
    val mockedTestDateService = new DateService() {
      override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = {
        if (isTimeMachineEnabled) {
          LocalDate.parse(date).plusYears(1)
        } else {
          LocalDate.parse(date)
        }
      }
    }
  }

  def fixture2(date: String) = new {
    val mockedTestDateService2 = new DateService() {
      override def getCurrentDate(isTimeMachineEnabled: Boolean = false): LocalDate = {
        if (isTimeMachineEnabled) {
          LocalDate.parse(date).plusYears(1)
        } else {
          LocalDate.parse(date)
        }
      }

      override def getCurrentTaxYearEnd(isTimeMachineEnabled: Boolean = false): Int = {

        val testDate: LocalDate = getCurrentDate(isTimeMachineEnabled)

        if (isBeforeLastDayOfTaxYear(isTimeMachineEnabled)) testDate.getYear else testDate.getYear + 1
      }
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is on" should {
    "return the next year date if the timeMachineAddYears 1" in {
      enable(TimeMachineAddYear)
      TestDateService.getCurrentDate(true) should equal(LocalDate.now.plusYears(1))
    }
    "return the mocked current date" in {
      val f = fixture("2018-03-29")
      enable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate(true) should equal(LocalDate.parse("2019-03-29"))
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is off" should {
    "return the current date" in {
      disable(TimeMachineAddYear)
      TestDateService.getCurrentDate() should equal(LocalDate.now)
    }

    "return mocked current date: 2018-03-29" in {
      val f = fixture("2018-03-29")
      disable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate() should equal(LocalDate.parse("2018-03-29"))
      f.mockedTestDateService.isBeforeLastDayOfTaxYear(false) shouldBe true
    }

    "return mocked current date: 2018-04-06" in {
      val f = fixture("2018-04-06")
      disable(TimeMachineAddYear)
      f.mockedTestDateService.isBeforeLastDayOfTaxYear(false) shouldBe false
    }

    "return mocked current date: 2019-04-05" in {
      val f = fixture("2019-04-05")
      disable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate() should equal(LocalDate.parse("2019-04-05"))
      f.mockedTestDateService.isBeforeLastDayOfTaxYear(false) shouldBe true
    }

  }

  "The getCurrentTaxYearEnd" should {
    "return the current tax year" in {
      disable(TimeMachineAddYear)
      val expectedYear = if (TestDateService.isBeforeLastDayOfTaxYear(false)) LocalDate.now.getYear
      else LocalDate.now.plusYears(1).getYear
      TestDateService.getCurrentTaxYearEnd() shouldBe expectedYear
    }

    "return next tax year when time machine is enabled" in {
      enable(TimeMachineAddYear)
      val expectedYear = if (TestDateService.isBeforeLastDayOfTaxYear(true)) LocalDate.now.plusYears(1).getYear
      else LocalDate.now.plusYears(2).getYear
      TestDateService.getCurrentTaxYearEnd(true) shouldBe expectedYear
    }
  }

  "getCurrentTaxYearStart" should {
    "return the start of the current tax year" in {
      disable(TimeMachineAddYear)
      val expectedDate = if (TestDateService.isBeforeLastDayOfTaxYear(false)) LocalDate.of(LocalDate.now.getYear - 1, 4, 6)
      else LocalDate.of(LocalDate.now.getYear, 4, 6)
      TestDateService.getCurrentTaxYearStart(false) shouldBe expectedDate
    }

    "return the start of the next tax year if time machine enabled" in {
      enable(TimeMachineAddYear)
      val expectedDate = if (TestDateService.isBeforeLastDayOfTaxYear(true)) LocalDate.of(LocalDate.now.getYear, 4, 6)
      else LocalDate.of(LocalDate.now.getYear + 1, 4, 6)
      TestDateService.getCurrentTaxYearStart(true) shouldBe expectedDate
    }
  }

  "getAccountingPeriodEndDate" should {
    "return 2020-04-05 (same year as business start year)" when {
      "business start date is 01/01/2020" in {
        val businessStartDate = LocalDate.of(2020, 1, 1)
        TestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2020, 4, 5)
      }
      "business start date is 05/04/2020 (last day of accounting period)" in {
        val businessStartDate = LocalDate.of(2020, 4, 5)
        TestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2020, 4, 5)
      }
    }
    "return 2021-04-05 (business start year + 1)" when {
      "business start date is 06/04/2020" in {
        val businessStartDate = LocalDate.of(2020, 4, 6)
        TestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2021, 4, 5)
      }
      "business start date is 23/07/2020" in {
        val businessStartDate = LocalDate.of(2020, 7, 23)
        TestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2021, 4, 5)
      }
    }
  }
  "getCurrentTaxYearMinusOneEnd" should {
    "return 2018" when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = true" in {
        disable(TimeMachineAddYear)
        val f = fixture("2019-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYear: Int = 2018
        val result = f.mockedTestDateService.getCurrentTaxYearMinusOneEnd(tm)

        result shouldBe taxYear
      }
    }
    "return 2019" when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = false" in {
        disable(TimeMachineAddYear)
        val f = fixture("2019-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYear: Int = 2019
        val result = f.mockedTestDateService.getCurrentTaxYearMinusOneEnd(tm)

        result shouldBe taxYear
      }
    }
    "return 2019" when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = true" in {
        enable(TimeMachineAddYear)
        val f = fixture("2019-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYear: Int = 2019
        val result = f.mockedTestDateService.getCurrentTaxYearMinusOneEnd(tm)

        result shouldBe taxYear
      }
    }
    "return 2020" when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = false" in {
        enable(TimeMachineAddYear)
        val f = fixture("2019-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYear: Int = 2020
        val result = f.mockedTestDateService.getCurrentTaxYearMinusOneEnd(tm)

        result shouldBe taxYear
      }
    }
  }

  "getCurrentTaxYearRange" should {
    "return '18-19' " when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = true" in {
        disable(TimeMachineAddYear)
        val f = fixture2("2019-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "18-19"
        val result = f.mockedTestDateService2.getCurrentTaxYearRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '19-20' " when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = false" in {
        disable(TimeMachineAddYear)
        val f = fixture2("2019-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "19-20"
        val result = f.mockedTestDateService2.getCurrentTaxYearRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '19-20' " when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = true" in {
        enable(TimeMachineAddYear)
        val f = fixture2("2019-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "19-20"
        val result = f.mockedTestDateService2.getCurrentTaxYearRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '20-21' " when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = false" in {
        enable(TimeMachineAddYear)
        val f = fixture2("2019-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "20-21"
        val result = f.mockedTestDateService2.getCurrentTaxYearRange(tm)

        result shouldBe taxYearRange
      }
    }
  }

  "getCurrentTaxYearMinusOneRange" should {
    "return '16-17' " when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = true" in {
        disable(TimeMachineAddYear)
        val f = fixture2("2018-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "16-17"
        val result = f.mockedTestDateService2.getCurrentTaxYearMinusOneRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '17-18' " when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = false" in {
        disable(TimeMachineAddYear)
        val f = fixture2("2018-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "17-18"
        val result = f.mockedTestDateService2.getCurrentTaxYearMinusOneRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '17-18' " when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = true" in {
        enable(TimeMachineAddYear)
        val f = fixture2("2018-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYear: String = "17-18"
        val result = f.mockedTestDateService2.getCurrentTaxYearMinusOneRange(tm)

        result shouldBe taxYear
      }
    }
    "return '18-19' " when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = false" in {
        enable(TimeMachineAddYear)
        val f = fixture("2018-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYear: String = "18-19"
        val result = f.mockedTestDateService.getCurrentTaxYearMinusOneRange(tm)

        result shouldBe taxYear
      }
    }
  }
  "getCurrentTaxYearPlusOneRange" should {
    "return '18-19' " when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = true" in {
        disable(TimeMachineAddYear)
        val f = fixture2("2018-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "18-19"
        val result = f.mockedTestDateService2.getCurrentTaxYearPlusOneRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '19-20' " when {
      "isTimeMachineEnabled = false, and isBeforeLastDayOfTaxYear = false" in {
        disable(TimeMachineAddYear)
        val f = fixture2("2018-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "19-20"
        val result = f.mockedTestDateService2.getCurrentTaxYearPlusOneRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '19-20' " when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = true" in {
        enable(TimeMachineAddYear)
        val f = fixture2("2018-02-26")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "19-20"
        val result = f.mockedTestDateService2.getCurrentTaxYearPlusOneRange(tm)

        result shouldBe taxYearRange
      }
    }
    "return '20-21' " when {
      "isTimeMachineEnabled = true, and isBeforeLastDayOfTaxYear = false" in {
        enable(TimeMachineAddYear)
        val f = fixture2("2018-08-27")

        val tm = isEnabled(TimeMachineAddYear)
        val taxYearRange: String = "20-21"
        val result = f.mockedTestDateService2.getCurrentTaxYearPlusOneRange(tm)

        result shouldBe taxYearRange
      }
    }
  }
}
