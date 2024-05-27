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

import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{MainIncomeLower, PoAAmendmentData}
import play.api.http.Status.{BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import controllers.claimToAdjustPoa.routes._
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaSuccess
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.FinancialDetailsIntegrationTestConstants.testFinancialDetailsErrorModel
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class ConfirmationForAdjustingPoaControllerISpec extends ComponentSpecBase {

  val isAgent = false
  private def homeUrl: String =
    if (isAgent) controllers.routes.HomeController.showAgent.url
    else         controllers.routes.HomeController.show().url
  private val testTaxYear = 2024
  private val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]
  private val validSession: PoAAmendmentData = PoAAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))
  private val url: String = "/adjust-poa/confirmation"

  override def beforeEach(): Unit = {

    super.beforeEach()
    sessionService.setMongoData(None)
    if(isAgent) stubAuthorisedAgentUser(isAgent, clientMtdId = testMtditid)

    Given("Income Source Details with multiple business and property")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      status = OK,
      response = propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
    )
  }

  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(s"""${if (isAgent) {"/agents" } else ""}$url""", additionalCookies = clientDetailsWithConfirmation)
  }

  def post(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.post(
      uri = s"""${if (isAgent) {"/agents" } else ""}$url""",
      additionalCookies = clientDetailsWithConfirmation
    )(Map.empty)
  }

  def stubFinancialDetailsSuccessResponse(): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
      OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
    )
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
      OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
    )
  }

  def stubFinancialDetailsEmptyResponse(): Unit = {
    And("I wiremock stub empty financial details response")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
      OK, Json.toJson(testEmptyFinancialDetailsModelJson)
    )
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
      OK, Json.toJson(testEmptyFinancialDetailsModelJson)
    )
  }

  def stubFinancialDetailsErrorResponse(): Unit = {
    And("I wiremock stub financial details error response")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
      OK, Json.toJson(testFinancialDetailsErrorModel)
    )
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
      OK, Json.toJson(testFinancialDetailsErrorModel)
    )
  }


  s"calling GET" should {
    s"return status $OK" when {
      "non-crystallised financial details are found" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsSuccessResponse()
        sessionService.setMongoData(Some(validSession))

        When(s"I call GET $url")
        val res = get(url)

        res should have(
          httpStatus(OK)
        )
      }
    }
    s"return status $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {

        disable(AdjustPaymentsOnAccount)

        When(s"I call GET $url")
        val res = get(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "an error response is returned when requesting financial details" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsErrorResponse()
        sessionService.setMongoData(Some(validSession))

        When(s"I call GET $url")
        val res = get(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsEmptyResponse()
        sessionService.setMongoData(Some(validSession))

        When(s"I call GET $url")
        val res = get(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling POST" should {
    s"return status $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {

        disable(AdjustPaymentsOnAccount)

        When(s"I call POST $url")
        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
      "an error response is returned when submitting POA" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsSuccessResponse()
        sessionService.setMongoData(Some(validSession))

        IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
          BAD_REQUEST,
          Json.stringify(Json.obj("message" -> "INVALID_REQUEST"))
        )

        When(s"I call POST $url")
        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ApiFailureSubmittingPoaController.show(isAgent).url)
        )
      }

      "a success response is returned when submitting POA" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsSuccessResponse()
        sessionService.setMongoData(Some(validSession))

        IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
          CREATED,
          Json.stringify(Json.toJson(
            ClaimToAdjustPoaSuccess(processingDate = "2024-01-31T09:27:17Z")
          ))
        )

        When(s"I call POST $url")
        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(SuccessController.show(isAgent).url)
        )
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "an error response is returned when requesting financial details" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsErrorResponse()
        sessionService.setMongoData(Some(validSession))

        When(s"I call POST $url")
        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsEmptyResponse()
        sessionService.setMongoData(Some(validSession))

        When(s"I call POST $url")
        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "some session data is missing" in {

        enable(AdjustPaymentsOnAccount)
        stubFinancialDetailsSuccessResponse()
        sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None)))

        When(s"I call POST $url")
        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
