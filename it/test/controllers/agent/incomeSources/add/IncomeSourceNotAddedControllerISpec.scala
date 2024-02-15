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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceNotAddedControllerISpec extends ComponentSpecBase {

  private lazy val incomeSourceNotAddedController = controllers.incomeSources.add.routes.IncomeSourceNotAddedController

  val selfEmploymentNotSavedErrorUrl: String = incomeSourceNotAddedController.showAgent(incomeSourceType = SelfEmployment).url
  val ukPropertyNotSavedErrorUrl: String = incomeSourceNotAddedController.showAgent(incomeSourceType = UkProperty).url
  val foreignPropertyNotSavedErrorUrl: String = incomeSourceNotAddedController.showAgent(incomeSourceType = ForeignProperty).url

  val continueButtonText: String = messagesAPI("")
  val pageTitle: String = messagesAPI("standardError.heading")

  s"calling GET $selfEmploymentNotSavedErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/agents/income-sources/add/error-business-not-added", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle)
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)
      stubAuthorisedAgentUser(authorised = true)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/agents/income-sources/add/error-business-not-added", clientDetailsWithConfirmation)
      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $ukPropertyNotSavedErrorUrl" should {
    "render the UK Property not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/agents/income-sources/add/error-uk-property-not-added", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle)
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)
      stubAuthorisedAgentUser(authorised = true)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/agents/income-sources/add/error-uk-property-not-added", clientDetailsWithConfirmation)
      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $foreignPropertyNotSavedErrorUrl" should {
    "render the Foreign property not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/agents/income-sources/add/error-foreign-property-not-added", clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(pageTitle)
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)
      stubAuthorisedAgentUser(authorised = true)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/agents/income-sources/add/error-foreign-property-not-added", clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }
}