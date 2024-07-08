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
import config.FrontendAppConfig
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.Mockito.{mock, reset, when}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time._
import scala.language.reflectiveCalls

class DateServiceSpec extends TestSupport {

  override implicit val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])

  object TestDateService extends DateService()

  def getTaxYearStartDate(year: Int): LocalDate = LocalDate.of(year, 4, 6)

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty), None,
    Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(appConfig)
  }

  def setupGetCurrentTestMocks(timeMachineFS: Boolean = false,
                               addYears: Int = 0, addDays: Int = 0): Unit = {
    when(appConfig.isTimeMachineEnabled).thenReturn(timeMachineFS)
    when(appConfig.timeMachineAddYears).thenReturn(addYears)
    when(appConfig.timeMachineAddDays).thenReturn(addDays)
  }

  def fixture(date: LocalDate) = new {
    val fakeTestDateService: DateService = new DateService() {
      override def getCurrentDate: LocalDate = date
    }
  }

  "The getCurrentDate method when TimeMachine FS is off" should {
    "return the current date and time" in {
      setupGetCurrentTestMocks()

      val getCurrentDate = TestDateService.getCurrentDate

      getCurrentDate shouldBe LocalDate.now()
    }
  }

  "The getCurrentDate method when TimeMachine FS is on" should {
    "return the current date and time when no additional time is added" in {
      setupGetCurrentTestMocks()

      val getCurrentDate = TestDateService.getCurrentDate

      getCurrentDate shouldBe LocalDate.now()
    }

    "return the current date plus a year" when {
      "the TimeMachine is set to add two additional years" in {
        setupGetCurrentTestMocks(timeMachineFS = true, addYears = 2)

        val expectedDate = LocalDate.now().plusYears(2)

        TestDateService.getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but 10 days in the future" when {
      "we set the config to add 10 days to the current date" in {
        setupGetCurrentTestMocks(timeMachineFS = true, addDays = 10)

        val expectedDate = LocalDate.now().plusDays(10)

        TestDateService.getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but 10 days in the past" when {
      "we set the config to subtract 10 days from the current date" in {
        setupGetCurrentTestMocks(timeMachineFS = true, addDays = -10)

        val expectedDate = LocalDate.now().plusDays(-10)

        TestDateService.getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but four years and 20 days in the past" when {
      "the TimeMachine is on, and we have set the TimeMachine to place us on the 27th of August" in {
        setupGetCurrentTestMocks(timeMachineFS = true, addYears = 4, addDays = 20)

        val expectedDate = LocalDate.now().plusDays(20).plusYears(4)

        TestDateService.getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but three years and 25 days in the past" when {
      "the TimeMachine is on, and we have set the TimeMachine to place us on the 27th of August" in {
        setupGetCurrentTestMocks(timeMachineFS = true, addYears = -3, addDays = -25)

        val expectedDate = LocalDate.now().plusDays(-25).plusYears(-3)

        TestDateService.getCurrentDate shouldBe expectedDate
      }
    }
  }

  "The isBeforeLastDayOfTaxYear method" should {
    "tell us we are before the last day of the tax year, when given a day before the last day of the tax year" in {
      val f = fixture(LocalDate.of(2024, 1, 15))

      f.fakeTestDateService.isBeforeLastDayOfTaxYear shouldBe true
    }

    "tell us we are NOT before the last day of the tax year, when given a day after the last day of the tax year" in {
      val f = fixture(LocalDate.of(2024, 12, 25))

      f.fakeTestDateService.isBeforeLastDayOfTaxYear shouldBe false
    }
  }

  "The getCurrentTaxYearEnd method" should {
    "tell us the end year of the given tax year when we are before the last day of the tax year" in {
      val f = fixture(LocalDate.of(1999, 2, 22))

      f.fakeTestDateService.getCurrentTaxYearEnd shouldBe 1999
    }
    "tell us the end year of the given tax year when we are after the last day of the tax year" in {
      val f = fixture(LocalDate.of(1999, 8, 27))

      f.fakeTestDateService.getCurrentTaxYearEnd shouldBe 2000
    }
  }

  "The getCurrentTaxYearStart method" should {
    "tell us the start date of the given tax year when we are before the last day of the tax year" in {
      val f = fixture(LocalDate.of(1999, 2, 22))
      val expectedDate: LocalDate = LocalDate.of(1998, 4, 6)

      f.fakeTestDateService.getCurrentTaxYearStart shouldBe expectedDate
    }
    "tell us the start date of the given tax year when we are after the last day of the tax year" in {
      val f = fixture(LocalDate.of(1999, 8, 27))
      val expectedDate: LocalDate = LocalDate.of(1999, 4, 6)

      f.fakeTestDateService.getCurrentTaxYearStart shouldBe expectedDate
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
