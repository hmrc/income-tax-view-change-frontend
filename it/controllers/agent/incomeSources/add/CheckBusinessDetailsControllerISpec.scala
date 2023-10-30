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

package controllers.agent.incomeSources.add


import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSelfEmploymentId, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import java.time.LocalDate

class CheckBusinessDetailsControllerISpec extends ComponentSpecBase {

  val checkBusinessDetailsShowUrlAgent: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
  val checkBusinessDetailsSubmitUrlAgent: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.submitAgent().url
  val addBusinessReportingMethodUrlAgent: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(testSelfEmploymentId).url
  val errorPageUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(SelfEmployment).url

  val testBusinessId: String = testSelfEmploymentId
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "Test Road"
  val testBusinessPostCode: String = "B32 1PQ"
  val testBusinessCountryCode: String = "United Kingdom"
  val testBusinessAccountingMethod: String = "cash"
  val testBusinessAccountingMethodView: String = "Cash basis accounting"
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 11, 11)
  val testCountryCode = "GB"
  val noAccountingMethod: String = ""
  val continueButtonText: String = messagesAPI("base.confirm-and-continue")
  val testBusinessAddress: Address = Address(lines = Seq(testBusinessAddressLine1), postcode = Some(testBusinessPostCode))
  val testJourneyType: String = JourneyType(Add, SelfEmployment).toString

  val testAddIncomeSourceData = AddIncomeSourceData(
    businessName = Some(testBusinessName),
    businessTrade = Some(testBusinessTrade),
    dateStarted = Some(testBusinessStartDate),
    createdIncomeSourceId = Some(testBusinessId),
    address = Some(testBusinessAddress),
    countryCode = Some(testCountryCode),
    accountingPeriodEndDate = Some(testAccountingPeriodEndDate),
    incomeSourcesAccountingMethod = Some(testBusinessAccountingMethod)
  )

  val testUIJourneySessionData: UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = testJourneyType,
    addIncomeSourceData = Some(testAddIncomeSourceData))

  val testUIJourneySessionDataNoAccountingMethod = testUIJourneySessionData.copy(
    addIncomeSourceData = Some(testAddIncomeSourceData.copy(
      incomeSourcesAccountingMethod = None)))

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }


  s"calling GET $checkBusinessDetailsShowUrlAgent" should {
    "render the Check Business details page with accounting method" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)
        await(sessionService.setMongoData(testUIJourneySessionData))
        When(s"I call GET $checkBusinessDetailsShowUrlAgent")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/business-check-details", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          elementTextByID("business-name-value")(testBusinessName),
          elementTextByID("business-date-value")("1 January 2023"),
          elementTextByID("business-trade-value")(testBusinessTrade),
          elementTextByID("business-address-value")(testBusinessAddressLine1 + " " + testBusinessPostCode + " " + testBusinessCountryCode),
          elementTextByID("business-accounting-value")(testBusinessAccountingMethodView),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $checkBusinessDetailsSubmitUrlAgent" should {
    s"redirect to $addBusinessReportingMethodUrlAgent" when {
      "user selects 'confirm and continue'" in {
        val formData: Map[String, Seq[String]] = Map("addBusinessName" -> Seq("Test Business Name"),
          "addBusinessTrade" -> Seq("Test Business Name"),
          "addBusinessStartDate" -> Seq("Test Business Name"),
          "addBusinessAddressLine1" -> Seq("Test Business Name"),
          "addBusinessPostalCode" -> Seq("Test Business Name"),
          "addIncomeSourcesAccountingMethod" -> Seq("Test Business Name"))
        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)
        await(sessionService.setMongoData(testUIJourneySessionData))
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-check-details", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessReportingMethodUrlAgent)
        )
      }
    }
    s"redirect to $errorPageUrl" when {
      "error in response from API" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponse(testMtditid)
        await(sessionService.setMongoData(testUIJourneySessionData))

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-check-details", clientDetailsWithConfirmation)(Map.empty)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }

      "agent session details are empty" in {

        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-check-details", clientDetailsWithConfirmation)(Map.empty)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }
    }
  }
}
