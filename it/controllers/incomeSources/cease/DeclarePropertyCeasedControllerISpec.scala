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
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.CeaseIncomeSourceData.ceasePropertyDeclare
import models.incomeSourceDetails.UIJourneySessionData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

class DeclarePropertyCeasedControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val showUKPropertyEndDateControllerUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, UkProperty).url
  val showDeclareUKPropertyCeasedControllerUrl: String = controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.show(UkProperty).url
  val showForeignPropertyEndDateControllerUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty).url
  val showDeclareForeignPropertyCeasedControllerUrl: String = controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.show(ForeignProperty).url
  val checkboxErrorMessageUK: String = messagesAPI("incomeSources.cease.UK.property.checkboxError")
  val checkboxLabelMessageUK: String = messagesAPI("incomeSources.cease.UK.property.checkboxLabel")
  val checkboxErrorMessageFP: String = messagesAPI("incomeSources.cease.FP.property.checkboxError")
  val checkboxLabelMessageFP: String = messagesAPI("incomeSources.cease.FP.property.checkboxLabel")
  val pageTitleMsgKeyFP = "incomeSources.cease.FP.property.heading"
  val pageTitleMsgKeyUK = "incomeSources.cease.UK.property.heading"
  val buttonLabel: String = messagesAPI("base.continue")

  val stringTrue: String = "true"
  val stringFalse: String = "false"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.deleteOne(UIJourneySessionData(testSessionId, "CEASE-SE")))
    await(repository.deleteOne(UIJourneySessionData(testSessionId, "CEASE-UK")))
    await(repository.deleteOne(UIJourneySessionData(testSessionId, "CEASE-FP")))
  }

  s"calling GET ${showDeclareUKPropertyCeasedControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET ${showDeclareUKPropertyCeasedControllerUrl}")
        val res = IncomeTaxViewChangeFrontend.getCeaseUKProperty
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKeyUK),
          elementTextBySelector("label")(checkboxLabelMessageUK),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }

  s"calling POST ${controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(UkProperty).url}" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(Some("true"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showUKPropertyEndDateControllerUrl)
        )

        sessionService.getMongoKey(ceasePropertyDeclare, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(stringTrue))

      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(None)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-property-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageUK)
        )

        sessionService.getMongoKey(ceasePropertyDeclare, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(stringFalse))

      }
    }
  }

  s"calling GET $showDeclareForeignPropertyCeasedControllerUrl" should {
    "render the Cease Foreign Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET $showDeclareForeignPropertyCeasedControllerUrl")
        val res = IncomeTaxViewChangeFrontend.getCeaseForeignProperty
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKeyFP),
          elementTextBySelector("label")(checkboxLabelMessageFP),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }

  s"calling POST ${controllers.incomeSources.cease.routes.DeclarePropertyCeasedController.submit(ForeignProperty).url}" should {
    "redirect to showForeignPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(Some("true"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showForeignPropertyEndDateControllerUrl)
        )

        sessionService.getMongoKey(ceasePropertyDeclare, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(stringTrue))

      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(None)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-property-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageFP)
        )

        sessionService.getMongoKey(ceasePropertyDeclare, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(stringFalse))

      }
    }
  }
}
