
package controllers.agent

import config.featureswitch.ForecastCalculation
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.{IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull

import java.time.LocalDate

class ForecastTaxCalcSummaryControllerISpec extends ComponentSpecBase {

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        Some(getCurrentTaxYearEnd)
      )
    )
  )


  "Calling the ForecastTaxCalcSummaryController(taxYear)" when {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)()

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status $OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(messagesAPI("standardError.heading"))
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)()

        result should have (
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid LiabilityCalculationModel response" should {
      "return the forecast tax calc summary page when the forecast calculation fs is enabled" in {
        Given("I enable the forecast calculation fs")
        enable(ForecastCalculation)
        stubAuthorisedAgentUser(authorised = true)

        And("I stub a successful calculation response")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/${getCurrentTaxYearEnd.getYear}/tax-due/forecast")
        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)

        result should have(
          httpStatus(OK),
          pageTitleAgent(messagesAPI("forecast_taxCalc.heading"))
        )
      }
    }
  }

}
