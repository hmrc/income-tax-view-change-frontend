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
import assets.BusinessDetailsTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.PropertyDetailsTestConstants._
import assets.ReportDeadlinesTestConstants._
import mocks.connectors.{MockBusinessDetailsConnector, MockPropertyDetailsConnector}
import mocks.services.MockReportDeadlinesService
import models.{IncomeSourcesError, NoPropertyIncomeDetails}
import utils.TestSupport


class IncomeSourceDetailsServiceSpec extends TestSupport with MockBusinessDetailsConnector with MockPropertyDetailsConnector with MockReportDeadlinesService {

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService(mockBusinessDetailsConnector, mockPropertysDetailsConnector, mockReportDeadlinesService)

  "The IncomeSourceDetailsService .getIncomeSourceDetails method" when {
    "a result with both business and property details is returned" should {
      "return an IncomeSourceDetailsModel with business and property options" in {
        setupMockBusinesslistResult(testNino)(multipleBusinessesSuccessModel)
        setupMockPropertyDetailsResult(testNino)(propertySuccessModel)
        setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)
        setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId2)(obligationsDataSuccessModel)
        setupMockPropertyReportDeadlinesResult(testNino)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe bothIncomeSourceSuccessMisalignedTaxYear
      }
    }
    "a result with just business details is returned" should {
      "return an IncomeSourceDetailsModel with just a business option" in {
        setupMockBusinesslistResult(testNino)(businessesSuccessModel)
        setupMockPropertyDetailsResult(testNino)(NoPropertyIncomeDetails)
        setupMockBusinessReportDeadlinesResult(testNino, testSelfEmploymentId)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe businessIncomeSourceSuccess
      }
    }
    "a result with just property details is returned" should {
      "return an IncomeSourceDetailsModel with just a property option" in {
        setupMockBusinesslistResult(testNino)(noBusinessDetails)
        setupMockPropertyDetailsResult(testNino)(propertySuccessModel)
        setupMockPropertyReportDeadlinesResult(testNino)(obligationsDataSuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe propertyIncomeSourceSuccess
      }
    }
    "a result with no income source details is returned" should {
      "return an IncomeSourceDetailsModel with no options" in {
        setupMockBusinesslistResult(testNino)(noBusinessDetails)
        setupMockPropertyDetailsResult(testNino)(NoPropertyIncomeDetails)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe noIncomeSourceSuccess
      }
    }
    "a result where the Business Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockBusinesslistResult(testNino)(businessErrorModel)
        setupMockPropertyDetailsResult(testNino)(NoPropertyIncomeDetails)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe IncomeSourcesError
      }
    }
    "a result where the Property Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockBusinesslistResult(testNino)(noBusinessDetails)
        setupMockPropertyDetailsResult(testNino)(propertyErrorModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe IncomeSourcesError
      }
    }
    "a result where Business and Property Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockBusinesslistResult(testNino)(businessErrorModel)
        setupMockPropertyDetailsResult(testNino)(propertyErrorModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe IncomeSourcesError
      }
    }
  }

  "The IncomeSourceDetailsService .getBusinessDetails method" when {
    "a self-employment-ID is passed through" should {
      "return the corresponding BusinessModel" in {
        setupMockBusinesslistResult(testNino)(businessesSuccessModel)

        await(TestIncomeSourceDetailsService.getBusinessDetails(testNino, 0)) shouldBe Right(Some((businessesSuccessModel.businesses.head, 0)))
      }
    }

    "an error is returned from the businessDetailsConnector" should {
      "return a BusinessDetailsErrorModel" in {
        setupMockBusinesslistResult(testNino)(businessErrorModel)

        await(TestIncomeSourceDetailsService.getBusinessDetails(testNino, 0)) shouldBe Left(businessErrorModel)
      }
    }
  }
}
