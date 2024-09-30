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

package controllers.agent.manageBusinesses.manage

import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{ManageIncomeSourceData, UIJourneySessionData}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}


class ReportingMethodErrorControllerISpec extends ComponentSpecBase {

  private lazy val reportingMethodChangeErrorController = controllers.manageBusinesses.manage.routes.ReportingMethodChangeErrorController

  val reportingMethodChangeErrorUKPropertyUrl: String = reportingMethodChangeErrorController
    .show(incomeSourceType = UkProperty, isAgent = true).url
  val reportingMethodChangeErrorForeignPropertyUrl: String = reportingMethodChangeErrorController
    .show(incomeSourceType = ForeignProperty, isAgent = true).url
  val reportingMethodChangeErrorBusinessUrl: String = reportingMethodChangeErrorController
    .show(incomeSourceType = SelfEmployment, isAgent = true).url

  val continueButtonText: String = messagesAPI("base.continue")

  val pageTitle: String = messagesAPI("standardError.heading")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  s"calling GET $reportingMethodChangeErrorUKPropertyUrl" should {
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "Income Sources FS is disabled" in {

        stubAuthorisedAgentUser(authorised = true)

        disable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get("/manage-your-businesses/manage/error-change-reporting-method-not-saved-uk-property", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER)
        )
      }
    }
    "render the UK Property Reporting Method Change Error page" when {
      "Income Sources FS is enabled" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/manage/error-change-reporting-method-not-saved-uk-property", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle)
        )
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the user does not have a UK property Income Source" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/manage/error-change-reporting-method-not-saved-uk-property", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling GET $reportingMethodChangeErrorForeignPropertyUrl" should {
    "render the Foreign Property Reporting Method Change Error page" when {
      "Income Sources FS is enabled" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/manage/error-change-reporting-method-not-saved-foreign-property", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle)
        )
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the user does not have a Foreign property Income Source" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/manage/error-change-reporting-method-not-saved-foreign-property", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling GET $reportingMethodChangeErrorBusinessUrl" should {
    "render the Sole Trader Business Reporting Method Change Error page" when {
      "Income Sources FS is enabled" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/manage/error-change-reporting-method-not-saved", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle)
        )
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "Sole Trader Income Source Id does not exist" in {

        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = None)))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/manage/error-change-reporting-method-not-saved", clientDetailsWithStartDate)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
