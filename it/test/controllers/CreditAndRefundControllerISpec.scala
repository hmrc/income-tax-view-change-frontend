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

package controllers

import audit.models.ClaimARefundAuditModel
import auth.MtdItUser
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import models.financialDetails.BalanceDetails
import play.api.http.Status.OK
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testTaxYearTo}
import testConstants.FinancialDetailsIntegrationTestConstants.documentDetailWithDueDateFinancialDetailListModel
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  lazy val fixedDate : LocalDate = LocalDate.of(2020, 11, 29)

  "Navigating to /report-quarterly/income-and-expenses/view/credit-and-refunds" should {

    val testTaxYear: Int = getCurrentTaxYearEnd.getYear
    val testPreviousTaxYear: Int = getCurrentTaxYearEnd.getYear - 1

    "display the credit and refund page with all credits/refund types and audit event" when {

      "a valid response is received and feature switches are enabled" in {
        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        Given("I wiremock stub a successful Income Source Details response with multiple business and a property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        And("I wiremock stub a successful Financial Details response with credits and refunds")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testPreviousTaxYear.toString, fixedDate.plusYears(1).toString))

        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

        Then("I verify Income Source Details was called")
        verifyIncomeSourceDetailsCall(testMtditid)

        Then("I verify Financial Details was called")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

        Then("I verify the audit event was as expected")
        AuditStub.verifyAuditEvent(ClaimARefundAuditModel(
          balanceDetails = Some(BalanceDetails(BigDecimal(1.00), BigDecimal(2.00), BigDecimal(3.00), Some(BigDecimal(5.00)), Some(BigDecimal(1.00)), Some(BigDecimal(3.00)), Some(BigDecimal(2.00)), None)),
          creditDocuments = List(
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -500, outstandingAmount = -500, dueDate = Some(LocalDate.of(2022, 3, 29)), mainType = Some("Payment on Account"), mainTransaction=Some("0060")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("SA Balancing Charge Credit"), mainTransaction=Some("4905")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Cutover Credits"), mainTransaction=Some("6110")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Cutover Credits"), mainTransaction=Some("6110")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Cutover Credits"), mainTransaction=Some("6110")),
            documentDetailWithDueDateFinancialDetailListModel(taxYear = testPreviousTaxYear, originalAmount = -2000, outstandingAmount = -2000, mainType = Some("ITSA Overpayment Relief"), mainTransaction=Some("4004"))
          )
        )(MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
          Some("12345-credId"), Some(Individual), None
        )(FakeRequest())))

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

            elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£500.00 " +
              messagesAPI("credit-and-refund.payment") + " 29 March 2022" ),

          elementTextBySelectorList("#main-content", "li:nth-child(7)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(8)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),
          pageTitleIndividual("credit-and-refund.heading")

        )
      }
    }

    "redirect to custom not found page" when {
      "the feature switch is off" in {
        disable(CreditsRefundsRepay)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testPreviousTaxYear, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, fixedDate.plusYears(1).toString))

        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messagesAPI("error.custom.heading"))
        )
      }
    }
  }
}