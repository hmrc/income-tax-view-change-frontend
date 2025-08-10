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
import models.claimToAdjustPoa.PoaAmendmentData
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class WhatYouNeedToKnowControllerISpec extends ControllerISpecHelper {

  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.setMongoData(Some(PoaAmendmentData(poaAdjustmentReason = None, newPoaAmount = None))))
  }

  def getPath(mtdUserRole: MTDUserRole): String = {
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/what-you-need-to-know"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    val isAgent = mtdUserRole != MTDIndividual
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            s"render the What you need to know page" when {
              s"User has originalAmount >= relevantAmount" in {
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
                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData()))
                val document: Document = Jsoup.parse(result.body)
                val continueButton = document.getElementById("continue")
                continueButton.attr("href") shouldBe routes.SelectYourReasonController.show(isAgent, NormalMode).url

                val increaseAfterPaymentContent = Option(document.getElementById("p6"))
                increaseAfterPaymentContent.isDefined shouldBe false
              }

              "User has originalAmount < relevantAmount" that {
                "is not paid or partially paid" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                    OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                    OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
                  )

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  result should have(
                    httpStatus(OK)
                  )
                  sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData()))
                  val document: Document = Jsoup.parse(result.body)
                  val continueButton = document.getElementById("continue")
                  continueButton.attr("href") shouldBe routes.EnterPoaAmountController.show(isAgent, NormalMode).url

                  val increaseAfterPaymentContent = Option(document.getElementById("p6"))
                  increaseAfterPaymentContent.isDefined shouldBe false
                }

                "is partially paid should display additional content" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                    OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                    OK, testValidFinancialDetailsModelJson(
                      originalAmount = 2000,
                      outstandingAmount = 1000,
                      taxYear = (testTaxYear - 1).toString,
                      dueDate = testDate.toString,
                      poaRelevantAmount = Some(3000))
                  )
                  IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                    OK, testValidFinancialDetailsModelJson(
                      originalAmount = 2000,
                      outstandingAmount = 1000,
                      taxYear = (testTaxYear - 1).toString,
                      dueDate = testDate.toString,
                      poaRelevantAmount = Some(3000))
                  )

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  result should have(
                    httpStatus(OK)
                  )
                  sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData()))
                  val document: Document = Jsoup.parse(result.body)
                  val continueButton = document.getElementById("continue")
                  continueButton.attr("href") shouldBe routes.EnterPoaAmountController.show(isAgent, NormalMode).url

                  val increaseAfterPaymentContent = Option(document.getElementById("p6"))
                  increaseAfterPaymentContent.isDefined shouldBe true
                }
              }

              "user has POAs that are fully paid" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(
                    originalAmount = 2000,
                    outstandingAmount = 0,
                    taxYear = (testTaxYear - 1).toString,
                    dueDate = testDate.toString,
                    poaRelevantAmount = Some(3000))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(
                    originalAmount = 2000,
                    outstandingAmount = 0,
                    taxYear = (testTaxYear - 1).toString,
                    dueDate = testDate.toString,
                    poaRelevantAmount = Some(3000))
                )

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData()))
                val document: Document = Jsoup.parse(result.body)
                val continueButton = document.getElementById("continue")
                continueButton.attr("href") shouldBe routes.EnterPoaAmountController.show(isAgent, NormalMode).url

                val increaseAfterPaymentContent = Option(document.getElementById("p6"))
                increaseAfterPaymentContent.isDefined shouldBe true
              }
            }

            s"return status $SEE_OTHER and redirect to the You Cannot Go Back page" when {
              "journeyCompleted flag is true and the user tries to access the page" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(poaAdjustmentReason = None, newPoaAmount = None, journeyCompleted = true))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
                )
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
