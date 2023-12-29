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

import audit.models.ForecastIncomeAuditModel
import auth.MtdItUserWithNino
import config.featureswitch.{FeatureSwitching, ForecastCalculation}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

object ForecastIncomeSummaryControllerTestConstants {
  val mtdItUser: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, None, None, Some("1234567890"),
    None, Some(Agent), Some("1"))(FakeRequest())

  val taxableIncome = 12500

  val endOfYearEstimate: EndOfYearEstimate = EndOfYearEstimate(
    incomeSource = Some(List(
      IncomeSource("01", Some("self-employment1"), taxableIncome),
      IncomeSource("01", Some("self-employment2"), taxableIncome),
      IncomeSource("02", None, taxableIncome),
      IncomeSource("03", None, taxableIncome),
      IncomeSource("04", None, taxableIncome),
      IncomeSource("05", Some("employment1"), taxableIncome),
      IncomeSource("05", Some("employment2"), taxableIncome),
      IncomeSource("06", None, taxableIncome),
      IncomeSource("07", None, taxableIncome),
      IncomeSource("08", None, taxableIncome),
      IncomeSource("09", None, taxableIncome),
      IncomeSource("10", None, taxableIncome),
      IncomeSource("11", None, taxableIncome),
      IncomeSource("12", None, taxableIncome),
      IncomeSource("13", None, taxableIncome),
      IncomeSource("14", None, taxableIncome),
      IncomeSource("15", None, taxableIncome),
      IncomeSource("16", None, taxableIncome),
      IncomeSource("17", None, taxableIncome),
      IncomeSource("18", None, taxableIncome),
      IncomeSource("19", None, taxableIncome),
      IncomeSource("20", None, taxableIncome),
      IncomeSource("21", None, taxableIncome),
      IncomeSource("22", None, taxableIncome),
      IncomeSource("98", None, taxableIncome)
    )),
    totalEstimatedIncome = Some(taxableIncome),
  )
}

class ForecastIncomeSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

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
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      None,
      Some(getCurrentTaxYearEnd),
      None,
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = List(
      PropertyDetailsModel(
        "testId2",
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        None,
        None,
        Some(getCurrentTaxYearEnd),
        None,
        cashOrAccruals = false
      )
    )
  )

  "Calling the IncomeSummaryController.showIncomeSummaryAgent(taxYear)" when {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastIncomeSummaryAgent(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastIncomeSummaryAgent(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastIncomeSummaryAgent(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastIncomeSummaryAgent(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }
    }
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid Liability Calc response" should {
      "return the correct income summary page and audit event" in {
        Given("I enable forecast calculation display feature switch")
        enable(ForecastCalculation)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/income/forecast")
        val res = IncomeTaxViewChangeFrontend.getForecastIncomeSummaryAgent(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        AuditStub.verifyAuditEvent(ForecastIncomeAuditModel(ForecastIncomeSummaryControllerTestConstants.mtdItUser,
          ForecastIncomeSummaryControllerTestConstants.endOfYearEstimate))

        res should have(
          httpStatus(OK),
          pageTitleAgent("forecast_income.heading")
        )
      }
    }
  }
}
