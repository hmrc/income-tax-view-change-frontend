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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceEndDateControllerISpec extends ComponentSpecBase {
  val dateBusinessShowAgentUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(Some(testPropertyIncomeId), SelfEmployment.key).url
  val dateBusinessSubmitAgentUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submitAgent(Some(testPropertyIncomeId), SelfEmployment.key).url
  val checkCeaseBusinessDetailsShowAgentUrl: String = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.showAgent().url

  val dateUKPropertyShowAgentUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, UkProperty.key).url
  val dateUKPropertySubmitAgentUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submitAgent(None, UkProperty.key).url
  val checkYourCeaseDetailsUkPropertyShowAgentUrl: String = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url

  val dateForeignPropertyShowAgentUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(None, ForeignProperty.key).url
  val dateForeignPropertySubmitAgentUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submitAgent(None, ForeignProperty.key).url
  val checkYourCeaseDetailsForeignPropertyShowAgentUrl: String = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.showAgent().url


  val hintText: String = messagesAPI("dateForm.hint")
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $dateBusinessShowAgentUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $dateBusinessShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getBusinessEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateBusinessSubmitAgentUrl" should {
    "redirect to showIncomeSourceEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("27"), "income-source-end-date.month" -> Seq("8"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentId", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowAgentUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("5"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentId", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )
      }
    }
  }

  s"calling GET $dateUKPropertyShowAgentUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $dateUKPropertyShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getUKPropertyEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitAgentUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/uk-property-end-date", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowAgentUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/uk-property-end-date", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )
      }
    }
  }

  s"calling GET $dateForeignPropertyShowAgentUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $dateForeignPropertyShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitAgentUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowAgentUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )
      }
    }
  }
}

