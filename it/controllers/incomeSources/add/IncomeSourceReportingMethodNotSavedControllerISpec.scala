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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceReportingMethodNotSavedControllerISpec extends ComponentSpecBase {

  val selfEmploymentReportingMethodNotSavedShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(SelfEmployment).url
  val ukPropertyReportingMethodNotSavedShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(UkProperty).url
  val foreignPropertyReportingMethodNotSavedShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(ForeignProperty).url

  object TestConstants {
    val selfEmployment: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.se")
    val seParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmployment)

    val ukProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.uk")
    val ukParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", ukProperty)

    val foreignProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.fp")
    val foreignParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", foreignProperty)

    val continueButtonText: String = messagesAPI("base.continue")
  }


  s"calling GET $selfEmploymentReportingMethodNotSavedShowUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $selfEmploymentReportingMethodNotSavedShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getSEReportingMethodNotSaved(Map.empty)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("paragraph-1")(TestConstants.seParagraph)
        )
      }
    }
  }

  s"calling GET $ukPropertyReportingMethodNotSavedShowUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $ukPropertyReportingMethodNotSavedShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getUkPropertyReportingMethodNotSaved(Map.empty)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("paragraph-1")(TestConstants.ukParagraph)
        )
      }
    }
  }

  s"calling GET $foreignPropertyReportingMethodNotSavedShowUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $foreignPropertyReportingMethodNotSavedShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.getForeignPropertyReportingMethodNotSaved(Map.empty)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("paragraph-1")(TestConstants.foreignParagraph)
        )
      }
    }
  }

}
