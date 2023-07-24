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
import forms.utils.SessionKeys.{addForeignPropertyAccountingMethod}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class ForeignPropertyAccountingMethodControllerISpec extends ComponentSpecBase {
  val foreignPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url
  val foreignPropertyAccountingMethodSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.submitAgent().url

  val checkForeignPropertyDetailsShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.showAgent().url

  val cashCookie: Map[String, String] = Map(addForeignPropertyAccountingMethod -> "cash")
  val accrualsCookie: Map[String, String] = Map(addForeignPropertyAccountingMethod -> "accruals")

  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $foreignPropertyAccountingMethodShowUrl" should {
    "render the Foreign Property Accounting Method page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyAccountingMethodShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-business-accounting-method", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.foreignPropertyAccountingMethod.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyAccountingMethodSubmitUrl" should {
    s"redirect to $checkForeignPropertyDetailsShowUrl" when {
      "user selects 'cash'" in {
        stubAuthorisedAgentUser(authorised = true)

        val formData: Map[String, Seq[String]] = Map("incomeSources.add.foreignPropertyAccountingMethod" -> Seq("cash"))
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method", cashCookie ++ clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkForeignPropertyDetailsShowUrl)
        )
      }
      s"redirect to $checkForeignPropertyDetailsShowUrl" when {
        "user selects 'accruals'" in {
          stubAuthorisedAgentUser(authorised = true)

          val formData: Map[String, Seq[String]] = Map("incomeSources.add.foreignPropertyAccountingMethod" -> Seq("traditional"))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method", accrualsCookie ++ clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkForeignPropertyDetailsShowUrl)
          )
        }
      }
      s"return BAD_REQUEST $foreignPropertyAccountingMethodShowUrl" when {
        "user does not select anything" in {
          stubAuthorisedAgentUser(authorised = true)

          val formData: Map[String, Seq[String]] = Map("incomeSources.add.foreignPropertyAccountingMethod" -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
}