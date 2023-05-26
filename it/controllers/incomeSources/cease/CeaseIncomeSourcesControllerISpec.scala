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

package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyAndCeasedBusinessResponse, multipleBusinessesAndPropertyResponse, multipleBusinessesAndUkProperty}

class CeaseIncomeSourcesControllerISpec extends ComponentSpecBase {

  val showIndividualCeaseIncomeSourceControllerUrl: String = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.show().url
  val showAgentCeaseIncomeSourceControllerUrl: String = controllers.incomeSources.cease.routes.CeaseIncomeSourceController.showAgent().url
  val pageTitleMsgKey = "cease-income-sources.heading"


  s"calling GET ${showIndividualCeaseIncomeSourceControllerUrl}" should {
    "render the Cease Income Source page for an Individual" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses, a uk property, a foreign property, and a ceased business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
        When(s"I call GET ${showIndividualCeaseIncomeSourceControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCeaseIncomeSourcesIndividual
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextByID("continue-button")(buttonLabel)

        )
      }
    }
  }

}
