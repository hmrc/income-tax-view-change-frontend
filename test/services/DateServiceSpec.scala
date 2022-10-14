/*
 * Copyright 2022 HM Revenue & Customs
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
  object MockedTestDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse("2018-03-29")
  }

  "The getCurrentDate method when TimeMachineAddYear FS is on" should {
    "return the next year date if the timeMachineAddYears 1" in {
      enable(TimeMachineAddYear)
      TestDateService.getCurrentDate should equal(LocalDate.now.plusYears(1))
    }
    "return the mocked current date" in {
      enable(TimeMachineAddYear)
      MockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is off" should {
    "return the current date" in {
      disable(TimeMachineAddYear)
      TestDateService.getCurrentDate should equal(LocalDate.now)
    }
    "return the mocked current date" in {
      disable(TimeMachineAddYear)
      MockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
    }
  }

  "The getCurrentTaxYearEnd method when TimeMachineAddYear FS is on" should {
    "return the next year if the current date is before april 6 and the timeMachineAddYears 1" in {
      enable(TimeMachineAddYear)
      TestDateService.getCurrentTaxYearEnd(LocalDate.parse("2018-03-29")) shouldBe 2019
    }
    "return the following year if current date is on or after april 6 and the timeMachineAddYears 1" in {
      enable(TimeMachineAddYear)
      TestDateService.getCurrentTaxYearEnd(LocalDate.parse("2018-04-06")) shouldBe 2020
    }
  }

  "The getCurrentTaxYearEnd method when TimeMachineAddYear FS is off" should {
    "return the current year if the current date is before april 6" in {
      disable(TimeMachineAddYear)
      TestDateService.getCurrentTaxYearEnd(LocalDate.parse("2018-03-29")) shouldBe 2018
    }
    "return the next year if current date is on or after april 6" in {
      disable(TimeMachineAddYear)
      TestDateService.getCurrentTaxYearEnd(LocalDate.parse("2018-04-06")) shouldBe 2019
    }
  }
}
