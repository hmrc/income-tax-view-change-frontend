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
import forms.adjustPoa.SelectYourReasonFormProvider
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PoaAmendmentData, SelectYourReason}
import models.core.NormalMode
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class SelectYourReasonControllerISpec extends ControllerISpecHelper {

  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]
  lazy val formProvider = app.injector.instanceOf[SelectYourReasonFormProvider]


  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.setMongoData(None))
  }

  def formData(answer: Option[SelectYourReason]): Map[String, Seq[String]] = {
    answer.fold(Map.empty[String, Seq[String]])(
      selection => {
        formProvider.apply()
          .fill(selection)
          .data.map { case (k, v) => (k, Seq(v)) }
      }
    )
  }

  def getPath(mtdUserRole: MTDUserRole): String = {
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/select-your-reason"
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
            s"render the Select your reason page" when {
              "user has entered an amount lower than current amount" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData())))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
              }

              "user has entered an amount lower than current amount but PoA adjustment reason is populated in session data" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower)))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
              }
            }

            s"return status $SEE_OTHER" when {

              "user has entered an amount higher than current amount" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(500, 500, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(500, 500, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
                )
                await(sessionService.setMongoData(Some(PoaAmendmentData(newPoaAmount = Some(1500.0)))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(
                  PoaAmendmentData(
                    newPoaAmount = Some(1500.0),
                    poaAdjustmentReason = Some(Increase))))
              }

              "journeyCompleted flag is true and the user tries to access the page" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(None, None, journeyCompleted = true))))

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
                await(sessionService.setMongoData(Some(PoaAmendmentData())))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "no adjust POA session is found" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
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

    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "redirect to EnterPOAAmount" when {
              s"when originalAmount >= relevantAmount" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData())))

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(Some(MainIncomeLower))).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.EnterPoaAmountController.show(isAgent, NormalMode).url)
                )
                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(MainIncomeLower), None)))
              }
            }

            "redirect to Check Your Answers page" when {
              "when originalAmount < relevantAmount" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData())))

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(Some(MainIncomeLower))).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )
                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(MainIncomeLower), None)))
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
                await(sessionService.setMongoData(Some(PoaAmendmentData())))

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(Some(MainIncomeLower))).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "no adjust POA session is found" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
                )

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(Some(MainIncomeLower))).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole, Some(Map.empty))
      }
    }
  }
}

