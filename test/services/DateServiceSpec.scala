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
import authV2.AuthActionsTestData.defaultMTDITUser
import config.FrontendAppConfig
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.Mockito.{mock, reset}
import testConstants.BaseTestConstants.testNino
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time._
import scala.language.reflectiveCalls

class DateServiceSpec extends TestSupport {

  override implicit val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])

  final case class DateServiceFixture(fakeTestDateService: DateService)

  val mockDateServiceInterface: DateServiceInterface = mock(classOf[DateServiceInterface])

  class TestDateService(isTimeMachineEnabled: Boolean = false, addYears: Int = 0, addDays: Int = 0) extends DateService() {
    override def now(): LocalDate = fixedDate

    override def getTimeMachineConfig: TimeMachineSettings = TimeMachineSettings(isTimeMachineEnabled = isTimeMachineEnabled, addYears, addDays)
  }

  val baseTestDateService = new TestDateService()

  def getTaxYearStartDate(year: Int): LocalDate = LocalDate.of(year, 4, 6)

  val testUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(appConfig)
  }

  def fixture(date: LocalDate): DateServiceFixture = {
    val fakeTestDateService: DateService = new DateService {
      override def getCurrentDate: LocalDate = date
    }

    DateServiceFixture(fakeTestDateService)
  }


  "The getCurrentDate method when TimeMachine FS is off" should {
    "return the current date and time" in {
      val getCurrentDate = new TestDateService().getCurrentDate

      getCurrentDate shouldBe fixedDate
    }
  }

  "The getCurrentDate method when TimeMachine FS is on" should {
    "return the current date and time when no additional time is added" in {
      val getCurrentDate = new TestDateService(true).getCurrentDate

      getCurrentDate shouldBe fixedDate
    }

    "return the current date plus a year" when {
      "the TimeMachine is set to add two additional years" in {
        val expectedDate = fixedDate.plusYears(2)

        new TestDateService(true, 2).getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but 10 days in the future" when {
      "we set the config to add 10 days to the current date" in {
        val expectedDate = fixedDate.plusDays(10)

        new TestDateService(true, 0, 10).getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but 10 days in the past" when {
      "we set the config to subtract 10 days from the current date" in {
        val expectedDate = fixedDate.plusDays(-10)

        new TestDateService(true, 0, -10).getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the fixed date, but four years and 20 days in the future" when {
      "the TimeMachine is on, and we have set the TimeMachine to place us on the 27th of August" in {
        val expectedDate = fixedDate.plusDays(20).plusYears(4)

        new TestDateService(true, 4, 20).getCurrentDate shouldBe expectedDate
      }
    }
    "return a LocalDate of the current date, but three years and 25 days in the past" when {
      "the TimeMachine is on, and we have set the TimeMachine to place us on the 27th of August" in {
        val expectedDate = fixedDate.plusDays(-25).plusYears(-3)

        new TestDateService(true, -3, -25).getCurrentDate shouldBe expectedDate
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
        baseTestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2020, 4, 5)
      }
      "business start date is 05/04/2020 (last day of accounting period)" in {
        val businessStartDate = LocalDate.of(2020, 4, 5)
        baseTestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2020, 4, 5)
      }
    }
    "return 2021-04-05 (business start year + 1)" when {
      "business start date is 06/04/2020" in {
        val businessStartDate = LocalDate.of(2020, 4, 6)
        baseTestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2021, 4, 5)
      }
      "business start date is 23/07/2020" in {
        val businessStartDate = LocalDate.of(2020, 7, 23)
        baseTestDateService.getAccountingPeriodEndDate(businessStartDate) shouldBe LocalDate.of(2021, 4, 5)
      }
    }
  }

  "isWithin30Days" should {
    val f = fixture(fixedDate)
    "return false" when {
      "date is more than 30 days in the future" in {
        f.fakeTestDateService.isWithin30Days(LocalDate.of(2024, 1, 16)) shouldBe false
      }
    }
    "return true" when {
      "date is within 30 days" in {
        f.fakeTestDateService.isWithin30Days(LocalDate.of(2024, 1, 1)) shouldBe true
      }
      "date is same as current date" in {
        f.fakeTestDateService.isWithin30Days(fixedDate) shouldBe true
      }
    }
  }
}
