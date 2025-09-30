/*
 * Copyright 2024 HM Revenue & Customs
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

import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.NavBarFs
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import play.api.http.Status.OK
import play.api.libs.json.Json
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.PaymentAllocationIntegrationTestConstants._

class PaymentAllocationControllerISpec extends ControllerISpecHelper with FeatureSwitching {

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )
  val docNumber = "docNumber1"

  val testUser: MTDUserRole => MtdItUser[_] = mtdUserRole =>
    getTestUser(mtdUserRole, paymentHistoryBusinessAndPropertyResponse)

  def getPath(mtdRole: MTDUserRole, documentNum: String = docNumber): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/payment-made-to-hmrc?documentNumber=$documentNum"
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
            s"render the payment allocation page" which {
              "is for non LPI" in {
                disable(NavBarFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

                IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
                IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, "paymentLot", "paymentLotItem")(OK, Json.toJson(testValidPaymentAllocationsModel))

                whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "paymentAllocation.heading"),
                  )

                  verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser(mtdUserRole), paymentAllocationViewModel).detail)
                }
              }
            }

            "shows payment allocation for HMRC adjustment" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              val docNumber = "MA999991A202202"
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)
              IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesHmrcAdjustmentJson)
              IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, "MA999991A", "5")(OK, Json.toJson(testValidNoLpiPaymentAllocationHmrcAdjustment))

              whenReady(buildGETMTDClient(getPath(mtdUserRole, docNumber), additionalCookies)) { result =>
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "paymentAllocation.heading"),
                  elementTextBySelector("tbody")("31 Jan 2021 HMRC adjustment Tax year 2021 to 2022 Tax year 2021 to 2022 £800.00"),
                )

                verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser(mtdUserRole), paymentAllocationViewModelHmrcAdjustment).detail)
              }
            }

            s"is for LPI" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, paymentHistoryBusinessAndPropertyResponse)

              IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(nino = testNino, from = s"${getCurrentTaxYearEnd.getYear - 1}-04-06",
                to = s"${getCurrentTaxYearEnd.getYear}-04-05")(OK, testValidFinancialDetailsModelJson(10.34, 1.2))

              IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, docNumber)(OK, validPaymentAllocationChargesJson)
              IncomeTaxViewChangeStub.stubGetPaymentAllocationResponse(testNino, "paymentLot", "paymentLotItem")(OK, Json.toJson(testValidLpiPaymentAllocationsModel))
              IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000872")(OK, validPaymentAllocationChargesJson)
              IncomeTaxViewChangeStub.stubGetFinancialsByDocumentId(testNino, "1040000873")(OK, validPaymentAllocationChargesJson)

              whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "paymentAllocation.heading"),
                  elementAttributeBySelector("#payment-allocation-0 a", "href")(
                    "/report-quarterly/income-and-expenses/view" + {if(mtdUserRole != MTDIndividual) "/agents" else ""} +"/tax-years/9999/charge?id=PAYID01&isInterestCharge=true"),
                  elementTextBySelector("#payment-allocation-0 a")(s"${messagesAPI("paymentAllocation.paymentAllocations.balancingCharge.text")} ${messagesAPI("paymentAllocation.taxYear", "9998", "9999")}")
                )

                verifyAuditContainsDetail(PaymentAllocationsResponseAuditModel(testUser(mtdUserRole), lpiPaymentAllocationViewModel).detail)
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}
