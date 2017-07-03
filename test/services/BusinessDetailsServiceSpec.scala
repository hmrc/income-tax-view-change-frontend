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
import assets.TestConstants._
import mocks.connectors.MockBusinessDetailsConnector
import utils.TestSupport


class BusinessDetailsServiceSpec extends TestSupport with MockBusinessDetailsConnector {

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService(mockBusinessDetailsConnector)

  "The IncomeSourceDetailsService.getBusinessDetails method" when {

    "a successful response is returned from the connector" should {

      "return the expected BusinessDetails success model" in {
        setupMockBusinesslistResult(testNino)(businessesSuccessModel)
        await(TestIncomeSourceDetailsService.getBusinessDetails(testNino)) shouldBe businessesSuccessModel
      }
    }

    "a non-successful response is returned from the connector" should {

      "return a Business Details Error Model" in {
        setupMockBusinesslistResult(testNino)(businessErrorModel)
        await(TestIncomeSourceDetailsService.getBusinessDetails(testNino)) shouldBe businessErrorModel
      }
    }
  }
}
