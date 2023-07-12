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
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.{noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class UKPropertyAccountingMethodControllerISpec extends ComponentSpecBase {

  val addUKPropertyBusinessAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.showAgent().url
  val addUKPropertyBusinessAccountingMethodSubmitUrl: String = controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.submitAgent().url
  val checkUKPropertyDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url

  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $addUKPropertyBusinessAccountingMethodShowUrl" should {
    "render the UK Property Business Accounting Method page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no UK properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addUKPropertyBusinessAccountingMethodShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-accounting-method", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.uk-property-business-accounting-method.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addUKPropertyBusinessAccountingMethodSubmitUrl" should {
    s"redirect to $checkUKPropertyDetailsShowUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map("incomeSources.add.uk-property-business-accounting-method" -> Seq("cash"))
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-accounting-method", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkUKPropertyDetailsShowUrl)
        )
      }

      "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map("incomeSources.add.uk-property-business-accounting-method" -> Seq("traditional"))
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-accounting-method", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkUKPropertyDetailsShowUrl),
        )
      }

      "UK property already exists" in {
        val formData: Map[String, Seq[String]] = Map("incomeSources.add.uk-property-business-accounting-method" -> Seq("traditional"))
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-accounting-method", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkUKPropertyDetailsShowUrl),
        )
      }
    }

    s"return BAD_REQUEST $checkUKPropertyDetailsShowUrl" when {
      "user does not select anything" in {

        stubAuthorisedAgentUser(authorised = true)
        val formData: Map[String, Seq[String]] = Map("incomeSources.add.uk-property-business-accounting-method" -> Seq(""))
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-accounting-method", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
        )
      }
    }
  }
}
