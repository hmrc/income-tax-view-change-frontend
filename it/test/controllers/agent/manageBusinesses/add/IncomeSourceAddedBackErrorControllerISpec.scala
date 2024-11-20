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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDAgentAuthStub}
import models.admin.{IncomeSources, NavBarFs}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{getAgentClientDetailsForCookie, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class IncomeSourceAddedBackErrorControllerISpec extends ControllerISpecHelper {

  private lazy val backErrorController = controllers.manageBusinesses.add.routes.IncomeSourceAddedBackErrorController

  val selfEmploymentBackErrorUrl: String = backErrorController.showAgent(SelfEmployment).url
  val ukPropertyBackErrorUrl: String = backErrorController.showAgent(UkProperty).url
  val foreignPropertyBackErrorUrl: String = backErrorController.showAgent(ForeignProperty).url

  val title = messagesAPI("cannotGoBack.heading")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val UIJourneySessionDataRepository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val pathSE = "/agents/manage-your-businesses/add/cannot-go-back-business-reporting-method"
  val pathUKProperty = "/agents/manage-your-businesses/add/cannot-go-back-uk-property-reporting-method"
  val pathForeignProperty = "/agents/manage-your-businesses/add/cannot-go-back-foreign-property-reporting-method"

  List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
    val isSupportingAgent = mtdUserRole == MTDSupportingAgent
    val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
    s"GET $pathSE" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the self employment business not added error page" when {
            "Income Sources FS is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), journeyIsComplete = None)))))

              val result = buildGETMTDClient(pathSE, additionalCookies).futureValue

              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(OK),
                pageTitleAgent(s"$title")
              )
            }
          }
          "redirect to home page" when {
            "Income Sources FS is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildGETMTDClient(pathSE, additionalCookies).futureValue

              verifyIncomeSourceDetailsCall(testMtditid)

              result should have(
                httpStatus(SEE_OTHER)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(pathSE, isSupportingAgent)
      }

      s"GET $pathUKProperty" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid agent and client delegated enrolment" should {
            "render the self employment business not added error page" when {
              "Income Sources FS is enabled" in {
                enable(IncomeSources)
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
                  addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), journeyIsComplete = None)))))

                val result = buildGETMTDClient(pathUKProperty, additionalCookies).futureValue

                verifyIncomeSourceDetailsCall(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitleAgent(s"$title")
                )
              }
            }
            "redirect to home page" when {
              "Income Sources FS is disabled" in {
                disable(IncomeSources)
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                val result = buildGETMTDClient(pathUKProperty, additionalCookies).futureValue

                verifyIncomeSourceDetailsCall(testMtditid)

                result should have(
                  httpStatus(SEE_OTHER)
                )
              }
            }
          }
          testAuthFailuresForMTDAgent(pathUKProperty, isSupportingAgent)

        }
      }

      s"GET $pathForeignProperty" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid agent and client delegated enrolment" should {
            "render the self employment business not added error page" when {
              "Income Sources FS is enabled" in {
                enable(IncomeSources)
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
                  addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), journeyIsComplete = None)))))

                val result = buildGETMTDClient(pathForeignProperty, additionalCookies).futureValue

                verifyIncomeSourceDetailsCall(testMtditid)

                result should have(
                  httpStatus(OK),
                  pageTitleAgent(s"$title")
                )
              }
            }
            "redirect to home page" when {
              "Income Sources FS is disabled" in {
                disable(IncomeSources)
                disable(NavBarFs)
                MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

                val result = buildGETMTDClient(pathForeignProperty, additionalCookies).futureValue

                verifyIncomeSourceDetailsCall(testMtditid)

                result should have(
                  httpStatus(SEE_OTHER)
                )
              }
            }
          }
          testAuthFailuresForMTDAgent(pathForeignProperty, isSupportingAgent)

        }
      }
    }
  }

}
