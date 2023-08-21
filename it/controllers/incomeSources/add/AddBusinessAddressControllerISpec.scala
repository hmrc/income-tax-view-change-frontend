/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddBusinessAddressControllerISpec extends ComponentSpecBase {

  val changeBusinessAddressShowUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = true).url
  val businessAddressShowUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url

  s"calling GET $businessAddressShowUrl" should {
    "render the add business address page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $businessAddressShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessAddress
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  s"calling GET $changeBusinessAddressShowUrl" should {
    "render the change business address page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $changeBusinessAddressShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddChangeBusinessAddress
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }
}
