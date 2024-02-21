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

package controllers.manageBusinesses.cease

import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

class IncomeSourceNotCeasedControllerISpec extends ComponentSpecBase {

  "The IncomeSourceNotCeasedController.show - Individual" should {
    "200 OK" when {
      "user is authorised with a business income source type in the request" in {
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(uri = "/manage-your-businesses/cease/error-business-not-ceased")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("standardError.heading", isErrorPage = true)
        )
      }
      "user is authorised with a UK property income source type in the request" in {
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(uri = "/manage-your-businesses/cease/error-uk-property-not-ceased")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("standardError.heading", isErrorPage = true)
        )
      }
      "user is authorised with a foreign property income source type in the request" in {
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(uri = "/manage-your-businesses/cease/error-foreign-property-not-ceased")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("standardError.heading", isErrorPage = true)
        )
      }
    }
  }
}
