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
import enums.IncomeSourceJourney.UkProperty
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

class CeaseUKPropertyControllerISpec extends ComponentSpecBase {
  val showUKPropertyEndDateControllerUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, UkProperty).url
  val showCeaseUKPropertyControllerUrl: String = controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url
  val radioErrorMessage: String = messagesAPI("incomeSources.ceaseUKProperty.radioError")
  val radioLabelMessage: String = messagesAPI("incomeSources.ceaseUKProperty.radioLabel")
  val buttonLabel: String = messagesAPI("base.continue")
  val pageTitleMsgKey = "incomeSources.ceaseUKProperty.heading"

  s"calling GET ${showCeaseUKPropertyControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET ${showCeaseUKPropertyControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCeaseUKProperty
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
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
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(Some("true"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showUKPropertyEndDateControllerUrl)
        )
      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(None)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-uk-property-declaration-error")(messagesAPI("base.error-prefix") + " " + radioErrorMessage)
        )
      }
    }
  }

}
