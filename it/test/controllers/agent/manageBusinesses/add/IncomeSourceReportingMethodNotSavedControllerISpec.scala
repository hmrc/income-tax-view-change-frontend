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

package controllers.agent.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.IncomeSources
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceReportingMethodNotSavedControllerISpec extends ComponentSpecBase {

  val selfEmploymentReportingMethodNotSavedShowAgentUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(SelfEmployment).url
  val ukPropertyReportingMethodNotSavedShowAgentUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(UkProperty).url
  val foreignPropertyReportingMethodNotSavedShowAgentUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty).url

  object TestConstants {
    val selfEmployment: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.se.incomeSource")
    val seParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmployment)

    val ukProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.uk.incomeSource")
    val ukParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", ukProperty)

    val foreignProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.fp.incomeSource")
    val foreignParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", foreignProperty)

    val continueButtonText: String = messagesAPI("base.continue")
  }


  s"calling GET $selfEmploymentReportingMethodNotSavedShowAgentUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        When(s"I call GET $selfEmploymentReportingMethodNotSavedShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getSEReportingMethodNotSaved(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("paragraph-1")(TestConstants.seParagraph)
        )
      }
    }
  }

  s"calling GET $ukPropertyReportingMethodNotSavedShowAgentUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        When(s"I call GET $ukPropertyReportingMethodNotSavedShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getUkPropertyReportingMethodNotSaved(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("paragraph-1")(TestConstants.ukParagraph)
        )
      }
    }
  }

  s"calling GET $foreignPropertyReportingMethodNotSavedShowAgentUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        When(s"I call GET $foreignPropertyReportingMethodNotSavedShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getForeignPropertyReportingMethodNotSaved(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("paragraph-1")(TestConstants.foreignParagraph)
        )
      }
    }
  }

}
