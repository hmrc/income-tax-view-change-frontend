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

package controllers.agent.manageBusinesses.add

import controllers.agent.ControllerISpecHelper
import enums.IncomeSourceJourney.SelfEmployment
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import forms.manageBusinesses.add.BusinessTradeForm
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import models.incomeSourceDetails.AddIncomeSourceData.businessTradeField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesResponse, noPropertyOrBusinessResponse}

class AddBusinessTradeControllerISpec extends ControllerISpecHelper {


  val addBusinessAddressUrl = controllers.manageBusinesses.add.routes.AddBusinessAddressController.showAgent(isChange = false).url
  val incomeSourcesUrl: String = controllers.routes.HomeController.showAgent.url
  val checkDetailsUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment).url

  val pageTitleMsgKey: String = messagesAPI("add-trade.heading")
  val pageHint: String = messagesAPI("add-trade.trade-info-1") + " " + messagesAPI("add-trade.trade-info-2")
  val button: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business Name"
  val testBusinessTrade: String = "Test Business Trade"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val path = "/agents/manage-your-businesses/add-sole-trader/business-trade"
  val changePath = "/agents/manage-your-businesses/add-sole-trader/change-business-trade"

  List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Add Business trade page for an Agent" when {
            "Income Sources FS enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

              val res = buildGETMTDClient(path, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              res should have(
                httpStatus(OK),
                pageTitleAgent(pageTitleMsgKey),
                elementTextByID("business-trade-hint")(pageHint),
                elementTextByID("continue-button")(button)
              )
            }
          }
          "303 SEE_OTHER - redirect to home page" when {
            "Income Sources FS disabled" in {
              disable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(incomeSourcesUrl)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(path, isSupportingAgent)
      }
    }

    s"GET $changePath" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Add Business trade page for an Agent" when {
            "Income Sources FS enabled" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))
              val res = buildGETMTDClient(changePath, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              res should have(
                httpStatus(OK),
                pageTitleAgent(pageTitleMsgKey),
                elementTextByID("business-trade-hint")(pageHint),
                elementTextByID("continue-button")(button)
              )
            }
          }
          "303 SEE_OTHER - redirect to home page" when {
            "Income Sources FS disabled" in {
              disable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val result = buildGETMTDClient(changePath, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(incomeSourcesUrl)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(changePath, isSupportingAgent)
      }
    }

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          s"303 SEE_OTHER and redirect to $addBusinessAddressUrl" when {
            "User is authorised and business trade is valid" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
                )
              }

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

              val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

              sessionService.getMongoKeyTyped[String](businessTradeField, IncomeSourceJourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(testBusinessTrade))

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(addBusinessAddressUrl)
              )
            }
          }
          "show error when form is filled incorrectly" in {
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
              addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

            val formData: Map[String, Seq[String]] = {
              Map(
                BusinessTradeForm.businessTrade -> Seq("")
              )
            }

            val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue

            result should have(
              httpStatus(BAD_REQUEST),
              elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
                messagesAPI("add-business-trade.form.error.empty"))
            )
          }
        }

        testAuthFailuresForMTDAgent(path, isSupportingAgent, optBody = Some(Map(
            BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
          )))
      }
    }

    s"POST $changePath" when {
      s"a user is a $mtdUserRole" that {
        val isSupportingAgent = mtdUserRole == MTDSupportingAgent
        val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
        "is authenticated, with a valid agent and client delegated enrolment" should {
          s"303 SEE_OTHER and redirect to $checkDetailsUrl" when {
            "User is authorised and business trade is valid" in {
              enable(IncomeSourcesFs)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

              val changedTrade = "Updated Business Trade"
              val formData: Map[String, Seq[String]] = {
                Map(
                  BusinessTradeForm.businessTrade -> Seq(changedTrade)
                )
              }

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

              val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData).futureValue

              sessionService.getMongoKeyTyped[String](businessTradeField, IncomeSourceJourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(changedTrade))

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(checkDetailsUrl)
              )
            }
          }
          "show error when form is filled incorrectly" in {
            enable(IncomeSourcesFs)
            disable(NavBarFs)
            MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

            await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
              addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

            val formData: Map[String, Seq[String]] = {
              Map(
                BusinessTradeForm.businessTrade -> Seq("")
              )
            }

            val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData).futureValue

            result should have(
              httpStatus(BAD_REQUEST),
              elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
                messagesAPI("add-business-trade.form.error.empty"))
            )
          }
        }

        testAuthFailuresForMTDAgent(changePath, isSupportingAgent, optBody = Some(Map(
          BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
        )))
      }
    }
  }
}
