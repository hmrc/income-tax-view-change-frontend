/*
 * Copyright 2018 HM Revenue & Customs
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


import assets.TestConstants
import assets.TestConstants.ReportDeadlines._
import assets.TestConstants._
import mocks.connectors.{MockBusinessEOPSDeadlinesConnector, MockBusinessReportDeadlinesConnector, MockPropertyEOPSDeadlinesConnector, MockPropertyReportDeadlinesConnector}
import models._
import utils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockBusinessReportDeadlinesConnector
                                                     with MockPropertyReportDeadlinesConnector
                                                     with MockBusinessEOPSDeadlinesConnector
                                                     with MockPropertyEOPSDeadlinesConnector{

  object TestReportDeadlinesService extends ReportDeadlinesService(mockBusinessObligationDataConnector, mockPropertyObligationDataConnector, mockBusinessEOPSDeadlinesConnector, mockPropertyEOPSDeadlinesConnector)

  "The ReportDeadlinesService.getNextObligation method" when {

    "a successful single business" which {

      "has a valid list of obligations returned from the connector" should {

        "return a valid list of obligations" in {
          setupMockObligation(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)
          await(TestReportDeadlinesService.getBusinessReportDeadlines(testNino, testSelfEmploymentId)) shouldBe obligationsDataSuccessModel
        }
      }
    }
    "does not have a valid list of obligations returned from the connector" should {

      "return a valid list of obligations" in {
        setupMockObligation(testNino, testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getBusinessReportDeadlines(testNino, testSelfEmploymentId)) shouldBe obligationsDataErrorModel
      }
    }
  }

  "The ReportDeadlinesService.getPropertyReportDeadlines method" when {

    "a single list of obligations is returned from the connector" should {

      "return a valid list of obligations" in {

        setupMockPropertyObligation(testNino)(TestConstants.ReportDeadlines.obligationsDataSuccessModel)

        val successfulReportDeadlinesResponse =
          ReportDeadlinesModel(
            List(
              ReportDeadlineModel(
                start = "2017-04-01",
                end = "2017-6-30",
                due = "2017-7-31",
                met = true
              ),
              ReportDeadlineModel(
                start = "2017-7-1",
                end = "2017-9-30",
                due = "2017-10-30",
                met = false
              ),
              ReportDeadlineModel(
                start = "2017-7-1",
                end = "2017-9-30",
                due = "2017-10-31",
                met = false
              )
            )
          )
        await(TestReportDeadlinesService.getPropertyReportDeadlines(testNino)) shouldBe successfulReportDeadlinesResponse
      }
    }
  }


  "The ReportDeadlinesService.getBusinessEOPSDeadlines method" when {

    "a successful single business" which {

      "a single list of obligations is returned from the connector" should {

        "return a valid list of obligations" in {
          setupMockBusinessEOPSDeadline(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)
          await(TestReportDeadlinesService.getBusinessEOPSDeadline(testNino, testSelfEmploymentId)) shouldBe obligationsDataSuccessModel
        }
      }
    }
    "does not have a valid list of obligations returned from the connector" should {

      "return a valid list of obligations" in {
        setupMockBusinessEOPSDeadline(testNino, testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getBusinessEOPSDeadline(testNino, testSelfEmploymentId)) shouldBe obligationsDataErrorModel
      }
    }
  }

  "The ReportDeadlinesService.getPropertyEOPSDeadlines method" when {

    "a single list of obligations is returned from the connector" should {

      "return a valid list of obligations" in {
        setupMockPropertyEOPSDeadline(testNino)(obligationsDataSuccessModel)
        await(TestReportDeadlinesService.getPropertyEOPSDeadline(testNino)) shouldBe obligationsDataSuccessModel
      }
    }

    "does not have a valid list of obligations returned from the connector" should {

      "return a valid list of obligations" in {
        setupMockPropertyEOPSDeadline(testNino)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getPropertyEOPSDeadline(testNino)) shouldBe obligationsDataErrorModel
      }
    }
  }
}
