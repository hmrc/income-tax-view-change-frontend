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
import enums.IncomeSourceJourney.UkProperty
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse
import java.time.LocalDate


class CheckUKPropertyDetailsControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val uiRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  object CheckUKPropertyDetails {
    val showUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url
    val submitUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submitAgent().url
    val backUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty).url
    val successUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(isAgent = true, UkProperty, "1234567890").url
    val failureUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(UkProperty).url
    val changeText: String = messagesAPI("incomeSources.add.checkUKPropertyDetails.change") + " " +
      messagesAPI("incomeSources.add.checkUKPropertyDetails.change") // duplicated due to visually hidden text
    val confirmText: String = messagesAPI("incomeSources.add.checkUKPropertyDetails.confirm")
  }

  val testPropertyStartDateLong: String = "1 January 2023"
  val testPropertyStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testPropertyAccountingMethod: String = "CASH"
  val testPropertyAccountingMethodView: String = "Cash basis accounting"
  val continueButtonText: String = messagesAPI("base.confirm-and-continue")
  val testJourneyType: JourneyType = JourneyType(Add, UkProperty)
  val testJourneyTypeString: String = JourneyType(Add, UkProperty).toString

  val testAddIncomeSourceData = AddIncomeSourceData(
    dateStarted = Some(testPropertyStartDate),
    incomeSourcesAccountingMethod = Some(testPropertyAccountingMethod)
  )

  val testUIJourneySessionData: UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = testJourneyTypeString,
    addIncomeSourceData = Some(testAddIncomeSourceData))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(uiRepository.deleteOne(UIJourneySessionData(testSessionId, testJourneyTypeString)))
  }


  s"calling GET ${CheckUKPropertyDetails.showUrl}" should {
    "200 - render the Check UK Property Details page" when {
      "User is authorised and has completed the Add UK Property journey" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData))

        When(s"I call GET ${CheckUKPropertyDetails.showUrl}")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.checkUKPropertyDetails.heading"),
          elementTextByID("change-start-date-link-value")(testPropertyStartDateLong),
          elementTextByID("change-start-date-link")(CheckUKPropertyDetails.changeText),
          elementTextByID("change-accounting-method-link-value")("Cash basis accounting"),
          elementTextByID("change-accounting-method-link")(CheckUKPropertyDetails.changeText),
          elementTextByID("continue-button")(CheckUKPropertyDetails.confirmText)
        )
      }
      "500 ISE" when {
        "User has not completed the Add UK Property journey" in {
          Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          When(s"I call GET ${CheckUKPropertyDetails.showUrl}")
          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)

          result should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
      "303 SEE_OTHER - redirect to home page" when {
        "Income Sources FS disabled" in {
          Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
          stubAuthorisedAgentUser(authorised = true)
          disable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          When(s"I call GET ${CheckUKPropertyDetails.showUrl}")
          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
          )
        }
      }
    }
  }

  s"calling POST ${CheckUKPropertyDetails.submitUrl}" should {
    "303 SEE_OTHER and redirect to UK Property Reporting Method page" when {
      "User is authorised and has completed the Add UK Property journey" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        Given("I wiremock stub a successful Create Income Sources (UK Property) response")
        val createResponseJson = List(CreateIncomeSourceResponse("1234567890"))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, createResponseJson)

        await(sessionService.setMongoData(testUIJourneySessionData))

        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2021-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(CheckUKPropertyDetails.successUrl)
        )
      }
    }
    "303 SEE_OTHER and redirect to UK Property Not Added error page" when {
      "Error received from API 1776" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        Given("I wiremock stub an unsuccessful Create Income Sources (UK Property) response")

        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2021-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponse(testMtditid)

        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(CheckUKPropertyDetails.failureUrl)
        )
      }
    }
    "500 ISE" when {
      "User has not completed the Add UK Property journey" in {
        Given("I wiremock stub a successful Create Income Sources (UK Property) response")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        val createResponseJson = List(CreateIncomeSourceResponse("1234567890"))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, createResponseJson)


        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2021-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
    "303 SEE_OTHER and redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2021-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        When(s"I call POST ${CheckUKPropertyDetails.submitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-check-details", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/agents/client-income-tax")
        )
      }
    }
  }

}
