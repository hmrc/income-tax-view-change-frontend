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
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{foreignPropertyOnlyResponse, singleBusinessResponse, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class IncomeSourceAddedControllerISpec extends ControllerISpecHelper {

  val prefix: String = "business-added"
  val viewAllBusinessesLinkText: String = messagesAPI(s"$prefix.view-all-businesses")
  val day: LocalDate = LocalDate.of(2023, 1, 1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(GroupedObligationsModel("123", List(SingleObligationModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS", StatusFulfilled)))))


  val HomeControllerShowUrl: String = controllers.routes.HomeController.showAgent.url
  val pageTitle: String = messagesAPI("htmlTitle.agent", {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}".trim()
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}"
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val pathSEAdded = "/agents/manage-your-businesses/add-sole-trader/business-added"
  val pathUKPropertyAdded = "/agents/manage-your-businesses/add-uk-property/uk-property-added"
  val pathForeignPropertyAdded = "/agents/manage-your-businesses/add-foreign-property/foreign-property-added"

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  List(MTDPrimaryAgent, MTDSupportingAgent).foreach { case mtdUserRole =>
    val isSupportingAgent = mtdUserRole == MTDSupportingAgent
    val additionalCookies = getAgentClientDetailsForCookie(isSupportingAgent, true)
    s"GET $pathSEAdded" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Business Added page" when {
            "income sources is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.createSession(JourneyType(Add, SelfEmployment).toString))

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
                addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

              val result = buildGETMTDClient(pathSEAdded, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
                messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
              }
              else {
                business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
              }

              sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

              result should have(
                httpStatus(OK),
                pageTitleAgent(expectedText),
                elementTextByID("view-all-businesses-link")(viewAllBusinessesLinkText)
              )
            }
          }
          s"redirect to $HomeControllerShowUrl" when {
            "Income Sources Feature Switch is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

              val result = buildGETMTDClient(pathSEAdded, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(HomeControllerShowUrl)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(pathSEAdded, isSupportingAgent)
      }
    }

    s"GET $pathUKPropertyAdded" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Business Added page" when {
            "income sources is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.createSession(JourneyType(Add, UkProperty).toString))
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
                addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

              val result = buildGETMTDClient(pathUKPropertyAdded, additionalCookies).futureValue
              sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, UkProperty)).futureValue shouldBe Right(Some(true))

              result should have(
                httpStatus(OK),
                pageTitleCustom(pageTitle),
                elementTextBySelectorList(".govuk-panel.govuk-panel--confirmation")(confirmationPanelContent)
              )
            }
          }
          s"redirect to $HomeControllerShowUrl" when {
            "Income Sources Feature Switch is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

              val result = buildGETMTDClient(pathUKPropertyAdded, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(HomeControllerShowUrl)
              )
            }
          }
          "render error page" when {
            "UK property income source is missing trading start date" in {
              enable(IncomeSources)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))

              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
                addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

              val result = buildGETMTDClient(pathUKPropertyAdded, additionalCookies).futureValue

              result should have(
                httpStatus(INTERNAL_SERVER_ERROR),
                pageTitleAgent("standardError.heading", isErrorPage = true)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(pathUKPropertyAdded, isSupportingAgent)
      }
    }

    s"GET $pathForeignPropertyAdded" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid agent and client delegated enrolment" should {
          "render the Business Added page" when {
            "income sources is enabled" in {
              enable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)

              await(sessionService.createSession(JourneyType(Add, ForeignProperty).toString))
              await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
                addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)
              IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

              val result = buildGETMTDClient(pathForeignPropertyAdded, additionalCookies).futureValue
              verifyIncomeSourceDetailsCall(testMtditid)

              val expectedText: String = messagesAPI("business-added.foreign-property.h1") + " " + messagesAPI("business-added.foreign-property.base")

              And("Mongo storage is successfully set")
              sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, ForeignProperty)).futureValue shouldBe Right(Some(true))

              result should have(
                httpStatus(OK),
                pageTitleAgent(expectedText),
                elementTextByID("view-all-businesses-link")(viewAllBusinessesLinkText)
              )
            }
          }
          s"redirect to $HomeControllerShowUrl" when {
            "Income Sources Feature Switch is disabled" in {
              disable(IncomeSources)
              disable(NavBarFs)
              MTDAgentAuthStub.stubAuthorisedMTDAgent(testMtditid, isSupportingAgent)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

              val result = buildGETMTDClient(pathForeignPropertyAdded, additionalCookies).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(HomeControllerShowUrl)
              )
            }
          }
        }
        testAuthFailuresForMTDAgent(pathForeignPropertyAdded, isSupportingAgent)
      }
    }
  }
}
