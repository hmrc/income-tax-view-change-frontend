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
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import controllers.claimToAdjustPoa.routes._
import models.claimToAdjustPoa.ClaimToAdjustPoaResponse.ClaimToAdjustPoaSuccess
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.FinancialDetailsTestConstants.testFinancialDetailsErrorModelJson
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
  private val validFinancialDetailsResponseBody: JsValue =
    testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))

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

  def stubFinancialDetailsResponse(response: JsValue = validFinancialDetailsResponseBody): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, response)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(OK, response)
  }

  s"calling GET" should {
    s"return status $OK" when {
      "non-crystallised financial details are found" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse()
        sessionService.setMongoData(Some(validSession))

        val res = get(url)

        res should have(
          httpStatus(OK)
        )
      }
    }
    s"return status $SEE_OTHER" when {
      "AdjustPaymentsOnAccount FS is disabled" in {

        disable(AdjustPaymentsOnAccount)

        val res = get(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "an error response is returned when requesting financial details" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse(testFinancialDetailsErrorModelJson)
        sessionService.setMongoData(Some(validSession))

        val res = get(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse(testEmptyFinancialDetailsModelJson)
        sessionService.setMongoData(Some(validSession))

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

        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
      "an error response is returned when submitting POA" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse()
        sessionService.setMongoData(Some(validSession))

        IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
          BAD_REQUEST,
          Json.stringify(Json.obj("message" -> "INVALID_REQUEST"))
        )

        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(ApiFailureSubmittingPoaController.show(isAgent).url)
        )
      }

      "a success response is returned when submitting POA" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse()
        sessionService.setMongoData(Some(validSession))

        IncomeTaxViewChangeStub.stubPostClaimToAdjustPoa(
          CREATED,
          Json.stringify(Json.toJson(
            ClaimToAdjustPoaSuccess(processingDate = "2024-01-31T09:27:17Z")
          ))
        )

        val res = post(url)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(SuccessController.show(isAgent).url)
        )
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "an error response is returned when requesting financial details" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse(testFinancialDetailsErrorModelJson)
        sessionService.setMongoData(Some(validSession))

        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse(testEmptyFinancialDetailsModelJson)
        sessionService.setMongoData(Some(validSession))

        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "some session data is missing" in {

        enableFs(AdjustPaymentsOnAccount)
        stubFinancialDetailsResponse()
        sessionService.setMongoData(Some(
          validSession.copy(poaAdjustmentReason = None)
        ))

        val res = post(url)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
