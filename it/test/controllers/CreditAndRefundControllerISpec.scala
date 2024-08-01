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
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.ANewCreditAndRefundModel
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

import java.time.LocalDate

class CreditAndRefundControllerISpec extends ComponentSpecBase {

  val isAgent = false

  lazy val fixedDate : LocalDate = LocalDate.of(2020, 11, 29)

  override def beforeEach(): Unit = {
    super.beforeEach()
    if(isAgent){
      stubAuthorisedAgentUser(authorised = true, clientMtdId = testMtditid)
    }
  }

  "Navigating to /report-quarterly/income-and-expenses/view/credit-and-refunds" should {

    val testTaxYear: Int = 2023
    val testPreviousTaxYear: Int = 2022

    "display the credit and refund page with all credits/refund types and audit event" when {

      "a valid response is received and feature switches are enabled" in {
        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        Given("I wiremock stub a successful Income Source Details response with multiple business and a property")
        IncomeTaxViewChangeStub
          .stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse
          .copy(yearOfMigration = Some(s"${testTaxYear}")))

        val creditsModel = ANewCreditAndRefundModel()
          .withAvailableCredit(5.0)
          .withAllocatedCredit(45.0)
          .withFirstRefund(3.0)
          .withSecondRefund(2.0)
          .withCutoverCredit(LocalDate.of(testPreviousTaxYear, 3, 29), 2000.0)
          .withCutoverCredit(LocalDate.of(testPreviousTaxYear, 3, 29), 2000.0)
          .withPayment(LocalDate.of(testPreviousTaxYear, 3, 29), 500.0)
          .withRepaymentInterest(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withBalancingChargeCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withMfaCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withCutoverCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .get()

        val json =  Json.toJson(creditsModel)

        And("I wiremock stub a successful Financial Details response with credits and refunds")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsCreditsByDateRange(
          testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          json)

        val res = isAgent match {
          case true => IncomeTaxViewChangeFrontend.getCreditAndRefunds(isAgent = true, additionalCookies = clientDetailsWithConfirmation)
          case false => IncomeTaxViewChangeFrontend.getCreditAndRefunds()
        }

        Then("I verify Income Source Details was called")
        verifyIncomeSourceDetailsCall(testMtditid)

        Then("I verify Financial Details was called")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsCreditsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

        val mtdUser = if(isAgent) {
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
            None, Some(Agent), Some("1"))(FakeRequest())
        } else {
          MtdItUser(
            testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, None, Some("1234567890"),
            Some("12345-credId"), Some(Individual), None
          )(FakeRequest())
        }

        Then("I verify the audit event was as expected")
        AuditStub.verifyAuditEvent(ClaimARefundAuditModel(creditsModel = creditsModel)(mtdUser))

          res should have(
          httpStatus(OK),
          elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.row.repaymentInterest-2") + s" $testPreviousTaxYear to $testTaxYear tax year"),

          elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-balancing-charge-prt-1") + s" $testPreviousTaxYear to $testTaxYear tax year"),

          elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-adjustment-prt-1") + s" $testPreviousTaxYear to $testTaxYear tax year"),

          elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + s" $testPreviousTaxYear to $testTaxYear tax year"),

          elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + s" ${testPreviousTaxYear - 1} to $testPreviousTaxYear tax year"),

          elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£2,000.00 " +
            messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + s" ${testPreviousTaxYear - 1} to $testPreviousTaxYear tax year"),

          elementTextBySelectorList("#main-content", "li:nth-child(7)", "p")(expectedValue = "£500.00 " +
            messagesAPI("credit-and-refund.payment") + " 29 March 2022" ),

          elementTextBySelectorList("#main-content", "li:nth-child(8)", "p")(expectedValue = "£3.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),

          elementTextBySelectorList("#main-content", "li:nth-child(9)", "p")(expectedValue = "£2.00 "
            + messagesAPI("credit-and-refund.refundProgress-prt-2")),
          if(isAgent) {
            pageTitleAgent("credit-and-refund.heading")
          } else {
            pageTitleIndividual("credit-and-refund.heading")
          }
        )
      }
    }

    "redirect to custom not found page" when {
      "the feature switch is off" in {
        disable(CreditsRefundsRepay)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse
          .copy(yearOfMigration = Some(s"${testTaxYear}")))

        val json =  Json.toJson(ANewCreditAndRefundModel()
          .withAvailableCredit(5.0)
          .withAllocatedCredit(45.0)
          .withFirstRefund(3.0)
          .withSecondRefund(2.0)
          .withCutoverCredit(LocalDate.of(testPreviousTaxYear, 3, 29), 2000.0)
          .withCutoverCredit(LocalDate.of(testPreviousTaxYear, 3, 29), 2000.0)
          .withPayment(LocalDate.of(testPreviousTaxYear, 3, 29), 500.0)
          .withRepaymentInterest(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withBalancingChargeCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withMfaCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withCutoverCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .get())

        And("I wiremock stub a successful Financial Details response with credits and refunds")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsCreditsByDateRange(
          testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          json)

        val res = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsCreditsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

        res should have(
          httpStatus(OK),
          pageTitleIndividual(messagesAPI("error.custom.heading"))
        )
      }
    }

    "redirect to unauthorised page" when {
      "the user is not authenticated" in {

        if(isAgent) {
          stubAuthorisedAgentUser(authorised = false)
        } else {
          isAuthorisedUser(authorised = false)
        }

        enable(CreditsRefundsRepay)
        enable(CutOverCredits)
        enable(MFACreditsAndDebits)

        Given("I wiremock stub a successful Income Source Details response with multiple business and a property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse
          .copy(yearOfMigration = Some(s"${testTaxYear}")))

        val creditsModel = ANewCreditAndRefundModel()
          .withAvailableCredit(5.0)
          .withAllocatedCredit(45.0)
          .withFirstRefund(3.0)
          .withSecondRefund(2.0)
          .withCutoverCredit(LocalDate.of(testPreviousTaxYear, 3, 29), 2000.0)
          .withCutoverCredit(LocalDate.of(testPreviousTaxYear, 3, 29), 2000.0)
          .withPayment(LocalDate.of(testPreviousTaxYear, 3, 29), 500.0)
          .withRepaymentInterest(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withBalancingChargeCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withMfaCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .withCutoverCredit(LocalDate.of(testTaxYear, 3, 29), 2000.0)
          .get()

        val json =  Json.toJson(creditsModel)

        And("I wiremock stub a successful Financial Details response with credits and refunds")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsCreditsByDateRange(
          testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")(OK,
          json)

        val result = IncomeTaxViewChangeFrontend.getCreditAndRefunds()

        Then("The user is redirected to")
        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }
}