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

import audit.models.RefundToTaxPayerResponseAuditModel
import auth.MtdItUser
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.PaymentHistoryRefunds
import models.core.Nino
import models.repaymentHistory._
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.paymentHistoryBusinessAndPropertyResponse

import java.time.LocalDate

class RefundToTaxPayerControllerISpec extends ControllerISpecHelper {

  val testRepaymentHistoryModel: RepaymentHistoryModel = RepaymentHistoryModel(
    List(
      RepaymentHistory(
        Some(705.2),
        705.2,
        Some("BACS"),
        Some(12345),
        Some(
          Vector(
            RepaymentItem(
              Vector(
                RepaymentSupplementItem(
                  Some("002420002231"),
                  Some(3.78),
                  Some(LocalDate.of(2021, 7, 31)),
                  Some(LocalDate.of(2021, 9, 15)),
                  Some(2.01)
                ),
                RepaymentSupplementItem(
                  Some("002420002231"),
                  Some(2.63),
                  Some(LocalDate.of(2021, 9, 15)),
                  Some(LocalDate.of(2021, 10, 24)),
                  Some(1.76)
                ),
                RepaymentSupplementItem(
                  Some("002420002231"),
                  Some(3.26),
                  Some(LocalDate.of(2021, 10, 24)),
                  Some(LocalDate.of(2021, 11, 30)),
                  Some(2.01)
                )
              )
            )
          )
        ),
        Some(LocalDate.of(2021, 7, 23)),
        Some(LocalDate.of(2021, 7, 21)),
        "000000003135",
        status = RepaymentHistoryStatus("A")
      )
    )
  )
  val repaymentRequestNumber: String = "023942042349"
  val testNino:               String = "AA123456A"

  lazy val testUser: MTDUserRole => MtdItUser[_] = mtdUserRole =>
    getTestUser(mtdUserRole, paymentHistoryBusinessAndPropertyResponse)

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/refund-to-taxpayer/$repaymentRequestNumber"
  }

  mtdAllRoles.foreach {
    case mtdUserRole =>
      val path              = getPath(mtdUserRole)
      val additionalCookies = getAdditionalCookies(mtdUserRole)
      s"GET $path" when {
        s"a user is a $mtdUserRole" that {
          "is authenticated, with a valid enrolment" should {
            if (mtdUserRole == MTDSupportingAgent) {
              testSupportingAgentAccessDenied(path, additionalCookies)
            } else {

              s"audit and render the refund to tax payer page" when {
                "the payment history refunds feature switch is enabled" in {
                  enable(PaymentHistoryRefunds)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    paymentHistoryBusinessAndPropertyResponse
                  )
                  IncomeTaxViewChangeStub.stubGetRepaymentHistoryByRepaymentId(Nino(testNino), repaymentRequestNumber)(
                    OK,
                    testRepaymentHistoryModel
                  )

                  val result = buildGETMTDClient(path, additionalCookies).futureValue

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "refund-to-taxpayer.heading")
                  )
                  AuditStub.verifyAuditEvent(
                    RefundToTaxPayerResponseAuditModel(testRepaymentHistoryModel)(testUser(mtdUserRole))
                  )
                }
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
  }
}
