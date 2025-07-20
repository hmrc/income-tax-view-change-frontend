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
import controllers.ControllerISpecHelper
import controllers.claimToAdjustPoa.routes._
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaSuccess
import models.claimToAdjustPoa.{MainIncomeLower, PoaAmendmentData}
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

import java.time.LocalDate

class ConfirmationForAdjustingPoaControllerISpec extends ControllerISpecHelper {

  private val testTaxYear = 2024
  private val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]
  private val validSession: PoaAmendmentData = PoaAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  private val validFinancialDetailsResponseBody: JsValue =
    testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
  lazy val fixedDate: LocalDate = LocalDate.of(2024, 6, 5)
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
    await(sessionService.setMongoData(None))
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      status = OK,
      response = propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
    )
  }

  def getPath(mtdUserRole: MTDUserRole) = {
    val pathStart = if (mtdUserRole == MTDIndividual) "" else "/agents"
    pathStart + "/adjust-poa/confirmation"
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
              "non-crystallised financial details are found" in {

                enable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse()
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
              }
            }
            s"redirect to home page" when {
              "AdjustPaymentsOnAccount FS is disabled" in {
                disable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
              "journeyCompleted flag is true and the user tries to access the page" in {
                enable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse()

                await(sessionService.setMongoData(Some(PoaAmendmentData(None, None, journeyCompleted = true))))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
                )
              }
            }
            s"return status $INTERNAL_SERVER_ERROR" when {
              "an error response is returned when requesting financial details" in {
                enable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse(testFinancialDetailsErrorModelJson)
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "no non-crystallised financial details are found" in {
                enable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse(testEmptyFinancialDetailsModelJson)
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
                enable(AdjustPaymentsOnAccount)
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

                enable(AdjustPaymentsOnAccount)
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
            s"redirect to home page" when {
              "AdjustPaymentsOnAccount FS is disabled" in {
                disable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(homeUrl(mtdUserRole))
                )
              }
            }

            s"return status $INTERNAL_SERVER_ERROR" when {
              "an error response is returned when requesting financial details" in {
                enable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse(testFinancialDetailsErrorModelJson)
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "no non-crystallised financial details are found" in {
                enable(AdjustPaymentsOnAccount)
                stubAuthorised(mtdUserRole)
                stubFinancialDetailsResponse(testEmptyFinancialDetailsModelJson)
                await(sessionService.setMongoData(Some(validSession)))

                val result = buildPOSTMTDPostClient(path, additionalCookies, Map.empty).futureValue
                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "some session data is missing" in {
                enable(AdjustPaymentsOnAccount)
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
