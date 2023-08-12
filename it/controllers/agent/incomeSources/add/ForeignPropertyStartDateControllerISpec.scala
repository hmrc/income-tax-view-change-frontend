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
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class ForeignPropertyStartDateControllerISpec extends ComponentSpecBase {
  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.showForeignPropertyAgent.url
  val foreignPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent.url
  val foreignPropertyStartDateCheckUrl: String = controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.showAgent().url

  val prefix = "incomeSources.add.foreignProperty.startDate"

  val hintText: String = messagesAPI("incomeSources.add.foreignProperty.startDate.hint") + " " +
    messagesAPI("incomeSources.add.foreignProperty.startDate.hintExample")
  val continueButtonText: String = messagesAPI("base.continue")

//  s"calling GET $foreignPropertyStartDateShowUrl" should {
//    "render the foreign property start date page" when {
//      "User is authorised" in {
//        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
//        stubAuthorisedAgentUser(authorised = true)
//        enable(IncomeSources)
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
//
//        When(s"I call GET $foreignPropertyStartDateShowUrl")
//        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date", clientDetailsWithConfirmation)
//        verifyIncomeSourceDetailsCall(testMtditid)
//
//        result should have(
//          httpStatus(OK),
//          pageTitleAgent("incomeSources.add.foreignProperty.startDate.heading"),
//          elementTextByID("income-source-start-date-hint")(hintText),
//          elementTextByID("continue-button")(continueButtonText)
//        )
//      }
//    }
//  }
//  s"calling POST $foreignPropertyStartDateSubmitUrl" should {
//    s"redirect to $foreignPropertyStartDateCheckUrl" when {
//      "form is filled correctly" in {
//        val formData: Map[String, Seq[String]] = {
//          Map("income-source-start-date.day" -> Seq("1"), "income-source-start-date.month" -> Seq("1"),
//            "income-source-start-date.year" -> Seq("2022"))
//        }
//        stubAuthorisedAgentUser(authorised = true)
//        enable(IncomeSources)
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
//
//        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date", clientDetailsWithConfirmation)(formData)
//
//        result should have(
//          httpStatus(SEE_OTHER),
//          redirectURI(foreignPropertyStartDateCheckUrl)
//        )
//      }
//      "form is filled incorrectly" in {
//        val formData: Map[String, Seq[String]] = {
//          Map("income-source-start-date.day" -> Seq("aa"), "income-source-start-date.month" -> Seq("02"),
//            "income-source-start-date.year" -> Seq("2023"))
//        }
//        stubAuthorisedAgentUser(authorised = true)
//        enable(IncomeSources)
//        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
//
//        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date", clientDetailsWithConfirmation)(formData)
//        result should have(
//          httpStatus(BAD_REQUEST),
//          elementTextByID("income-source-start-date-error")(messagesAPI("base.error-prefix") + " " +
//            messagesAPI("incomeSources.add.foreignProperty.startDate.error.invalid"))
//        )
//      }
//    }
//  }
}