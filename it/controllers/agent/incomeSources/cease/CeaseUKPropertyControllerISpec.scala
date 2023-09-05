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
import enums.IncomeSourceJourney.UkProperty
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuthStub, IncomeTaxViewChangeStub}
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponse

class CeaseUKPropertyControllerISpec extends ComponentSpecBase with FeatureSwitching {
  val showUKPropertyEndDateControllerUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, UkProperty.key).url
  val showCeaseUKPropertyControllerUrl: String = controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url
  val radioErrorMessage: String = messagesAPI("incomeSources.ceaseUKProperty.radioError")
  val radioLabelMessage: String = messagesAPI("incomeSources.ceaseUKProperty.radioLabel")
  val buttonLabel: String = messagesAPI("base.continue")
  val pageTitleMsgKey = "incomeSources.ceaseUKProperty.heading"

  s"calling GET ${showCeaseUKPropertyControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "user is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = businessAndPropertyResponse
        )
        When(s"I call GET ${showCeaseUKPropertyControllerUrl}")
        val res: WSResponse = IncomeTaxViewChangeFrontend.getCeaseUKProperty(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.ceaseUKProperty.heading"),
          elementTextBySelector("label")(radioLabelMessage),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }

  s"calling POST ${controllers.incomeSources.cease.routes.CeaseUKPropertyController.submit.url}" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        AuthStub.stubAuthorisedAgent()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(Some("true"), clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showUKPropertyEndDateControllerUrl)
        )
      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        AuthStub.stubAuthorisedAgent()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(None, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-uk-property-declaration-error")(messagesAPI("base.error-prefix") + " " + radioErrorMessage)
        )
      }
    }
  }


}
