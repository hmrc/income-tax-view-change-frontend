/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.TestConstants.BusinessDetails._
import mocks.connectors.{MockBusinessDetailsConnector, MockPropertyDetailsConnector}
import utils.TestSupport
import assets.TestConstants._
import assets.TestConstants.PropertyDetails._
import assets.TestConstants.IncomeSourceDetails._
class IncomeSourceDetailsServiceSpec extends TestSupport with MockBusinessDetailsConnector with MockPropertyDetailsConnector{

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService(mockBusinessDetailsConnector, mockPropertysDetailsConnector)

  "The IncomeSourceDetailsService .getIncomeSourceDetails method" when {
    "a result with both business and property details is returned" should {
      "return an IncomeSourceDetailsModel with business and property options" in {
        setupMockBusinesslistResult(testNino)(businessesSuccessModel)
        setupMockPropertyDetailsResult(testNino)(propertySuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe bothIncomeSourceSuccessMisalignedTaxYear
      }
    }
    "a result with just business details is returned" should {
      "return an IncomeSourceDetailsModel with just a business option" in {
        setupMockBusinesslistResult(testNino)(businessesSuccessModel)
        setupMockPropertyDetailsResult(testNino)(propertyErrorModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe businessIncomeSourceSuccess
      }
    }
    "a result with just property details is returned" should {
      "return an IncomeSourceDetailsModel with just a property option" in {
        setupMockBusinesslistResult(testNino)(businessErrorModel)
        setupMockPropertyDetailsResult(testNino)(propertySuccessModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe propertyIncomeSourceSuccess
      }
    }
    "a result with no income source details is returned" should {
      "return an IncomeSourceDetailsModel with no options" in {
        setupMockBusinesslistResult(testNino)(businessErrorModel)
        setupMockPropertyDetailsResult(testNino)(propertyErrorModel)

        await(TestIncomeSourceDetailsService.getIncomeSourceDetails(testNino)) shouldBe noIncomeSourceSuccess
      }
    }
  }
}
