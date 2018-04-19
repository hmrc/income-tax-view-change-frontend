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
import assets.IncomeSourceDetailsTestConstants._
import assets.IncomeSourcesWithDeadlinesTestConstants._
import assets.ReportDeadlinesTestConstants.obligationsDataSuccessModel
import audit.mocks.MockAuditingService
import mocks.connectors.MockIncomeSourceDetailsConnector
import mocks.services.MockReportDeadlinesService
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesError
import utils.TestSupport


class IncomeSourceDetailsServiceSpec extends TestSupport with MockIncomeSourceDetailsConnector with MockReportDeadlinesService with MockAuditingService {

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService(
    mockIncomeSourceDetailsConnector,
    mockReportDeadlinesService,
    mockAuditingService
  )

  "The IncomeSourceDetailsService.getIncomeSourceDetails method" when {

    "a result with both business and property details is returned" should {

      "return an IncomeSourceDetailsModel with business and property options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(businessesAndPropertyIncome)
        setupMockReportDeadlinesResult(testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockReportDeadlinesResult(testSelfEmploymentId2)(obligationsDataSuccessModel)
        setupMockReportDeadlinesResult(testPropertyIncomeId)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe bothIncomeSourceSuccessMisalignedTaxYear
      }
    }

    "a result with just business details is returned" should {
      "return an IncomeSourceDetailsModel with just a business option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(singleBusinessIncome)
        setupMockReportDeadlinesResult(testSelfEmploymentId)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe businessIncomeSourceSuccess
      }
    }

    "a result with just property details is returned" should {
      "return an IncomeSourceDetailsModel with just a property option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(propertyIncomeOnly)
        setupMockReportDeadlinesResult(testPropertyIncomeId)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe propertyIncomeSourceSuccess
      }
    }

    "a result with no income source details is returned" should {
      "return an IncomeSourceDetailsModel with no options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(noIncomeDetails)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe noIncomeSourceSuccess
      }
    }

    "a result where the Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(errorResponse)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe IncomeSourcesWithDeadlinesError
      }
    }
  }
}
