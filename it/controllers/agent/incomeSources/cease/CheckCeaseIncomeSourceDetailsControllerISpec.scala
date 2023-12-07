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

import audit.models.CeaseIncomeSourceAuditModel
import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import models.updateIncomeSource.{Cessation, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val cessationDate = "2022-10-10"
  val businessEndShortLongDate = "23 April 2022"
  val testLongEndDate2022: String = "10 October 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"

  val showCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment).url
  val submitCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment).url
  val formActionSE = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment).url
  val businessStopDateLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")
  val businessNameLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")
  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")
  val redirectUriSE = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment).url
  val requestSE: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = business1.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val showCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty).url
  val submitCheckCeaseUKPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty).url
  val formActionUK = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty).url
  val businessStopDateLabelUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.content")
  val pageTitleMsgKeyUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")
  val redirectUriUK = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty).url
  val requestUK: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = ukProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val showCheckCeaseForeignPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url
  val submitCheckCeaseForeignPropertyDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url

  val formActionFP = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(ForeignProperty).url
  val pageTitleMsgKeyFP = messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.heading")
  val redirectUriFP = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty).url
  val requestFP: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceID = foreignProperty.incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), None, Some(Agent), None
  )(FakeRequest())

  s"calling GET ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    "render the Cease Business Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = businessAndPropertyResponse
        )

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(testEndDate2022), ceasePropertyDeclare = None)))))

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseBusinessDetails(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),

          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessName")),
          elementTextBySelector("dl:nth-of-type(1) div:nth-of-type(2) dd:nth-of-type(1)")(testBusinessName),
          elementAttributeBySelector("form", "action")(formActionSE)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseBusinessDetailsControllerUrl}" should {
    s"redirect to $redirectUriSE" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a SE business")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseBusinessDetailsControllerUrl}")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(testEndDate2022), ceasePropertyDeclare = None)))))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseBusinessDetails(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestSE).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriSE),
        )
        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(SelfEmployment, testEndDate2022, mkIncomeSourceId(testSelfEmploymentId), None)(testUser, hc).detail)
      }
    }
  }

  s"calling GET ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    "render the Cease UK Property Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = ukPropertyOnlyResponse
        )

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(testEndDate2022), ceasePropertyDeclare = Some(stringTrue))))))

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseUKPropertyDetails(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeyUK),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseUKProperty.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),
          elementAttributeBySelector("form", "action")(formActionUK)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseUKPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUriUK" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseUKPropertyDetailsControllerUrl}")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(testEndDate2022), ceasePropertyDeclare = Some(stringTrue))))))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseUKPropertyDetails(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestUK).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriUK),
        )
        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(UkProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser, hc).detail)
      }
    }
  }

  s"calling GET ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    "render the Cease Foreign Property Page" when {
      "User is authorised" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = foreignPropertyOnlyResponse
        )
        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(testEndDate2022), ceasePropertyDeclare = Some(stringTrue))))))

        val res = IncomeTaxViewChangeFrontend.getCheckCeaseForeignPropertyDetails(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKeyFP),
          elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.dateStopped")),
          elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
          elementTextByID("change")(changeLink),
          elementAttributeBySelector("form", "action")(formActionFP)
        )
      }
    }
  }

  s"calling POST ${showCheckCeaseForeignPropertyDetailsControllerUrl}" should {
    s"redirect to $redirectUriFP" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign Property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
        IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))
        When(s"I call POST ${submitCheckCeaseForeignPropertyDetailsControllerUrl}")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(testEndDate2022), ceasePropertyDeclare = Some(stringTrue))))))

        val res = IncomeTaxViewChangeFrontend.postCheckCeaseForeignPropertyDetails(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyUpdateIncomeSource(Some(Json.toJson(requestFP).toString()))

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(redirectUriFP),
        )
        AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(ForeignProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser, hc).detail)
      }
    }
  }
}
