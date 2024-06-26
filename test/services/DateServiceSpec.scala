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
import config.featureswitch.FeatureSwitching
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time._
import scala.language.reflectiveCalls

class DateServiceSpec extends TestSupport with FeatureSwitching {

  object TestDateService extends DateService()

  def getTaxYearStartDate(year: Int): LocalDate = LocalDate.of(year, 4, 6)

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty), None,
    Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  def fixture(date: String) = new {
    val mockedTestDateService = new DateService() {
      override def getCurrentDate: LocalDate = {
//        val timeMachineIsOn: Boolean = isEnabled(TimeMachineAddYear)(testUser)

//        if (timeMachineIsOn) {
//          LocalDate.parse(date).plusYears(1)
//        } else {
          LocalDate.parse(date)
//        }
      }
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is on" should {
    "return the next year date if the timeMachineAddYears 1" in {
      //enable(TimeMachineAddYear)
      val f = fixture(fixedDate.toString)
//      f.mockedTestDateService.getCurrentDate  should equal(fixedDate.plusYears(1))
      f.mockedTestDateService.getCurrentDate  should equal(fixedDate.plusYears(0))
    }
    "return the mocked current date" in {
      val f = fixture("2018-03-29")
      //enable(TimeMachineAddYear)
//      f.mockedTestDateService.getCurrentDate should equal(LocalDate.parse("2019-03-29"))
      f.mockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
    }
  }

  "The getCurrentDate method when TimeMachineAddYear FS is off" should {
    "return the current date" in {
      //disable(TimeMachineAddYear)
      val f = fixture(fixedDate.toString)
      f.mockedTestDateService.getCurrentDate should equal(fixedDate)
    }

    "return mocked current date: 2018-03-29" in {
      val f = fixture("2018-03-29")
      //disable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate should equal(LocalDate.parse("2018-03-29"))
      f.mockedTestDateService.isBeforeLastDayOfTaxYear shouldBe true
    }

    "return mocked current date: 2018-04-06" in {
      val f = fixture("2018-04-06")
      //disable(TimeMachineAddYear)
      f.mockedTestDateService.isBeforeLastDayOfTaxYear shouldBe false
    }

    "return mocked current date: 2019-04-05" in {
      val f = fixture("2019-04-05")
      //disable(TimeMachineAddYear)
      f.mockedTestDateService.getCurrentDate  should equal(LocalDate.parse("2019-04-05"))
      f.mockedTestDateService.isBeforeLastDayOfTaxYear shouldBe true
    }

  }

  "The getCurrentTaxYearEnd" should {
    "return the current tax year when time machine is disabled and isBeforeLastDayOfTaxYear = false" in {
      //disable(TimeMachineAddYear)
      val f = fixture("2018-02-26")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) f.mockedTestDateService.getCurrentDate .getYear
      else f.mockedTestDateService.getCurrentDate .plusYears(1).getYear
      f.mockedTestDateService.getCurrentTaxYearEnd shouldBe expectedYear
    }
    "return the current tax year when time machine is disabled and isBeforeLastDayOfTaxYear = true" in {
      //disable(TimeMachineAddYear)
      val f = fixture("2018-08-27")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) f.mockedTestDateService.getCurrentDate .getYear
      else f.mockedTestDateService.getCurrentDate .plusYears(1).getYear
      f.mockedTestDateService.getCurrentTaxYearEnd shouldBe expectedYear
    }

    "return next tax year when time machine is enabled and isBeforeLastDayOfTaxYear = false" in {
      //enable(TimeMachineAddYear)
      val f = fixture("2017-02-26")
//      val f = fixture("2018-02-26")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) {
        f.mockedTestDateService.getCurrentDate.getYear
      } else f.mockedTestDateService.getCurrentDate.plusYears(1).getYear
      f.mockedTestDateService.getCurrentTaxYearEnd shouldBe expectedYear
    }
    "return next tax year when time machine is enabled and isBeforeLastDayOfTaxYear = true" in {
      //enable(TimeMachineAddYear)
//      val f = fixture("2018-08-27")
      val f = fixture("2017-08-27")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) {
        f.mockedTestDateService.getCurrentDate .getYear
      } else {
        f.mockedTestDateService.getCurrentDate .plusYears(1).getYear
      }
      f.mockedTestDateService.getCurrentTaxYearEnd shouldBe expectedYear
    }
  }

  "getCurrentTaxYearStart" should {
    "return the start of the current tax year when time machine is disabled and isBeforeLastDayOfTaxYear = false" in {
      //disable(TimeMachineAddYear)
      val f = fixture("2018-02-26")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) {
        f.mockedTestDateService.getCurrentDate .minusYears(1).getYear
      } else f.mockedTestDateService.getCurrentDate .getYear
      f.mockedTestDateService.getCurrentTaxYearStart shouldBe getTaxYearStartDate(expectedYear)
    }
    "return the start of the current tax year when time machine is disabled and isBeforeLastDayOfTaxYear = true" in {
      //disable(TimeMachineAddYear)
      val f = fixture("2018-08-27")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) {
        f.mockedTestDateService.getCurrentDate .minusYears(1).getYear
      } else f.mockedTestDateService.getCurrentDate .getYear
      f.mockedTestDateService.getCurrentTaxYearStart shouldBe getTaxYearStartDate(expectedYear)
    }

    "return the start of the next tax year when time machine is enabled and isBeforeLastDayOfTaxYear = false" in {
      //enable(TimeMachineAddYear)
//      val f = fixture("2018-02-26")
      val f = fixture("2017-02-26")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) {
        f.mockedTestDateService.getCurrentDate .minusYears(1).getYear
      } else f.mockedTestDateService.getCurrentDate .getYear
      f.mockedTestDateService.getCurrentTaxYearStart shouldBe getTaxYearStartDate(expectedYear)
    }
    "return the start of the next tax year when time machine is enabled and isBeforeLastDayOfTaxYear = true" in {
      //enable(TimeMachineAddYear)
//      val f = fixture("2018-08-27")
      val f = fixture("2017-08-27")
      val expectedYear = if (f.mockedTestDateService.isBeforeLastDayOfTaxYear) {
        f.mockedTestDateService.getCurrentDate .minusYears(1).getYear
      } else f.mockedTestDateService.getCurrentDate .getYear
      f.mockedTestDateService.getCurrentTaxYearStart shouldBe getTaxYearStartDate(expectedYear)
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
}
