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

package controllers.agent.incomeSources.cease

import config.featureswitch.IncomeSources
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.foreignPropertyOnlyResponse

class ForeignPropertyEndDateISpec extends ComponentSpecBase {
  val dateForeignPropertyShowUrl: String = controllers.incomeSources.cease.routes.ForeignPropertyEndDateController.showAgent().url
  val dateForeignPropertySubmitUrl: String = controllers.incomeSources.cease.routes.ForeignPropertyEndDateController.submitAgent().url
  val checkYourCeaseDetailsShowUrl: String = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url
  val hintText: String = messagesAPI("incomeSources.cease.ForeignPropertyEndDate.hint")
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $dateForeignPropertyShowUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $dateForeignPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.ForeignPropertyEndDate.heading"),
          elementTextByID("foreign-property-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitUrl" should {
    "redirect to showForeignPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("foreign-property-end-date.day" -> Seq("20"), "foreign-property-end-date.month" -> Seq("12"), "foreign-property-end-date.year" -> Seq("2022"))
        }
        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("foreign-property-end-date.day" -> Seq("aa"), "foreign-property-end-date.month" -> Seq("12"), "foreign-property-end-date.year" -> Seq("2022"))
        }
        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("foreign-property-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.ForeignPropertyEndDate.error.invalid"))
        )
      }
    }
  }
}

