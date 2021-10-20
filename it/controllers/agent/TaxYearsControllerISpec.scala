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

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcDataIntegrationTestConstants.estimatedCalculationFullJson
import testConstants.messages.MyTaxYearsMessages.agentTitle
import config.featureswitch._
import controllers.Assets.INTERNAL_SERVER_ERROR
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{IncomeTaxViewChangeStub, IndividualCalculationStub}
import models.calculation.{CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest

import java.time.{LocalDate, LocalDateTime}

class TaxYearsControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(ITSASubmissionIntegration)
  }

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

  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
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

  s"GET ${routes.TaxYearsController.show().url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYears()

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYears()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYears()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYears(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
  }

  s"GET ${routes.TaxYearsController.show().url}" should {
    "return the tax years page" when {
      "all calls were successful and returned data and the submission integration feature switch is enabled" in {
        enable(ITSASubmissionIntegration)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYears(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)", "li:nth-of-type(1)")(
            expectedValue = s"6 April ${getCurrentTaxYearEnd.getYear - 1} to 5 April ${getCurrentTaxYearEnd.getYear}"
          ),
          elementTextBySelectorList("#content", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)", "li:nth-of-type(1)")(
            expectedValue = s"View return 6 April ${getCurrentTaxYearEnd.getYear - 1} to 5 April ${getCurrentTaxYearEnd.getYear}"
          ),
          elementTextBySelectorList("#content", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)", "li:nth-of-type(2)")(
            expectedValue = s"Update return 6 April ${getCurrentTaxYearEnd.getYear - 1} to 5 April ${getCurrentTaxYearEnd.getYear}"
          )
        )

      }
      "all calls were successful and returned data and the submission integration feature switch is disabled" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYears(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)", "li:nth-of-type(1)")(
            expectedValue = s"6 April ${getCurrentTaxYearEnd.getYear - 1} to 5 April ${getCurrentTaxYearEnd.getYear}"
          ),
          elementTextBySelectorList("#content", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)", "li:nth-of-type(1)")(
            expectedValue = s"View return 6 April ${getCurrentTaxYearEnd.getYear - 1} to 5 April ${getCurrentTaxYearEnd.getYear}"
          )
        )

      }
    }
    "return a technical difficulties page to the user" when {
      "there was a problem retrieving the client's income sources" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = INTERNAL_SERVER_ERROR,
          response = incomeSourceDetailsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYears(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500 - Business Tax account - GOV.UK")
        )
      }
      "there was a problem retrieving the calculation list for a year" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = INTERNAL_SERVER_ERROR,
          body = ListCalculationItems(Seq())
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYears(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
      "there was a problem retrieving a calculation for a year" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = INTERNAL_SERVER_ERROR,
          body = estimatedCalculationFullJson
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYears(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }
  }
}
