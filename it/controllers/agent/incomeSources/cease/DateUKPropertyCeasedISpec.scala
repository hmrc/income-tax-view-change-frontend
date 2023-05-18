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
import testConstants.IncomeSourceIntegrationTestConstants.ukPropertyOnlyResponse

class DateUKPropertyCeasedISpec extends ComponentSpecBase {
  val dateUKPropertyShowUrl: String = controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.showAgent().url
  val dateUKPropertySubmitUrl: String = controllers.incomeSources.cease.routes.DateUKPropertyCeasedController.submitAgent().url
  val checkYourCeaseDetailsShowUrl: String = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url
  val hintText: String = messagesAPI("incomeSources.cease.dateUKPropertyCeased.hint")
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $dateUKPropertyShowUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $dateUKPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontend.getDateUKPropertyCeased(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.dateUKPropertyCeased.heading"),
          elementTextByID("date-uk-property-stopped-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitUrl" should {
    "redirect to showDateUKPropertyCeasedControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("date-uk-property-stopped.day" -> Seq("20"), "date-uk-property-stopped.month" -> Seq("12"), "date-uk-property-stopped.year" -> Seq("2022"))
        }
        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/uk-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("date-uk-property-stopped.day" -> Seq("aa"), "date-uk-property-stopped.month" -> Seq("12"), "date-uk-property-stopped.year" -> Seq("2022"))
        }
        stubAuthorisedAgentUser(authorised = true)

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/uk-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("date-uk-property-stopped-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.dateUKPropertyCeased.error.invalid"))
        )
      }
    }
  }
}

