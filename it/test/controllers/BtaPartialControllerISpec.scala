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

package controllers

import enums.MTDIndividual
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponse

class BtaPartialControllerISpec extends ControllerISpecHelper {

  val path = "/partial"

  s"GET $path" when {
    "the user is an authenticated individual" should {
      "display the bta partial with the correct information" in {
        stubAuthorised(MTDIndividual)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        val result = buildGETMTDClient(path).futureValue

        result should have(
          httpStatus(OK)
        )
      }
    }
    testAuthFailures(path, MTDIndividual)
  }
}
