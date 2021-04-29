/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers.agent

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.messages.IncomeSummaryMessages.{agentTitle, incomeSummaryAgentHeading}
import config.featureswitch.{AgentViewer, FeatureSwitching, IncomeBreakdown}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation.{CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest

import java.time.{LocalDate, LocalDateTime}

class IncomeSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val clientDetailsWithoutConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid
  )

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
      Some("Test Trading Name"), None, None, None, None, None, None, None,
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        "testId2",
        AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
        None, None, None, None,
        Some(getCurrentTaxYearEnd)
      )
    )
  )

  "Calling the IncomeSummaryController.showIncomeSummary(taxYear)" when {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getIncomeSummary(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getIncomeSummary(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $NOT_FOUND" when {
      "the agent viewer feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getIncomeSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        Then(s"A not found page is returned to the user")
        result should have(
          httpStatus(NOT_FOUND),
          pageTitle("Page not found - 404 - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getIncomeSummary(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getIncomeSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid CalcDisplayModel response, " +
      "feature switch AgentViewer and IncomeBreakdown is enabled" should {
      "return the correct income summary page" in {
        And("I wiremock stub a successful Income Source Details response with single Business and Property income")
        enable(IncomeBreakdown)
        enable(AgentViewer)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("calculationId1", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        When(s"I call GET ${routes.IncomeSummaryController.showIncomeSummary(getCurrentTaxYearEnd.getYear).url}")
        enable(AgentViewer)
        enable(IncomeBreakdown)
        val res = IncomeTaxViewChangeFrontend.getIncomeSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelector("h1")(incomeSummaryAgentHeading)
        )
      }
    }
  }
}
