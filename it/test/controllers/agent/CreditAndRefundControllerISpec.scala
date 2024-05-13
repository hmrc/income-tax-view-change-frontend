/*
 * Copyright 2022 HM Revenue & Customs
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

import audit.models.ClaimARefundAuditModel
import auth.MtdItUser
import config.featureswitch.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.core.AccountingPeriodModel
import models.financialDetails.BalanceDetails
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.FinancialDetailsIntegrationTestConstants.documentDetailWithDueDateFinancialDetailListModel
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson, testValidFinancialDetailsModelJson}
import testConstants.OutstandingChargesIntegrationTestConstants.validOutStandingChargeResponseJsonWithAciAndBcdCharges
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  lazy val fixedDate : LocalDate = LocalDate.of(2020, 11, 29)

  "Navigating to /report-quarterly/income-and-expenses/view/agents/credit-and-refunds" should {

    val testTaxYear: Int = getCurrentTaxYearEnd.getYear
    val testPreviousTaxYear: Int = getCurrentTaxYearEnd.getYear - 1

    "display the credit and refund page with all credits/refund types and audit event" when {
      "a valid response is received and feature switches are enabled" in {
        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with multiple business and a property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        And("I wiremock stub a successful Financial Details response with credits and refunds")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testPreviousTaxYear.toString, fixedDate.plusYears(1).toString))

        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

        Then("I verify Income Source Details was called")
        verifyIncomeSourceDetailsCall(testMtditid)

        Then("I verify Financial Details was called")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

        Then("I verify the audit event was as expected")
        AuditStub.verifyAuditEvent(ClaimARefundAuditModel(
          balanceDetails = Some(BalanceDetails(BigDecimal(1.00), BigDecimal(2.00), BigDecimal(3.00), Some(BigDecimal(5.00)), Some(BigDecimal(1.00)), Some(BigDecimal(3.00)), Some(BigDecimal(2.00)), None)),
          creditDocuments = List(
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("SA Balancing Charge Credit"), mainTransaction=Some("4905")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Cutover Credits"), mainTransaction=Some("6110")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Cutover Credits"), mainTransaction=Some("6110")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Cutover Credits"), mainTransaction=Some("6110")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Overpayment Relief"), mainTransaction=Some("4004"))
          )
        )(MtdItUser(testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          None, Some(Agent), Some("1"))(FakeRequest())))

        res should have(
          httpStatus(OK),

          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-balancing-charge-prt-1") + " " + testTaxYearTo),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-adjustment-prt-1") + " " + testTaxYearTo),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + " " + testTaxYearTo),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + " " + testTaxYearTo),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + " " + testTaxYearTo),

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

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testPreviousTaxYear, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, fixedDate.plusYears(1).toString))


        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds(clientDetailsWithConfirmation)

        Then("I verify Income Source Details was called")
        verifyIncomeSourceDetailsCall(testMtditid)

        Then("I verify Financial Details was called")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

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
            nino = testNino,
            mtdbsa = testMtditid,
            yearOfMigration = None,
            businesses = List(BusinessDetailsModel(
              "testId",
              incomeSource = Some(testIncomeSource),
              Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
              None,
              None,
              Some(getCurrentTaxYearEnd),
              None,
              address = Some(address),
              cashOrAccruals = false
            )),
            properties = Nil
          )
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelJson(
            -2000, -2000, testTaxYear.toString, fixedDate.toString))
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