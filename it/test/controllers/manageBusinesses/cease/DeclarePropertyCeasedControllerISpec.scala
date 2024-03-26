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

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
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

  val testId = Some("test-id")

  val showUKPropertyEndDateUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, UkProperty).url
  val showDeclareUKPropertyCeasedUrl: String = controllers.manageBusinesses.cease.routes.DeclarePropertyCeasedController.show(None, UkProperty).url
  val showForeignPropertyEndDateUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty).url
  val showDeclareForeignPropertyCeasedUrl: String = controllers.manageBusinesses.cease.routes.DeclarePropertyCeasedController.show(None, ForeignProperty).url
  val showSelfEmploymentEndDateUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(testId, SelfEmployment).url
  val showDeclareSelfEmploymentCeasedUrl: String = controllers.manageBusinesses.cease.routes.DeclarePropertyCeasedController.show(testId, SelfEmployment).url

  val checkboxErrorMessageUK: String = messagesAPI("incomeSources.cease.UK.checkboxError")
  val checkboxLabelMessageUK: String = messagesAPI("incomeSources.cease.UK.checkboxLabel")
  val checkboxErrorMessageFP: String = messagesAPI("incomeSources.cease.FP.checkboxError")
  val checkboxLabelMessageFP: String = messagesAPI("incomeSources.cease.FP.checkboxLabel")
  val pageTitleMsgKeyFP = "incomeSources.cease.FP.heading"
  val pageTitleMsgKeyUK = "incomeSources.cease.UK.heading"
  val buttonLabel: String = messagesAPI("base.continue")

  val stringTrue: String = "true"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"calling GET ${showDeclareUKPropertyCeasedUrl}" should {
    "render the Cease UK Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET ${showDeclareUKPropertyCeasedUrl}")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.getCeaseUKProperty
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

  s"calling POST ${controllers.manageBusinesses.cease.routes.DeclarePropertyCeasedController.submit(None, UkProperty).url}" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postCeaseUKProperty(Some("true"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showUKPropertyEndDateUrl)
        )

        sessionService.getMongoKey(ceasePropertyDeclare, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(stringTrue))

      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postCeaseUKProperty(None)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-property-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageUK)
        )
      }
    }
  }

  s"calling GET $showDeclareForeignPropertyCeasedUrl" should {
    "render the Cease Foreign Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET $showDeclareForeignPropertyCeasedUrl")
        val res = IncomeTaxViewChangeFrontendManageBusinesses.getCeaseForeignProperty
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

  s"calling POST ${controllers.manageBusinesses.cease.routes.DeclarePropertyCeasedController.submit(None, ForeignProperty).url}" should {
    "redirect to showForeignPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postCeaseForeignProperty(Some("true"))
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showForeignPropertyEndDateUrl)
        )

        sessionService.getMongoKey(ceasePropertyDeclare, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(stringTrue))

      }
      "form is filled incorrectly" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postCeaseForeignProperty(None)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-property-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageFP)
        )
      }
    }
  }
}
