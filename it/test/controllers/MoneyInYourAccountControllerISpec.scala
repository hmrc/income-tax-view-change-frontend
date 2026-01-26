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
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.CreditsRefundsRepay
import models.core.ErrorModel
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsValue, Json}
import testConstants.ANewCreditAndRefundModel
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndPropertyResponse

import java.time.LocalDate

class MoneyInYourAccountControllerISpec extends ControllerISpecHelper {

  lazy val fixedDate : LocalDate = LocalDate.of(2020, 11, 29)

  val testTaxYear: Int = 2023

  val testPreviousTaxYear: Int = 2022

  val validResponseModel = ANewCreditAndRefundModel()
    .withAvailableCredit(5.0)
    .withAllocatedFutureCredit(45.0)
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

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/money-in-your-account"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "render the credit and refund page" that {
              "has all credits/refund types" when {
                "a valid response is received and feature switches are enabled" in new CustomFinancialDetailsResponse(
                  Seq(FinancialDetailsResponse(
                    taxYear = testTaxYear,
                    code = OK,
                    json = Json.toJson(validResponseModel))),
                  mtdUserRole) {

                  val res = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsCreditsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")
                  AuditStub.verifyAuditEvent(ClaimARefundAuditModel(creditsModel = validResponseModel)(mtdUser))

                  res should have(
                    httpStatus(OK),
                    //TODO: Re-enable as part of MISUV-10631
//                    elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.row.repaymentInterest-2") + s" $testPreviousTaxYear to $testTaxYear tax year"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(2)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.credit-from-balancing-charge-prt-1") + s" $testPreviousTaxYear to $testTaxYear tax year"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(3)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.credit-from-adjustment-prt-1") + s" $testPreviousTaxYear to $testTaxYear tax year"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(4)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + s" $testPreviousTaxYear to $testTaxYear tax year"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(5)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + s" ${testPreviousTaxYear - 1} to $testPreviousTaxYear tax year"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(6)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.credit-from-earlier-tax-year") + s" ${testPreviousTaxYear - 1} to $testPreviousTaxYear tax year"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(7)", "p")(expectedValue = "£500.00 " +
//                      messagesAPI("credit-and-refund.payment") + " 29 March 2022"),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(8)", "p")(expectedValue = "£3.00 "
//                      + messagesAPI("credit-and-refund.refundProgress-prt-2")),
//
//                    elementTextBySelectorList("#main-content", "li:nth-child(9)", "p")(expectedValue = "£2.00 "
//                      + messagesAPI("credit-and-refund.refundProgress-prt-2")),

                    pageTitle(mtdUserRole, "money-in-your-account.heading")
                  )
                }

                "a not found response from the API is received for a single tax year" in new CustomFinancialDetailsResponse(
                  Seq(FinancialDetailsResponse(
                    taxYear = testTaxYear - 1,
                    code = NOT_FOUND,
                    json = Json.toJson(ErrorModel(NOT_FOUND, "Not found"))),
                    FinancialDetailsResponse(
                      taxYear = testTaxYear,
                      code = OK,
                      json = Json.toJson(validResponseModel))),
                  mtdUserRole) {

                  val res = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsCreditsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")
                  AuditStub.verifyAuditEvent(ClaimARefundAuditModel(creditsModel = validResponseModel)(mtdUser))

                  res should have(
                    httpStatus(OK),
                    //TODO: Re-enable as part of MISUV-10631
//                    elementTextBySelectorList("#main-content", "li:nth-child(1)", "p")(expectedValue = "£2,000.00 " +
//                      messagesAPI("credit-and-refund.row.repaymentInterest-2") + s" $testPreviousTaxYear to $testTaxYear tax year"),
//                    elementTextBySelectorList("#main-content", "li:nth-child(8)", "p")(expectedValue = "£3.00 "
//                      + messagesAPI("credit-and-refund.refundProgress-prt-2")),
                    pageTitle(mtdUserRole, "money-in-your-account.heading")
                  )
                }
              }
              "displays 'no money in your account' message" when {

                "a not found response from the API is received" in new CustomFinancialDetailsResponse(
                  Seq(FinancialDetailsResponse(
                    taxYear = testTaxYear,
                    code = NOT_FOUND,
                    json = Json.toJson(ErrorModel(NOT_FOUND, "Not found")))),
                  mtdUserRole) {

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  res should have(
                    httpStatus(OK),
                    elementTextByID("credit-explanation")(expectedValue = "You do not have any money in your account at the moment."),

                    pageTitle(mtdUserRole, "money-in-your-account.heading")
                  )
                }
              }
            }

            "redirect to custom not found page" when {

              "the feature switch is off" in new CustomFinancialDetailsResponse(
                Seq(FinancialDetailsResponse(
                  taxYear = testTaxYear,
                  code = OK,
                  json = Json.toJson(validResponseModel))),
                mtdUserRole,
                enableCreditAndRefunds = false) {
                val res = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsCreditsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

                res should have(
                  httpStatus(OK),
                  pageTitleIndividual(messagesAPI("error.custom.heading"))
                )
              }
            }

            "render the error page" when {

              "an error response from the API is received" in new CustomFinancialDetailsResponse(
                Seq(FinancialDetailsResponse(
                  taxYear = testTaxYear,
                  code = INTERNAL_SERVER_ERROR,
                  json = Json.toJson(ErrorModel(INTERNAL_SERVER_ERROR, "Internal server error")))),
                mtdUserRole) {

                val res = buildGETMTDClient(path, additionalCookies).futureValue
                IncomeTaxViewChangeStub.verifyGetFinancialDetailsCreditsByDateRange(testNino, s"$testPreviousTaxYear-04-06", s"$testTaxYear-04-05")

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR),
                  pageTitle(mtdUserRole, "standardError.heading", isErrorPage = true),
                  elementAttributeBySelector(".govuk-phase-banner__text a", "href")
                  (s"/report-quarterly/income-and-expenses/view${if(mtdUserRole == MTDIndividual) "" else "/agents"}/feedback")
                )
              }

              "an invalid response from the API is received" in new CustomFinancialDetailsResponse(
                Seq(FinancialDetailsResponse(
                  taxYear = testTaxYear,
                  code = OK,
                  json = Json.parse("""{ "invalid": "json" }"""))),
                mtdUserRole) {

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR),

                  pageTitle(mtdUserRole, "standardError.heading", isErrorPage = true)
                )
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }

  case class FinancialDetailsResponse(taxYear: Int, code: Int, json: JsValue)

  class CustomFinancialDetailsResponse(responses: Seq[FinancialDetailsResponse],
                                       mtdUserRole: MTDUserRole,
                                       enableCreditAndRefunds: Boolean = true) {

    if(enableCreditAndRefunds) {
      enable(CreditsRefundsRepay)
    }
    stubAuthorised(mtdUserRole)

    val incomeSources = multipleBusinessesAndPropertyResponse
      .copy(yearOfMigration = Some(s"${responses.map(_.taxYear).min}"))

    val mtdUser: MtdItUser[_] = getTestUser(mtdUserRole, incomeSources)

    IncomeTaxViewChangeStub
      .stubGetIncomeSourceDetailsResponse(testMtditid)(OK, incomeSources)

    responses.foreach(response => {
      val fromYear = {response.taxYear - 1}.toString
      val toYear = {response.taxYear}.toString
      IncomeTaxViewChangeStub.stubGetFinancialDetailsCreditsByDateRange(
        testNino, s"${fromYear}-04-06", s"${toYear}-04-05")(
        response.code,
        response.json)
    })
  }
}