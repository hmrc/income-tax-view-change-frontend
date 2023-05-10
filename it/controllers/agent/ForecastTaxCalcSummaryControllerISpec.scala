
package controllers.agent

import audit.models.ForecastTaxCalculationAuditModel
import auth.MtdItUserWithNino
import config.featureswitch.ForecastCalculation
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks.{AuditStub, IncomeTaxCalculationStub}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

object ForecastTaxSummaryAgentControllerTestConstants {
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
    totalTaxableIncome = Some(taxableIncome),
    totalAllowancesAndDeductions = Some(4200.00),
    incomeTaxAmount = Some(5000.99),
    nic2 = Some(5000.99),
    nic4 = Some(5000.99),
    totalNicAmount = Some(5000.99),
    totalTaxDeductedBeforeCodingOut = Some(5000.99),
    saUnderpaymentsCodedOut = Some(5000.99),
    totalStudentLoansRepaymentAmount = Some(5000.99),
    totalAnnuityPaymentsTaxCharged = Some(5000.99),
    totalRoyaltyPaymentsTaxCharged = Some(5000.99),
    totalTaxDeducted = Some(-99999999999.99),
    incomeTaxNicAmount = Some(-99999999999.99),
    cgtAmount = Some(5000.99),
    incomeTaxNicAndCgtAmount = Some(5000.99)
  )
}

class ForecastTaxCalcSummaryControllerISpec extends ComponentSpecBase {

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      None,
      Some(getCurrentTaxYearEnd),
      None
    )),
    properties = List(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        None,
        None,
        Some(getCurrentTaxYearEnd),
        None
      )
    )
  )


  "Calling the ForecastTaxCalcSummaryController(taxYear)" when {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)()

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

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status $OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
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
          body = liabilityCalculationModelSuccessful
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/${getCurrentTaxYearEnd.getYear}/tax-due/forecast")
        val result: WSResponse = IncomeTaxViewChangeFrontend.getForecastTaxCalcSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)

        AuditStub.verifyAuditEvent(ForecastTaxCalculationAuditModel(ForecastTaxSummaryAgentControllerTestConstants.mtdItUser,
          ForecastTaxSummaryAgentControllerTestConstants.endOfYearEstimate))

        result should have(
          httpStatus(OK),
          pageTitleAgent("forecast_taxCalc.heading")
        )
      }
    }
  }

}
