/*
 * Copyright 2017 HM Revenue & Customs
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

import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.core.PaymentJourneyModel
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndUkProperty

class PaymentControllerISpec extends ControllerISpecHelper {

  val url: String = "/pay-api/mtd-income-tax/sa/journey/start"

  val submissionJson: JsValue = Json.parse(
    s"""
       |{
       | "utr": "1234567890",
       | "amountInPence": 10000,
       | "returnUrl": "${appConfig.paymentRedirectUrl}",
       | "backUrl": "${appConfig.paymentRedirectUrl}"
       |}
    """.stripMargin
  )

  val agentSubmissionJson: JsValue = Json.parse(
    s"""
       |{
       | "utr": "1234567890",
       | "amountInPence": 10000,
       | "returnUrl": "${appConfig.agentPaymentRedirectUrl}",
       | "backUrl": "${appConfig.agentPaymentRedirectUrl}"
       |}
    """.stripMargin
  )

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/payment?amountInPence=10000"
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
              "redirect the user correctly" when {
                "the payments api responds with a 200 and valid json" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    multipleBusinessesAndUkProperty
                  )
                  IncomeTaxViewChangeStub.stubPayApiResponse(
                    url,
                    201,
                    Json.toJson(PaymentJourneyModel("id", "redirect-url"))
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyStubPayApi(
                    url,
                    if (mtdUserRole == MTDIndividual) submissionJson else agentSubmissionJson
                  )
                  res.status shouldBe 303
                  res.header("Location") shouldBe Some("redirect-url")
                }
              }

              "return an internal server error" when {
                "the payments api responds with a 500" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK,
                    multipleBusinessesAndUkProperty
                  )
                  IncomeTaxViewChangeStub.stubPayApiResponse(
                    url,
                    500,
                    Json.toJson(PaymentJourneyModel("id", "redirect-url"))
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue
                  IncomeTaxViewChangeStub.verifyStubPayApi(
                    url,
                    if (mtdUserRole == MTDIndividual) submissionJson else agentSubmissionJson
                  )
                  res.status shouldBe 500
                }
              }
            }
          }
          testAuthFailures(path, mtdUserRole)
        }
      }
  }
}
