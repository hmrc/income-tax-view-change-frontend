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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

class CeaseForeignPropertyControllerISpec extends ComponentSpecBase {
  val showForeignPropertyEndDateControllerUrl: String = controllers.incomeSources.cease.routes.ForeignPropertyEndDateController.show().url
  val showCeaseForeignPropertyControllerUrl: String = controllers.incomeSources.cease.routes.CeaseForeignPropertyController.show().url
  val checkboxErrorMessage: String = messagesAPI("incomeSources.ceaseForeignProperty.checkboxError")
  val checkboxLabelMessage: String = messagesAPI("incomeSources.ceaseForeignProperty.checkboxLabel")
  val buttonLabel: String = messagesAPI("base.continue")
  val pageTitleMsgKey = "incomeSources.ceaseForeignProperty.heading"

  s"calling GET $showCeaseForeignPropertyControllerUrl" should {
    "render the Cease Foreign Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET $showCeaseForeignPropertyControllerUrl")
        val res = IncomeTaxViewChangeFrontend.getCeaseForeignProperty
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextBySelector("label")(checkboxLabelMessage),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }

  s"calling POST ${controllers.incomeSources.cease.routes.CeaseForeignPropertyController.submit.url}" should {
    "redirect to showForeignPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(Some("true"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showForeignPropertyEndDateControllerUrl)
        )
      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(None)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-foreign-property-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessage)
        )
      }
    }
  }

}
