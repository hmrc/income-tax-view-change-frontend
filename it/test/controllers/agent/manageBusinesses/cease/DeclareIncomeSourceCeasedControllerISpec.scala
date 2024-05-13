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

package controllers.agent.manageBusinesses.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.CeaseIncomeSourceData.ceaseIncomeSourceDeclare
import models.incomeSourceDetails.UIJourneySessionData
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSelfEmploymentIdHashed, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse
import controllers.manageBusinesses.cease.routes._
import play.api.http.Status

class DeclareIncomeSourceCeasedControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val showUKPropertyEndDateUrl: String            = IncomeSourceEndDateController.showAgent(None, UkProperty).url
  val showDeclareUKPropertyCeasedUrl: String      = DeclareIncomeSourceCeasedController.showAgent(None, UkProperty).url
  val showForeignPropertyEndDateUrl: String       = IncomeSourceEndDateController.showAgent(None, ForeignProperty).url
  val showDeclareForeignPropertyCeasedUrl: String = DeclareIncomeSourceCeasedController.showAgent(None, ForeignProperty).url
  val showSelfEmploymentEndDateUrl: String        = IncomeSourceEndDateController.showAgent(Some(testSelfEmploymentIdHashed), SelfEmployment).url
  val showDeclareSelfEmploymentCeasedUrl: String  = DeclareIncomeSourceCeasedController.showAgent(Some(testSelfEmploymentIdHashed), SelfEmployment).url

  val submitDeclareUKPropertyCeasedUrl: String      = DeclareIncomeSourceCeasedController.submitAgent(None, UkProperty).url
  val submitDeclareForeignPropertyCeasedUrl: String = DeclareIncomeSourceCeasedController.submitAgent(None, ForeignProperty).url
  val submitDeclareSelfEmploymentCeasedUrl: String  = DeclareIncomeSourceCeasedController.submitAgent(Some(testSelfEmploymentIdHashed), SelfEmployment).url

  val checkboxErrorMessageUK: String = messagesAPI("incomeSources.cease.UK.checkboxError")
  val checkboxLabelMessageUK: String = messagesAPI("incomeSources.cease.UK.checkboxLabel")
  val checkboxErrorMessageFP: String = messagesAPI("incomeSources.cease.FP.checkboxError")
  val checkboxLabelMessageFP: String = messagesAPI("incomeSources.cease.FP.checkboxLabel")
  val checkboxErrorMessageSE: String = messagesAPI("incomeSources.cease.SE.checkboxError")
  val checkboxLabelMessageSE: String = messagesAPI("incomeSources.cease.SE.checkboxLabel")
  val pageTitleMsgKeyFP = "incomeSources.cease.FP.heading"
  val pageTitleMsgKeyUK = "incomeSources.cease.UK.heading"
  val pageTitleMsgKeySE = "incomeSources.cease.SE.heading"
  val buttonLabel: String = messagesAPI("base.continue")

  val stringTrue: String = "true"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"calling GET $showDeclareUKPropertyCeasedUrl" should {
    s"return status: ${Status.OK}: render the UK Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET $showDeclareUKPropertyCeasedUrl")
        val res = IncomeTaxViewChangeFrontend.getCeaseUKProperty(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeyUK),
          elementTextBySelector("label")(checkboxLabelMessageUK),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }
  s"calling POST $submitDeclareUKPropertyCeasedUrl" should {
    s"redirect to $showUKPropertyEndDateUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(Some("true"), clientDetailsWithConfirmation)
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showUKPropertyEndDateUrl)
        )

        sessionService.getMongoKey(ceaseIncomeSourceDeclare, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(stringTrue))

      }
    }
    s"return status: ${Status.BAD_REQUEST}" when {
      "form is filled incorrectly" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        val result = IncomeTaxViewChangeFrontend.postCeaseUKProperty(None, clientDetailsWithConfirmation)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-income-source-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageUK)
        )
      }
    }
  }

  s"calling GET $showDeclareForeignPropertyCeasedUrl" should {
    s"return status: ${Status.OK}: render the Foreign Property Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET $showDeclareForeignPropertyCeasedUrl")
        val res = IncomeTaxViewChangeFrontend.getCeaseForeignProperty(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeyFP),
          elementTextBySelector("label")(checkboxLabelMessageFP),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }
  s"calling POST $submitDeclareForeignPropertyCeasedUrl" should {
    s"return status: ${Status.SEE_OTHER}: redirect to $showForeignPropertyEndDateUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(Some("true"), clientDetailsWithConfirmation)
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showForeignPropertyEndDateUrl)
        )

        sessionService.getMongoKey(ceaseIncomeSourceDeclare, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(stringTrue))

      }
    }
    s"return status: ${Status.BAD_REQUEST}" when {
      "form is filled incorrectly" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        val result = IncomeTaxViewChangeFrontend.postCeaseForeignProperty(None, clientDetailsWithConfirmation)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-income-source-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageFP)
        )
      }
    }
  }

  s"calling GET $showDeclareSelfEmploymentCeasedUrl" should {
    s"return status: ${Status.OK}: render the Cease Sole Trader Business Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)
        When(s"I call GET $showDeclareSelfEmploymentCeasedUrl")
        val res = IncomeTaxViewChangeFrontend.getCeaseSelfEmployment(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeySE),
          elementTextBySelector("label")(checkboxLabelMessageSE),
          elementTextByID("continue-button")(buttonLabel)
        )
      }
    }
  }
  s"calling POST $submitDeclareSelfEmploymentCeasedUrl" should {
    s"return status: ${Status.SEE_OTHER}: redirect to $showSelfEmploymentEndDateUrl" when {
      "form is filled correctly" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE")))

        val result = IncomeTaxViewChangeFrontend.postCeaseSelfEmployment(Some("true"), clientDetailsWithConfirmation)
        result.status shouldBe SEE_OTHER
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(showSelfEmploymentEndDateUrl)
        )

        sessionService.getMongoKey(ceaseIncomeSourceDeclare, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(stringTrue))

      }
    }
    s"return status: ${Status.BAD_REQUEST}" when {
      "form is filled incorrectly" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE")))

        val result = IncomeTaxViewChangeFrontend.postCeaseSelfEmployment(None, clientDetailsWithConfirmation)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("cease-income-source-declaration-error")(messagesAPI("base.error-prefix") + " " + checkboxErrorMessageSE)
        )
      }
    }
  }
}
