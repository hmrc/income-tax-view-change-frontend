
package controllers

import config.featureswitch.CreditsRefundsRepay
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYear}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson}

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  "Navigating to /report-quarterly/income-and-expenses/view/credit-and-refunds" should {
    "display the credit and refund page" when {
      "a valid response is received" in {
        enable(CreditsRefundsRepay)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))


        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual("credit-and-refund.heading")
        )
      }
    }

    "redirect to custom not found page" when {
      "the feature switch is off" in {
        disable(CreditsRefundsRepay)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))


        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messagesAPI("error.custom.heading"))
        )
      }
    }
  }
}
