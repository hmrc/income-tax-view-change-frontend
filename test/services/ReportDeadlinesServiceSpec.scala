/*
 * Copyright 2020 HM Revenue & Customs
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

import java.time.LocalDate

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants.{obligationsDataSuccessModel => _}
import assets.IncomeSourceDetailsTestConstants._
import assets.ReportDeadlinesTestConstants._
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.reportDeadlines.ObligationsModel
import testUtils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  class Setup extends ReportDeadlinesService(mockIncomeTaxViewChangeConnector)

  "getNextDeadlineDueDate" should {
    "return the next report deadline due date" when {
      "there are income sources from property, business with crystallisation" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsAllDeadlinesSuccessModel)

        await(getNextDeadlineDueDate(businessAndPropertyAligned)) shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just one report deadline from an income source" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsPropertyOnlySuccessModel)

        await(getNextDeadlineDueDate(propertyIncomeOnly)) shouldBe LocalDate.of(2017, 10, 1)
      }
      "there is just a crystallisation deadline" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsCrystallisedOnlySuccessModel)

        await(getNextDeadlineDueDate(noIncomeDetails)) shouldBe LocalDate.of(2017, 10, 31)
      }

      "there are no deadlines available" in new Setup {
        setupMockReportDeadlines(testNino)(emptyObligationsSuccessModel)

        the[Exception] thrownBy await(getNextDeadlineDueDate(noIncomeDetails)) should have message "Unexpected Exception getting next deadline due"
      }

      "the report deadlines returned back an error model" in new Setup {
        setupMockReportDeadlines(testNino)(obligationsDataErrorModel)

        the[Exception] thrownBy await(getNextDeadlineDueDate(noIncomeDetails)) should have message "Dummy Error Message"
      }
    }
  }

  "The ReportDeadlinesService.getReportDeadlines method" when {

    "a valid list of Report Deadlines is returned from the connector" should {

      "return a valid list of Report Deadlines" in {
        setupMockReportDeadlines(testNino)(ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel)))
        await(TestReportDeadlinesService.getReportDeadlines()) shouldBe ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel))
      }

      "return a valid list of previous Report Deadlines" in {
        setupMockPreviousObligations(testNino)(ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel)))
        await(TestReportDeadlinesService.getReportDeadlines(previous = true)) shouldBe ObligationsModel(Seq(reportDeadlinesDataSelfEmploymentSuccessModel))
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines()) shouldBe obligationsDataErrorModel
      }

      "return the error for previous deadlines" in {
        setupMockPreviousObligations(testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines(previous = true)) shouldBe obligationsDataErrorModel
      }
    }
  }
}
