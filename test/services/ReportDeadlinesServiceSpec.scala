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

import assets.BaseTestConstants._
import assets.ReportDeadlinesTestConstants._
import mocks.connectors.MockReportDeadlinesConnector
import utils.TestSupport

class ReportDeadlinesServiceSpec extends TestSupport with MockReportDeadlinesConnector {

  object TestReportDeadlinesService extends ReportDeadlinesService(mockReportDeadlinesConnector)

  "The ReportDeadlinesService.getReportDeadlines method" when {

    "a valid list of Report Deadlines is returned from the connector" should {

      "return a valid list of Report Deadlines" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataSuccessModel)
        await(TestReportDeadlinesService.getReportDeadlines(testSelfEmploymentId)) shouldBe obligationsDataSuccessModel
      }
    }

    "an error is returned from the connector" should {

      "return the error" in {
        setupMockReportDeadlines(testSelfEmploymentId)(obligationsDataErrorModel)
        await(TestReportDeadlinesService.getReportDeadlines(testSelfEmploymentId)) shouldBe obligationsDataErrorModel
      }
    }
  }
}
