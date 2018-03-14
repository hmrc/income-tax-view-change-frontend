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


import assets.TestConstants.ReportDeadlines._
import assets.TestConstants._
import mocks.connectors.{MockBusinessEOPSDeadlinesConnector, MockBusinessReportDeadlinesConnector, MockPropertyEOPSDeadlinesConnector, MockPropertyReportDeadlinesConnector}
import models.reportDeadlines.ReportDeadlinesModel
import utils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockBusinessReportDeadlinesConnector
                                                     with MockPropertyReportDeadlinesConnector
                                                     with MockBusinessEOPSDeadlinesConnector
                                                     with MockPropertyEOPSDeadlinesConnector{

  object TestReportDeadlinesService extends ReportDeadlinesService(mockBusinessObligationDataConnector, mockPropertyObligationDataConnector, mockBusinessEOPSDeadlinesConnector, mockPropertyEOPSDeadlinesConnector)

  "The ReportDeadlinesService.getNextObligation method" when {

    "a successful single business" which {

      "has a valid list of obligations and EOPS returned from the connector" should {

        "return a valid list of obligations" in {
          setupMockObligation(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)
          setupMockBusinessEOPSDeadline(testNino, testSelfEmploymentId)(obligationsEOPSDataSuccessModel)

          await(TestReportDeadlinesService.getBusinessReportDeadlines(testNino, testSelfEmploymentId)) shouldBe
            ReportDeadlinesModel(obligationsDataSuccessModel.obligations ++ obligationsEOPSDataSuccessModel.obligations)
        }
      }

      "has a valid list of obligations and but does not have a valid list of EOPS returned from the connector" should {

        "return a valid list of obligations" in {
          setupMockObligation(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)
          setupMockBusinessEOPSDeadline(testNino, testSelfEmploymentId)(obligationsDataErrorModel)

          await(TestReportDeadlinesService.getBusinessReportDeadlines(testNino, testSelfEmploymentId)) shouldBe obligationsDataSuccessModel
        }
      }

      "does not have a valid list of obligations but does have a valid list of EOPS returned from the connector" should {

        "return an error" in {
          setupMockObligation(testNino, testSelfEmploymentId)(obligationsDataErrorModel)
          setupMockBusinessEOPSDeadline(testNino, testSelfEmploymentId)(obligationsEOPSDataSuccessModel)

          await(TestReportDeadlinesService.getBusinessReportDeadlines(testNino, testSelfEmploymentId)) shouldBe obligationsDataErrorModel
        }
      }

      "does not have a valid list of either obligations or EOPS returned from the connector" should {

        "return an error" in {
          setupMockObligation(testNino, testSelfEmploymentId)(obligationsDataErrorModel)
          setupMockBusinessEOPSDeadline(testNino, testSelfEmploymentId)(obligationsDataErrorModel)

          await(TestReportDeadlinesService.getBusinessReportDeadlines(testNino, testSelfEmploymentId)) shouldBe obligationsDataErrorModel
        }
      }
    }
  }

  "The ReportDeadlinesService.getPropertyReportDeadlines method" when {

    "a valid list of obligations and EOPS is returned from the connector" should {

      "return a valid list of obligations" in {

        setupMockPropertyObligation(testNino)(obligationsDataSuccessModel)
        setupMockPropertyEOPSDeadline(testNino)(obligationsEOPSDataSuccessModel)

        await(TestReportDeadlinesService.getPropertyReportDeadlines(testNino)) shouldBe
          ReportDeadlinesModel(obligationsDataSuccessModel.obligations ++ obligationsEOPSDataSuccessModel.obligations)
      }
    }

    "a valid list of obligations but no EOPS are returned from the connector" should {

      "return a valid list of obligations" in {

        setupMockPropertyObligation(testNino)(obligationsDataSuccessModel)
        setupMockPropertyEOPSDeadline(testNino)(obligationsDataErrorModel)

        await(TestReportDeadlinesService.getPropertyReportDeadlines(testNino)) shouldBe obligationsDataSuccessModel
      }
    }

    "does not have a valid list of obligations but does return a list EOPS are returned from the connector" should {

      "return an error" in {

        setupMockPropertyObligation(testNino)(obligationsDataErrorModel)
        setupMockPropertyEOPSDeadline(testNino)(obligationsEOPSDataSuccessModel)

        await(TestReportDeadlinesService.getPropertyReportDeadlines(testNino)) shouldBe obligationsDataErrorModel
      }
    }

    "does not have a valid list of obligations or EOPS returned from the connector" should {

      "return an error" in {

        setupMockPropertyObligation(testNino)(obligationsDataErrorModel)
        setupMockPropertyEOPSDeadline(testNino)(obligationsDataErrorModel)

        await(TestReportDeadlinesService.getPropertyReportDeadlines(testNino)) shouldBe obligationsDataErrorModel
      }
    }
  }


}
