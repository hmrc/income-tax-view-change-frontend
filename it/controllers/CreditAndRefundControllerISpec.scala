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

import audit.models.{ChargeSummaryAudit, ClaimARefundAuditModel}
import auth.MtdItUser
import config.featureswitch.{CreditsRefundsRepay, CutOverCredits, MFACreditsAndDebits}
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.BalanceDetails
import play.api.http.Status.OK
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.{getCurrentTaxYearEnd, testMtditid, testNino, testTaxYear}
import testConstants.FinancialDetailsIntegrationTestConstants.documentDetailWithDueDateFinancialDetailListModel
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, noPropertyOrBusinessResponse, propertyOnlyResponse, propertyOnlyResponseWithMigrationData, singleBusinessResponse, testValidFinancialDetailsModelCreditAndRefundsJson}

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  val testTaxYear: Int = getCurrentTaxYearEnd.getYear
  val testPreviousTaxYearAsString: String = (getCurrentTaxYearEnd.getYear - 1).toString

  "Navigating to /report-quarterly/income-and-expenses/view/credit-and-refunds" should {
    "display the credit and refund page" when {
      "a valid response is received" in {
        enable(CreditsRefundsRepay)
        enable(MFACreditsAndDebits)
        enable(CutOverCredits)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelCreditAndRefundsJson(-2000, -2000, testTaxYear.toString, LocalDate.now().plusYears(1).toString))


        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

        AuditStub.verifyAuditEvent(ClaimARefundAuditModel(
          MtdItUser(
            testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
            Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()),
          balanceDetails = Some(BalanceDetails(BigDecimal(1.00), BigDecimal(2.00), BigDecimal(3.00), Some(BigDecimal(5.00)), Some(BigDecimal(3.00)), Some(BigDecimal(2.00)), None)),
          creditDocuments = List(
            documentDetailWithDueDateFinancialDetailListModel(originalAmount = Some(-2000), outstandingAmount = Some(-2000), mainType = Some("ITSA Cutover Credits")),
            documentDetailWithDueDateFinancialDetailListModel(originalAmount = Some(-2000), outstandingAmount = Some(-2000), mainType = Some("ITSA Cutover Credits")),
            documentDetailWithDueDateFinancialDetailListModel(originalAmount = Some(-2000), outstandingAmount = Some(-2000), mainType = Some("ITSA Cutover Credits")),
            documentDetailWithDueDateFinancialDetailListModel(originalAmount = Some(-2000), outstandingAmount = Some(-2000), mainType = Some("ITSA Overpayment Relief"))
          )
        ))

        res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-hmrc-adjustment") + " 0"),
          elementAttributeBySelector("#credit-and-refund-0", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 1"),
          elementAttributeBySelector("#credit-and-refund-1", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 2"),
          elementAttributeBySelector("#credit-and-refund-2", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " + messagesAPI("credit-and-refund.credit-from-hmrc-title-prt-1") + " " + messagesAPI("credits.drop-down-list.credit-from-an-earlier-tax-year") + " 3"),
          elementAttributeBySelector("#credit-and-refund-3", "href")(s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/2023"),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£3.00 " + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£2.00 " + messagesAPI("credit-and-refund.refundProgress-prt-2")),
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