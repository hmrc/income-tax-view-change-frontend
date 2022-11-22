
package controllers.agent

import config.featureswitch.CreditsRefundsRepay
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson, testValidFinancialDetailsModelJson}
import testConstants.OutstandingChargesIntegrationTestConstants.validOutStandingChargeResponseJsonWithAciAndBcdCharges
import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  "Navigating to /report-quarterly/income-and-expenses/view/agents/credit-and-refunds" should {

    val testTaxYear: Int = getCurrentTaxYearEnd.getYear
    val testPreviousTaxYearAsString: String = (getCurrentTaxYearEnd.getYear - 1).toString

    "display the credit and refund page" when {
      "a valid response is received" in {
        enable(CreditsRefundsRepay)

        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with a property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))

        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 0"),
          elementAttributeBySelector("#credit-and-refund-0", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-1", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-2", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),
          pageTitleAgent("credit-and-refund.heading")

        )
      }
    }

    // TODO maybe make different tax year on this test
    "display the credit and refund page with MFA credits in the previous tax year" when {
      "a valid response is received" in {
        enable(CreditsRefundsRepay)

        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with a property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some((testTaxYear - 2).toString)))


        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testPreviousTaxYearAsString, LocalDate.now().plusYears(1).toString))

        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 0"),
          elementAttributeBySelector("#credit-and-refund-0", "href")(s"/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/$testPreviousTaxYearAsString"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-1", "href")(s"/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/$testPreviousTaxYearAsString"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-2", "href")(s"/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/$testPreviousTaxYearAsString"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),
          pageTitleAgent("credit-and-refund.heading")

        )
      }
    }

    "redirect to custom not found page" when {
      "the feature switch is off" in {
        disable(CreditsRefundsRepay)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))


        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messagesAPI("error.custom.heading"))
        )
      }
    }

    "redirect to unauthorised page" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = IncomeSourceDetailsModel(
            mtdbsa = testMtditid,
            yearOfMigration = None,
            businesses = List(BusinessDetailsModel(
              Some("testId"),
              Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
              None,
              Some(getCurrentTaxYearEnd)
            )),
            property = None
          )
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testTaxYear - 1-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelJson(
            -2000, -2000, testTaxYear.toString, LocalDate.now().toString))
        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, testTaxYear.toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

        Then("The user is redirected to")
        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }
}
