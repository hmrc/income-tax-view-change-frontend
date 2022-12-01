
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
          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 0"),
          elementAttributeBySelector("#credit-and-refund-0", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-1", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-2", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£3.00 " + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2.00 " + messagesAPI("credit-and-refund.refundProgress-prt-2")),
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
