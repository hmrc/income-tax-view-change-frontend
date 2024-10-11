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
import auth.MtdItUser
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.claimToAdjustPoa.routes.{ApiFailureSubmittingPoaController, PoaAdjustedController}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaSuccess
import models.claimToAdjustPoa.PoaAmendmentData
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testIncomeSource, testMtditid, testNino, testUserTypeAgent, testUserTypeIndividual}
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.validSession
import testConstants.FinancialDetailsTestConstants.testFinancialDetailsErrorModelJson
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  val isAgent = false

  def homeUrl: String =
    if (isAgent) controllers.routes.HomeController.showAgent.url
    else controllers.routes.HomeController.show().url

  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]
  val postUrlTestName: String = routes.CheckYourAnswersController.submit(isAgent).url
  val url: String = "/adjust-poa/check-your-answers"
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
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = Nil
  )
  lazy val testUser: MtdItUser[_] = {
    if (isAgent)
      MtdItUser(
        testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSource,
        None, Some("1234567890"), credId = None, Some(testUserTypeAgent), Some("1"))(FakeRequest())
    else
      MtdItUser(
        testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSource,
        None, Some("1234567890"), credId = None, Some(testUserTypeIndividual), None)(FakeRequest())
  }
  private def auditAdjustPayementsOnAccount(isSuccessful: Boolean): AdjustPaymentsOnAccountAuditModel = AdjustPaymentsOnAccountAuditModel(
    isSuccessful = isSuccessful,
    previousPaymentOnAccountAmount = 2000.00,
    requestedPaymentOnAccountAmount = 1000.00,
    adjustmentReasonCode = "001",
    adjustmentReasonDescription = "My main income will be lower",
    isDecreased = true
  )(testUser)

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupGetIncomeSourceDetails()
    await(sessionService.setMongoData(None))
    if (isAgent) {
      stubAuthorisedAgentUser(authorised = true, clientMtdId = testMtditid)
    }
  }

  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(
      s"""${
        if (isAgent) {
          "/agents"
        } else ""
      }$url""", additionalCookies = clientDetailsWithConfirmation)
  }

  def post(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.post(
      uri =
        s"""${
          if (isAgent) {
            "/agents"
          } else ""
        }$url""",
      additionalCookies = clientDetailsWithConfirmation
    )(Map.empty)
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

  s"calling GET" should {
    s"return status $OK" when {
      "user has successfully entered a new POA amount" in {
        enable(AdjustPaymentsOnAccount)

        setupGetFinancialDetails()

        And("A session exists which contains the new Payment On Account amount and reason")
        await(sessionService.setMongoData(Some(validSession)))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(OK)
        )
      }
    }

    s"return status $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
      "journeyCompleted flag is true and the user tries to access the page" in {
        enable(AdjustPaymentsOnAccount)

        setupGetFinancialDetails()

        And("A session has been created with journeyCompleted flag set to true")
        await(sessionService.setMongoData(Some(PoaAmendmentData(None, None, journeyCompleted = true))))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
        )
      }
    }

    s"return $INTERNAL_SERVER_ERROR" when {
      "the Payment On Account Adjustment reason is missing from the session" in {
        enable(AdjustPaymentsOnAccount)

        setupGetFinancialDetails()

        And("A session exists which is missing the Payment On Account adjustment reason")
        await(sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None))))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "the New Payment On Account Amount is missing from the session" in {
        enable(AdjustPaymentsOnAccount)

        setupGetFinancialDetails()

        And("A session exists which is missing the New Payment On Account amount")
        await(sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None))))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "both the New Payment On Account Amount and adjustment reason are missing from the session" in {
        enable(AdjustPaymentsOnAccount)

        setupGetFinancialDetails()

        And("A session exists which is missing the New Payment On Account amount")
        await(sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None, newPoaAmount = None))))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no adjust POA session is found" in {
        enable(AdjustPaymentsOnAccount)

        setupGetFinancialDetails()

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {
        enable(AdjustPaymentsOnAccount)

        Given("Empty financial details response")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testEmptyFinancialDetailsModelJson
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testEmptyFinancialDetailsModelJson
        )

        And("A session has been created and an amount entered")
        await(sessionService.setMongoData(Some(validSession)))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling POST $postUrlTestName" should {
    s"return status $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)

        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
      "an error response is returned when submitting POA" in {
        enable(AdjustPaymentsOnAccount)

        stubFinancialDetailsResponse()
        await(sessionService.setMongoData(Some(validSession)))

        IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
          BAD_REQUEST,
          Json.stringify(Json.obj("message" -> "INVALID_REQUEST"))
        )

        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ApiFailureSubmittingPoaController.show(isAgent).url)
        )
        verifyAuditContainsDetail(auditAdjustPayementsOnAccount(false).detail)
      }

      "a success response is returned when submitting POA" in {
        enable(AdjustPaymentsOnAccount)

        stubFinancialDetailsResponse()
        await(sessionService.setMongoData(Some(validSession)))

        IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
          CREATED,
          Json.stringify(Json.toJson(
            ClaimToAdjustPoaSuccess(processingDate = "2024-01-31T09:27:17Z")
          ))
        )

        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(PoaAdjustedController.show(isAgent).url)
        )
        verifyAuditContainsDetail(auditAdjustPayementsOnAccount(true).detail)
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "an error response is returned when requesting financial details" in {
        enable(AdjustPaymentsOnAccount)

        stubFinancialDetailsResponse(testFinancialDetailsErrorModelJson)
        await(sessionService.setMongoData(Some(validSession)))

        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse(testEmptyFinancialDetailsModelJson)
        await(sessionService.setMongoData(Some(validSession)))

        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "some session data is missing" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse()
        await(sessionService.setMongoData(Some(
          validSession.copy(poaAdjustmentReason = None)
        )))

        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}