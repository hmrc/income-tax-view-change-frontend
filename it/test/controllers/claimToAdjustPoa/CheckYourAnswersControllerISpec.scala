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

import audit.models.AdjustPaymentsOnAccountAuditModel
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.ControllerISpecHelper
import controllers.claimToAdjustPoa.routes.{ApiFailureSubmittingPoaController, PoaAdjustedController}
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaSuccess
import models.claimToAdjustPoa.PoaAmendmentData
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.FinancialDetailsTestConstants.testFinancialDetailsErrorModelJson
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.validSession

import java.time.LocalDate

class CheckYourAnswersControllerISpec extends ControllerISpecHelper {

  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]
  private val validFinancialDetailsResponseBody: JsValue =
    testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
  lazy val fixedDate : LocalDate = LocalDate.of(2024, 6, 5)
  lazy val incomeSource: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      None,
      None,
      Some(getCurrentTaxYearEnd),
      None,
      None,
      address = Some(address),
      cashOrAccruals = Some(false)
    )),
    properties = Nil
  )

  private def auditAdjustPayementsOnAccount(isSuccessful: Boolean, mtdUserRole: MTDUserRole): AdjustPaymentsOnAccountAuditModel = AdjustPaymentsOnAccountAuditModel(
    isSuccessful = isSuccessful,
    previousPaymentOnAccountAmount = 2000.00,
    requestedPaymentOnAccountAmount = 1000.00,
    adjustmentReasonCode = "001",
    adjustmentReasonDescription = "My main income will be lower",
    isDecreased = true
  )(getTestUser(mtdUserRole, incomeSource))

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupGetIncomeSourceDetails()
    await(sessionService.setMongoData(None))
  }

  def getPath(mtdUserRole: MTDUserRole): String = {
    val pathStart = if(mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/check-your-answers"
  }

  def setupGetIncomeSourceDetails(): Unit = {
    Given("Income Source Details with multiple business and property")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
    )
  }

  def setupGetFinancialDetails(): StubMapping = {
    And("Financial details for multiple years with POAs")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
      OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
    )
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
      OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
    )
  }

  def stubFinancialDetailsResponse(response: JsValue = validFinancialDetailsResponseBody): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, response)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(OK, response)
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
            s"render the Adjusting your payments on account page" when {
              "user has successfully entered a new POA amount" in {
                stubAuthorised(mtdUserRole)
                setupGetFinancialDetails()
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
              }
            }

            "redirect to You cannot go back page" when {
              "journeyCompleted flag is true and the user tries to access the page" in {
                stubAuthorised(mtdUserRole)
                setupGetFinancialDetails()
                await(sessionService.setMongoData(Some(PoaAmendmentData(None, None, journeyCompleted = true))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
                )
              }
            }

            s"return $INTERNAL_SERVER_ERROR" when {
              "the Payment On Account Adjustment reason is missing from the session" in {
                stubAuthorised(mtdUserRole)
                setupGetFinancialDetails()
                await(sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "the New Payment On Account Amount is missing from the session" in {
                stubAuthorised(mtdUserRole)
                setupGetFinancialDetails()
                await(sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "both the New Payment On Account Amount and adjustment reason are missing from the session" in {
                stubAuthorised(mtdUserRole)
                setupGetFinancialDetails()
                await(sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None, newPoaAmount = None))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "no adjust POA session is found" in {
                stubAuthorised(mtdUserRole)
                setupGetFinancialDetails()

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "no non-crystallised financial details are found" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
                  OK, testEmptyFinancialDetailsModelJson
                )
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
                  OK, testEmptyFinancialDetailsModelJson
                )
                await(sessionService.setMongoData(Some(validSession)))

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
            "redirect to PoaAdjustedController" when {
              "a success response is returned when submitting POA" in {
                stubAuthorised(mtdUserRole)

                stubFinancialDetailsResponse()
                await(sessionService.setMongoData(Some(validSession)))

                IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
                  CREATED,
                  Json.stringify(Json.toJson(
                    ClaimToAdjustPoaSuccess(processingDate = "2024-01-31T09:27:17Z")
                  ))
                )

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(PoaAdjustedController.show(isAgent).url)
                )
                verifyAuditContainsDetail(auditAdjustPayementsOnAccount(true, mtdUserRole).detail)
              }
            }
            "redirect to ApiFailureSubmittingPoaController" when {
              "an error response is returned when submitting POA" in {
                stubAuthorised(mtdUserRole)

                stubFinancialDetailsResponse()
                await(sessionService.setMongoData(Some(validSession)))

                IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
                  BAD_REQUEST,
                  Json.stringify(Json.obj("message" -> "INVALID_REQUEST"))
                )

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(ApiFailureSubmittingPoaController.show(isAgent).url)
                )
                verifyAuditContainsDetail(auditAdjustPayementsOnAccount(false, mtdUserRole).detail)
              }
            }

            s"return status $INTERNAL_SERVER_ERROR" when {
              "an error response is returned when requesting financial details" in {
                stubAuthorised(mtdUserRole)

                stubFinancialDetailsResponse(testFinancialDetailsErrorModelJson)
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "no non-crystallised financial details are found" in {
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse(testEmptyFinancialDetailsModelJson)
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "some session data is missing" in {
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse()
                await(sessionService.setMongoData(Some(
                  validSession.copy(poaAdjustmentReason = None)
                )))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
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