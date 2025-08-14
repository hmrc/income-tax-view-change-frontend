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

package controllers.claimToAdjustPoa

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.JsValue
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesResponse
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.{testTaxYearPoa, validFinancialDetailsResponseBody, validSession}

class YouCannotGoBackControllerISpec extends ControllerISpecHelper {

  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.setMongoData(None))
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      OK, multipleBusinessesResponse
    )
  }

  def stubFinancialDetailsResponse(response: JsValue = validFinancialDetailsResponseBody(testTaxYearPoa)): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 1}-04-06", s"$testTaxYearPoa-04-05")(OK, response)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 2}-04-06", s"${testTaxYearPoa - 1}-04-05")(OK, response)
  }

  def getPath(mtdUserRole: MTDUserRole): String = {
    val pathStart = if(mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/poa-updated-cannot-go-back"
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
            s"render the You cannot go back page" when {
              s"the journeyCompleted flag is set to false" in {
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse()
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "claimToAdjustPoa.youCannotGoBack.heading")
                )
              }

              s"the journeyCompleted flag is set to true" in {
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse()
                await(sessionService.setMongoData(Some(validSession.copy(journeyCompleted = true))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "claimToAdjustPoa.youCannotGoBack.heading")
                )
              }
            }

            s"return status $INTERNAL_SERVER_ERROR" when {
              "session is missing" in {
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse()

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "poa data is missing" in {
                stubAuthorised(mtdUserRole)
                await(sessionService.setMongoData(Some(validSession.copy(journeyCompleted = true))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
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
