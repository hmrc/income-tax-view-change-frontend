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

import testUtils.TestSupport

import java.time.LocalDate

class DateServiceSpec extends TestSupport {

  object TestDateService extends DateService()
  object MockedTestDateService extends DateService() {
    override def getCurrentDate: LocalDate = LocalDate.parse("2018-03-29")
  }
  "The DateService.getCurrentDate method" should {
    "return the current date" in {
      TestDateService.getCurrentDate should equal(LocalDate.now)
    }
    "return the mocked current date" in {
      MockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
    }
  }

  "The DateService.getCurrentTaxYearEnd method" should {
    "return the current year if the current date is before april 6" in {
      TestDateService.getCurrentTaxYearEnd(LocalDate.parse("2018-03-29")) shouldBe 2018
    }
    "return the next year if current date is on or after april 6" in {
      TestDateService.getCurrentTaxYearEnd(LocalDate.parse("2018-04-06")) shouldBe 2019
    }
  }
}
