
package controllers.agent

import config.featureswitch.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.{testTaxYear, _}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson, testValidFinancialDetailsModelJson}
import testConstants.OutstandingChargesIntegrationTestConstants.validOutStandingChargeResponseJsonWithAciAndBcdCharges

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  "Navigating to /report-quarterly/income-and-expenses/view/agents/credit-and-refunds" should {

    val financialDetailsJson: JsValue = testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000,
      testTaxYear.toString, LocalDate.now().plusYears(1).toString, accruingInterestAmount = Some(2.37))

    def setupCreditAndRefundControllerITests(financialDetailsJson: JsValue = financialDetailsJson): WSResponse = {

      stubAuthorisedAgentUser(authorised = true)

      Given("a success response from getIncomeSourceDetails API")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

      And("a success response from getFinancialDetails API")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, financialDetailsJson)

      val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

      res
    }

    "display Credit from HMRC adjustments, Credits from Interest Accrued and Credits from an earlier tax year in the correct order" when {
      "valid responses are received and feature switches are ENABLED" in {
        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        val res = setupCreditAndRefundControllerITests()

        res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "h2:nth-of-type(1)")(expectedValue = messagesAPI("credit-and-refund.subHeading.has-credits-1") + " £5.00 "
            + messagesAPI("credit-and-refund.agent.subHeading.has-credits-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-2") + " 0"),
          elementAttributeBySelector("#credit-and-refund-1", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2.37 " + messagesAPI("credit-and-refund.credit-interest-accrued-prt-1") + " " + messagesAPI("credit-and-refund.credit-interest-accrued-prt-2") + " 0a"),
          elementAttributeBySelector("#credit-and-refund-2", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-0", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-3", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " +
            messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 3"),
          elementAttributeBySelector("#credit-and-refund-3", "href")("/report-quarterly/income-and-expenses/view/agents/credits-from-hmrc/2018"),

          elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(7)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),
          pageTitleAgent("credit-and-refund.heading")
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
