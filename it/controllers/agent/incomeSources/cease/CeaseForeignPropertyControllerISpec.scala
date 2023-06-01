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

package controllers.agent.incomeSources.cease

import config.featureswitch.{FeatureSwitching, IncomeSources}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuthStub, IncomeTaxViewChangeStub}
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponse

class CeaseForeignPropertyControllerISpec extends ComponentSpecBase with FeatureSwitching {
  val showDateForeignPropertyCeasedControllerUrl: String = controllers.incomeSources.cease.routes.DateForeignPropertyCeasedController.showAgent().url
  val showCeaseForeignPropertyControllerUrl: String = controllers.incomeSources.cease.routes.CeaseForeignPropertyController.showAgent().url
  val checkboxErrorMessage: String = messagesAPI("incomeSources.ceaseForeignProperty.checkboxError")
  val checkboxLabelMessage: String = messagesAPI("incomeSources.ceaseForeignProperty.checkboxLabel")
  val buttonLabel: String = messagesAPI("base.continue")
  val pageTitleMsgKey = "incomeSources.ceaseForeignProperty.heading"

  s"calling GET $showCeaseForeignPropertyControllerUrl" should {
    "render the Cease Foreign Property Page" when {
      "user is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = businessAndPropertyResponse
        )
        When(s"I call GET $showCeaseForeignPropertyControllerUrl")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getCeaseForeignProperty(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.ceaseForeignProperty.heading"),
          elementTextBySelector("label")(checkboxLabelMessage),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }

  s"calling POST ${controllers.incomeSources.cease.routes.CeaseForeignPropertyController.submit.url}" should {
    "redirect to showDateForeignPropertyCeasedControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        AuthStub.stubAuthorisedAgent()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(Some("true"), clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showDateForeignPropertyCeasedControllerUrl)
        )
      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        AuthStub.stubAuthorisedAgent()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(None, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-foreign-property-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessage)
        )
      }
    }
  }


}
