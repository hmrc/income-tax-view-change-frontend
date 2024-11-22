/*
 * Copyright 2024 HM Revenue & Customs
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
import controllers.agent.ControllerISpecHelper
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Cease
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSources, NavBarFs}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.{CeaseIncomeSourceData, IncomeSourceDetailsModel, UIJourneySessionData}
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class CheckCeaseIncomeSourceDetailsControllerISpec extends ControllerISpecHelper {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val testLongEndDate2022: String = "10 October 2022"
  val changeLink = "Change"
  val testBusinessName = "business"
  val timestamp = "2023-01-31T09:26:17Z"

  val selfEmploymentPath = "/agents/income-sources/cease/business-check-details"
  val ukPropertyPath = "/agents/income-sources/cease/uk-property-check-details"
  val foreignPropertyPath = "/agents/income-sources/cease/foreign-property-check-details"

  val showCheckCeaseBusinessDetailsControllerUrl = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment).url
  val formActionSE = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment).url

  val businessAddressLabel = messagesAPI("incomeSources.ceaseBusiness.checkDetails.businessAddress")
  val pageTitleMsgKey = messagesAPI("incomeSources.ceaseBusiness.checkDetails.heading")
  val unknown: String = messagesAPI("incomeSources.ceaseBusiness.checkDetails.unknown")

  val redirectUriSE = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment).url

  val formActionUK = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty).url
  val pageTitleMsgKeyUK = messagesAPI("incomeSources.ceaseUKProperty.checkDetails.heading")
  val redirectUriUK = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty).url

  val formActionFP = controllers.incomeSources.cease.routes.CeaseCheckIncomeSourceDetailsController.submitAgent(ForeignProperty).url
  val redirectUriFP = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty).url

  def testUser(incomeSourceDetails: IncomeSourceDetailsModel): MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, incomeSourceDetails,
    None, Some("1234567890"), Some("12345-credId"), Some(Agent), Some("1")
  )(FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"GET $selfEmploymentPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Cease Business Page" when {
            "Income source is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = businessAndPropertyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
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
          "redirect to Home Page" when {
            "Income source is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = businessAndPropertyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              val result = buildGETMTDClient(selfEmploymentPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.showAgent.url)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(selfEmploymentPath, isSupportingAgent)
      }
    }
  }

  s"GET $ukPropertyPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Cease Business Page" when {
            "Income source is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = ukPropertyOnlyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              val result = buildGETMTDClient(ukPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitleAgent(pageTitleMsgKeyUK),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("cease-check-answers.cease-date")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),
                elementAttributeBySelector("form", "action")(formActionUK)
              )
            }
          }
          "redirect to Home Page" when {
            "Income source is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = ukPropertyOnlyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              val result = buildGETMTDClient(ukPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.showAgent.url)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(ukPropertyPath, isSupportingAgent)
      }
    }
  }

  s"GET $foreignPropertyPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Cease Business Page" when {
            "Income source is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = foreignPropertyOnlyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              val result = buildGETMTDClient(foreignPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitleAgent(pageTitleMsgKeyUK),
                elementTextBySelectorList(".govuk-summary-list__key", "dt:nth-of-type(1)")(messagesAPI("incomeSources.ceaseForeignProperty.checkDetails.dateStopped")),
                elementTextBySelectorList(".govuk-summary-list__value", "dd:nth-of-type(1)")(testLongEndDate2022),
                elementTextByID("change")(changeLink),
                elementAttributeBySelector("form", "action")(formActionFP)
              )
            }
          }
          "redirect to Home Page" when {
            "Income source is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = foreignPropertyOnlyResponse
              )

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              val result = buildGETMTDClient(foreignPropertyPath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.routes.HomeController.showAgent.url)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(foreignPropertyPath, isSupportingAgent)
      }
    }
  }

  s"POST $selfEmploymentPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          s"redirect to $redirectUriSE" when {
            "Income source is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)
              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              When(s"I call POST $selfEmploymentPath")
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))

              val result = buildPOSTMTDPostClient(selfEmploymentPath, additionalCookies, Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(redirectUriSE),
              )
              AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(SelfEmployment, testEndDate2022, mkIncomeSourceId(testSelfEmploymentId), None)(testUser(businessOnlyResponse), hc).detail)
            }
          }
          testAuthFailuresForMTDAgent(selfEmploymentPath, isSupportingAgent)
        }
      }
    }
  }

  s"POST $ukPropertyPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          s"redirect to $redirectUriUK" when {
            "Income source is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              When(s"I call POST $ukPropertyPath")
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              val result = buildPOSTMTDPostClient(ukPropertyPath, additionalCookies, Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(redirectUriUK),
              )
              AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(UkProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser(ukPropertyOnlyResponse), hc).detail)
            }
          }
          testAuthFailuresForMTDAgent(ukPropertyPath, isSupportingAgent)
        }
      }
    }
  }

  s"POST $foreignPropertyPath" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          s"redirect to $redirectUriFP" when {
            "Income source is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
              IncomeTaxViewChangeStub.stubUpdateIncomeSource(OK, Json.toJson(UpdateIncomeSourceResponseModel(timestamp)))

              When(s"I call POST $foreignPropertyPath")
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
                Some(CeaseIncomeSourceData(incomeSourceId = None, endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

              val result = buildPOSTMTDPostClient(foreignPropertyPath, additionalCookies, Map.empty).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(redirectUriFP),
              )
              AuditStub.verifyAuditContainsDetail(CeaseIncomeSourceAuditModel(ForeignProperty, testEndDate2022, mkIncomeSourceId(testPropertyIncomeId), None)(testUser(foreignPropertyOnlyResponse), hc).detail)
            }
          }
          testAuthFailuresForMTDAgent(foreignPropertyPath, isSupportingAgent)
        }
      }
    }
  }

}
