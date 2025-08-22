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

package controllers.claimToAdjustPoa

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.claimToAdjustPoa.{OtherIncomeLower, PoaAmendmentData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class AmendablePoaControllerISpec extends ControllerISpecHelper {

  private val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  def getPath(mtdUserRole: MTDUserRole) = {
    val pathStart = if(mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/start"
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
            s"render the Adjusting your payments on account page" when {
              "there is no active session" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
              }
              "there is an active session and journeyCompleted = false" in {
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                )

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                await(sessionService.setMongoData(Some(PoaAmendmentData(
                  poaAdjustmentReason = Some(OtherIncomeLower),
                  newPoaAmount = Some(BigDecimal(3333))
                ))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
                await(sessionService.getMongo) shouldBe Right(Some(PoaAmendmentData(poaAdjustmentReason = Some(OtherIncomeLower), newPoaAmount = Some(3333))))
              }

              "there is an active session and journeyCompleted = true" in {
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                )

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                await(sessionService.setMongoData(Some(PoaAmendmentData(
                  poaAdjustmentReason = Some(OtherIncomeLower),
                  newPoaAmount = Some(BigDecimal(1111)),
                  journeyCompleted = true
                ))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
                await(sessionService.getMongo) shouldBe Right(Some(PoaAmendmentData(None, None)))
              }
            }

            s"return $INTERNAL_SERVER_ERROR" when {
              "no non-crystallised financial details are found" in {
                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testEmptyFinancialDetailsModelJson
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testEmptyFinancialDetailsModelJson
                )

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
