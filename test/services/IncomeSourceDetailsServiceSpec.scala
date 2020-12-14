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

import assets.BaseTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import audit.mocks.MockAuditingService
import javax.inject.Inject
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.services.MockReportDeadlinesService
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils

//scalastyle:off
class IncomeSourceDetailsServiceSpec @Inject() (val languageUtils: LanguageUtils) extends TestSupport with MockIncomeTaxViewChangeConnector with MockReportDeadlinesService with MockAuditingService {

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService(mockIncomeTaxViewChangeConnector)

  "The IncomeSourceDetailsService.getIncomeSourceDetails method" when {

    "a result with both business and property details is returned" should {

      "return an IncomeSourceDetailsModel with business and property options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(businessesAndPropertyIncome)
        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe businessesAndPropertyIncome
      }
    }

    "a result with just business details is returned" should {
      "return an IncomeSourceDetailsModel with just a business option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(singleBusinessIncome)
        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe singleBusinessIncome
      }
    }

    "a result with just property details is returned" should {
      "return an IncomeSourceDetailsModel with just a property option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(propertyIncomeOnly)
        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe propertyIncomeOnly
      }
    }

    "a result with no income source details is returned" should {
      "return an IncomeSourceDetailsModel with no options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(noIncomeDetails)
        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe noIncomeDetails
      }
    }

    "a result where the Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(errorResponse)
        await(TestIncomeSourceDetailsService.getIncomeSourceDetails()) shouldBe errorResponse
      }
    }
  }
}
