/*
 * Copyright 2021 HM Revenue & Customs
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

import testConstants.BaseTestConstants.testMtditid
import testConstants.NinoLookupTestConstants._
import mocks.connectors.MockIncomeTaxViewChangeConnector
import testUtils.TestSupport

class NinoLookupServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector {

  object TestNinoLookupService extends NinoLookupService(mockIncomeTaxViewChangeConnector)


  "The NinoLookupService.getNino method" when {

    "a successful NINO response is returned from the connector" should {

      "return a valid NINO model" in {

        setupNinoLookupResponse(testMtditid)(testNinoModel)

        TestNinoLookupService.getNino(testMtditid).futureValue shouldBe testNinoModel
      }
    }
    "an error model is returned from the connector" should {
      "return a Nino Error model" in {
        setupNinoLookupResponse(testMtditid)(testNinoErrorModel)

        TestNinoLookupService.getNino(testMtditid).futureValue shouldBe testNinoErrorModel
      }
    }
  }
}
