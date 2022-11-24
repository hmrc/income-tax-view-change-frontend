
package controllers

import config.featureswitch.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{getCurrentTaxYearEnd, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson}

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  "Navigating to /report-quarterly/income-and-expenses/view/credit-and-refunds" should {

    val testTaxYear: Int = getCurrentTaxYearEnd.getYear
    val testPreviousTaxYear: Int = (getCurrentTaxYearEnd.getYear - 1)

    def setupCreditAndRefundControllerITests(): WSResponse = {

      Given("a success response from getIncomeSourceDetails API")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testPreviousTaxYear, Some(testTaxYear.toString)))

      And("a success response from getFinancialDetails API")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK, testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000,
        testTaxYear.toString, LocalDate.now().plusYears(1).toString, accruingInterestAmount = Some(2.37)))

      val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

      res
    }

    def setupCreditAndRefundControllerITestsPreviousYear(): WSResponse = {

      Given("a success response from getIncomeSourceDetails API")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testPreviousTaxYear, Some((testPreviousTaxYear - 1).toString)))

      And("a success response from getFinancialDetails API")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
        testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testPreviousTaxYear.toString, LocalDate.now().plusYears(1).toString, accruingInterestAmount = Some(2.37)))

      val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

      res
    }


    "display Credit from HMRC adjustments, Credits from Interest Accrued and Credits from an earlier tax year" when {
      "valid responses are received and feature switches are ENABLED" in {
        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        val res = setupCreditAndRefundControllerITests()

        res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "h2:nth-of-type(1)")(expectedValue = messagesAPI("credit-and-refund.subHeading.has-credits-1") + " £5.00 "
            + messagesAPI("credit-and-refund.subHeading.has-credits-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-2") + " 0"),
          elementAttributeBySelector("#credit-and-refund-1", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2.37 " + messagesAPI("credit-and-refund.credit-interest-accrued-prt-1") + " " + messagesAPI("credit-and-refund.credit-interest-accrued-prt-2") + " 0a"),
          elementAttributeBySelector("#credit-and-refund-2", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-0", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-3", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 3"),
          elementAttributeBySelector("#credit-and-refund-3", "href")("/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(7)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          pageTitleIndividual("credit-and-refund.heading")
        )
      }

      "Credits from the previous tax year" in {
        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        val res = setupCreditAndRefundControllerITestsPreviousYear()
        res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "h2:nth-of-type(1)")(expectedValue = messagesAPI("credit-and-refund.subHeading.has-credits-1") + " £5.00 "
            + messagesAPI("credit-and-refund.subHeading.has-credits-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-2") + " 0"),
          elementAttributeBySelector("#credit-and-refund-0", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/$testPreviousTaxYear"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2.37 " + messagesAPI("credit-and-refund.credit-interest-accrued-prt-1") + " " + messagesAPI("credit-and-refund.credit-interest-accrued-prt-2") + " 0a"),
          elementAttributeBySelector("#credit-and-refund-0a", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/$testPreviousTaxYear"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-1", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/$testPreviousTaxYear"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-2", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/$testPreviousTaxYear"),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 3"),
          elementAttributeBySelector("#credit-and-refund-3", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/$testPreviousTaxYear"),

          elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(7)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),
          pageTitleIndividual("credit-and-refund.heading")
        )
      }
    }

    "redirect to custom not found page" when {
      "the feature switch is off" in {
        disable(CreditsRefundsRepay)
        val res = setupCreditAndRefundControllerITests()

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messagesAPI("error.custom.heading"))
        )
      }
    }
  }
}

