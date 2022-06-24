
package controllers

import config.featureswitch.MFACreditsAndDebits
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{OK, SEE_OTHER}
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

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messages("credits.heading", s"$calendarYear"))
        )
      }
    }

    "redirect to Home page" when {
      "the feature switch is off" in {
        disable(MFACreditsAndDebits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK,
          propertyOnlyResponseWithMigrationData(
            testTaxYear - 1,
            Some(testTaxYear.toString))
        )

        val res = IncomeTaxViewChangeFrontend.getCreditsSummary(calendarYear)

        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.show().url)
        )
      }
    }
  }
}
