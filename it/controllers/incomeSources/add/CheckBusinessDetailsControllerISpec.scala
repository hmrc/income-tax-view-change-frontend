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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.{AddIncomeSourceData, Address, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import java.time.LocalDate

class CheckBusinessDetailsControllerISpec extends ComponentSpecBase {

  val checkBusinessDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  val checkBusinessDetailsSubmitUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit().url
  val addBusinessReportingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(isAgent = false, SelfEmployment, testSelfEmploymentId).url
  val errorPageUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(SelfEmployment).url


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

  s"calling GET $checkBusinessDetailsShowUrl" should {
    "render the Check Business details page with accounting method" when {
      "User is authorised and has no existing businesses" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        await(sessionService.setMongoData(testUIJourneySessionData))

        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/business-check-details")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("check-business-details.heading"),
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

  s"calling POST $checkBusinessDetailsSubmitUrl" should {
    s"redirect to $addBusinessReportingMethodUrl" when {
      "user selects 'confirm and continue'" in {
        enable(IncomeSources)
        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)
        await(sessionService.setMongoData(testUIJourneySessionData))
        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/business-check-details")(Map.empty)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessReportingMethodUrl)
        )
      }
    }
    s"redirect to business not added $errorPageUrl" when {
      "error in response from API" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponse(testMtditid)

        When(s"I call $checkBusinessDetailsSubmitUrl")
        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/business-check-details")(Map.empty)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }
      "user session has no details" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-check-details")(Map.empty)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }
    }
  }
}
