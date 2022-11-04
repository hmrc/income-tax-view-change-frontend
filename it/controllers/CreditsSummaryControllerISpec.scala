
package controllers

import config.featureswitch.{CutOverCredits, MFACreditsAndDebits}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYear}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson}

import java.time.LocalDate

class CreditsSummaryControllerISpec extends ComponentSpecBase {

  val calendarYear = "2018"
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  s"Navigating to /report-quarterly/income-and-expenses/view/credits-from-hmrc/$testTaxYear" should {
    "display the credit summary page" when {
      "a valid response is received" in {
        enable(MFACreditsAndDebits)
        enable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          testNino,
          s"${testTaxYear - 1}-04-06",
          s"$testTaxYear-04-05")(
          OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(
            -1400,
            -1400,
            testTaxYear.toString,
            LocalDate.now().plusYears(1).toString)
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid, 1)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05", 2)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messages("credits.heading", s"$calendarYear"))
        )
      }
    }

    "display an empty credit summary page" when {
      "MFACreditsAndDebits and CutOverCredits feature switches are off" in {
        disable(MFACreditsAndDebits)
        disable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK,
          propertyOnlyResponseWithMigrationData(
            testTaxYear - 1,
            Some(testTaxYear.toString))
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messages("credits.heading", s"$calendarYear"))
        )
      }
    }
  }
}
