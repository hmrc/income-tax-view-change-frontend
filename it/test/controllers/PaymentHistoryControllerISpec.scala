/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.models.PaymentHistoryResponseAuditModel
import auth.MtdItUser
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.PaymentHistoryRefunds
import models.financialDetails.Payment
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._

import java.time.LocalDate

class PaymentHistoryControllerISpec extends ControllerISpecHelper {

  val payments: List[Payment] = List(
    Payment(reference = Some("payment1"), amount = Some(100.00), outstandingAmount = None, method = Some("method"),
      documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"), dueDate = Some(LocalDate.parse("2018-04-25")),
      documentDate = LocalDate.parse("2018-04-25"), transactionId = Some("DOCID01"),
      mainType = Some("SA Balancing Charge")),
    Payment(reference = Some("mfa1"), amount = Some(-10000.00), outstandingAmount = None, method = Some("method"),
      documentDescription = Some("TRM New Charge"), lot = None, lotItem = None, dueDate = None,
      documentDate = LocalDate.parse("2018-04-25"), transactionId = Some("AY777777202206"),
      mainType = Some("ITSA Overpayment Relief")),
    Payment(reference = Some("cutover1"), amount = Some(-10000.00), outstandingAmount = None, method = Some("method"),
      documentDescription = None, lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2018-04-25")), documentDate = LocalDate.parse("2018-04-25"),
      transactionId = Some("AY777777202206"),
      mainType = Some("ITSA Cutover Credits")),
    Payment(reference = Some("bcc"), amount = Some(-10000.00), outstandingAmount = None, method = Some("method"),
      documentDescription = None, lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2018-04-25")), documentDate = LocalDate.parse("2018-04-25"),
      transactionId = Some("AY777777202203"),
      mainType = Some("SA Balancing Charge Credit"))
  )

  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val testUser: MTDUserRole => MtdItUser[_] = mtdUserRole => getTestUser(mtdUserRole, paymentHistoryBusinessAndPropertyResponse)

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/payment-refund-history"
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

            s"render the payment history page" that {
              "has payment history title" when {
                "the PaymentHistoryRefunds FS is disabled" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
                  IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, payments)

                  whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                    result should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "paymentHistory.heading"),
                      elementTextBySelector("#refundstatus")(""),
                    )

                    verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser(mtdUserRole), payments).detail)
                  }
                }
              }

              "has payment and refund history title" when {

                "the payment is from an earlier tax year description when CutOverCreditsEnabled and credit is defined" in {
                  enable(PaymentHistoryRefunds)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
                  IncomeTaxViewChangeStub.stubGetPaymentsResponse(testNino, s"$twoPreviousTaxYearEnd-04-06", s"$previousTaxYearEnd-04-05")(OK, payments)

                  whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                    result should have(
                      httpStatus(OK),
                      pageTitle(mtdUserRole, "paymentHistory.paymentAndRefundHistory.heading"),
                      elementTextBySelector("h1")(messagesAPI("paymentHistory.paymentAndRefundHistory.heading"))
                    )
                    if(mtdUserRole == MTDIndividual) {
                      result should have(
                        elementTextBySelector("#refundstatus")(messagesAPI("paymentHistory.check-refund-1") + " " +
                          messagesAPI("paymentHistory.check-refund-2") + " " + messagesAPI("paymentHistory.check-refund-3"))
                      )
                    }

                    verifyAuditContainsDetail(PaymentHistoryResponseAuditModel(testUser(mtdUserRole), payments).detail)
                  }
                }
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
