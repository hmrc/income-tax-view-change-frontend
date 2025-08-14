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
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PoaAmendmentData}
import models.core.{CheckMode, NormalMode}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testChargeHistoryJson, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class EnterPoaAmountControllerISpec extends ControllerISpecHelper {

  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  def msg(key: String) = msgs(s"claimToAdjustPoa.enterPoaAmount.$key")

  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.setMongoData(None))
  }

  def formData(newPoaAmount: BigDecimal): Map[String, Seq[String]] = {
    Map(
      "poa-amount" -> Seq(newPoaAmount.toString())
    )
  }

  def getPath(mtdUserRole: MTDUserRole, isChange: Boolean): String = {
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    if (isChange) {
      pathStart + "/adjust-poa/change-poa-amount"
    } else {
      pathStart + "/adjust-poa/enter-poa-amount"
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole, false)
    val changePath = getPath(mtdUserRole, true)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    val isAgent = mtdUserRole != MTDIndividual
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            s"render the Enter PoA Amount page" when {
              "the user has not previously adjusted their PoA" in {
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

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByClass("govuk-table__head")(msg("chargeHeading") + " " + msg("amountPreviousHeading"))
                )
              }
              "User is authorised and has not previously adjusted their PoA but PoA amount is populated in session data" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(None, Some(BigDecimal(3333.33))))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByClass("govuk-table__head")(msg("chargeHeading") + " " + msg("amountPreviousHeading"))
                )
              }
              "User has previously adjusted their PoA" in {
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
                IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testMtditid, "1040000124", 1500))
                await(sessionService.setMongoData(Some(PoaAmendmentData())))

                val expectedMsg = msg("chargeHeading") + " " + msg("amountPreviousHeading") + " " + msg("adjustedAmount")
                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByClass("govuk-table__head")(expectedMsg)
                )
              }

              s"redirect to the You Cannot Go Back page" when {
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
            "redirect to Select your reason page" when {
              "user has decreased poa" in {
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

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(1234.56)).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.SelectYourReasonController.show(isAgent, NormalMode).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(None, Some(1234.56))))
              }
            }
            s"return status $SEE_OTHER and redirect to check details page" when {
              "user has increased poa" in {
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

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(2500.00)).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(Increase), Some(2500.00))))
              }
              "user was on decrease only journey" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower)))))

                When(s"I call POST")

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(1.11)).futureValue

                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(1.11))))
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

                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(2000)).futureValue

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
                val result = buildPOSTMTDPostClient(path, additionalCookies, formData(2000)).futureValue

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

    s"GET $changePath" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(changePath, additionalCookies)
          } else {
            s"render the Enter PoA Amount page" that {
              "has the amount pre-populated" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower), Some(100)))))

                val result = buildGETMTDClient(changePath, additionalCookies).futureValue

                result should have(
                  httpStatus(OK),
                  elementTextByClass("govuk-table__head")(msg("chargeHeading") + " " + msg("amountPreviousHeading")),
                  elementAttributeByClass("govuk-input", "value")("100")
                )
              }
            }
          }
        }
        testAuthFailures(changePath, mtdUserRole)
      }
    }

    s"POST $changePath" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(changePath, additionalCookies)
          } else {
            "redirect to check your answers page, and overwrite amount in session" when {
              "user is on decrease only journey, and has entered new amount" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower), Some(1200)))))

                val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData(100)).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(100))))
              }
              "user is on increase/decrease journey, had previously increased, is still increasing" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower), Some(2500)))))

                val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData(2800)).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(Increase), Some(2800))))
              }
              "user is on increase/decrease journey, had previously decreased, is now increasing" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower), Some(500)))))

                val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData(2800)).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(Increase), Some(2800))))
              }
              "user is on increase/decrease journey, had previously decreased, is still decreasing" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(MainIncomeLower), Some(500)))))

                val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData(1000)).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.CheckYourAnswersController.show(isAgent).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(MainIncomeLower), Some(1000))))
              }
            }
            s"return status $SEE_OTHER and redirect to select your reason page" when {
              "user is on increase/decrease journey, had previously increased, is now decreasing" in {
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
                await(sessionService.setMongoData(Some(PoaAmendmentData(Some(Increase), Some(2500)))))

                val result = buildPOSTMTDPostClient(changePath, additionalCookies, formData(500)).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(routes.SelectYourReasonController.show(isAgent, CheckMode).url)
                )

                sessionService.getMongo.futureValue shouldBe Right(Some(PoaAmendmentData(Some(Increase), Some(500))))
              }
            }
          }
        }
        testAuthFailures(changePath, mtdUserRole, Some(Map.empty))
      }
    }
  }
}
