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
import assets.IncomeSourcesTestConstants._
import assets.BusinessDetailsTestConstants._
import assets.ReportDeadlinesTestConstants.obligationsDataSuccessModel
import audit.mocks.MockAuditingService
import mocks.connectors.MockIncomeSourceDetailsConnector
import mocks.services.MockReportDeadlinesService
import models.incomeSourcesWithDeadlines.IncomeSourcesError
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
        setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId2)(obligationsDataSuccessModel)
        setupMockPropertyReportDeadlinesResult(testNino)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testMtditid, testNino)) shouldBe bothIncomeSourceSuccessMisalignedTaxYear
      }
    }

    "a result with just business details is returned" should {
      "return an IncomeSourceDetailsModel with just a business option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(singleBusinessIncome)
        setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testMtditid, testNino)) shouldBe businessIncomeSourceSuccess
      }
    }

    "a result with just property details is returned" should {
      "return an IncomeSourceDetailsModel with just a property option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(propertyIncomeOnly)
        setupMockPropertyReportDeadlinesResult(testNino)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testMtditid, testNino)) shouldBe propertyIncomeSourceSuccess
      }
    }

    "a result with no income source details is returned" should {
      "return an IncomeSourceDetailsModel with no options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(noIncomeDetails)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testMtditid, testNino)) shouldBe noIncomeSourceSuccess
      }
    }

    "a result where the Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(errorResponse)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testMtditid, testNino)) shouldBe IncomeSourcesError
      }
    }
  }

  "The IncomeSourceDetailsService .getBusinessDetails method" when {
    "a self-employment-ID is passed through" should {
      "return the corresponding BusinessModel" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(singleBusinessIncome)

        await(TestIncomeSourceDetailsService.getBusinessDetails(testMtditid, testNino, 0)) shouldBe Right(Some((business1, 0)))
      }
    }

    "an error is returned from the Income Source Details connector" should {
      "return a ErrorModel" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino)(errorResponse)

        await(TestIncomeSourceDetailsService.getBusinessDetails(testMtditid, testNino, 0)) shouldBe Left(businessErrorModel)
      }
    }
  }
}
