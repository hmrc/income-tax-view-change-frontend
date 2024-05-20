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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.featureswitch.AdjustPaymentsOnAccount
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.claimToAdjustPoa.{MainIncomeLower, PoAAmendmentData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class CheckYourAnswersControllerISpec extends ComponentSpecBase {

  val isAgent = false
  def homeUrl: String =
    if (isAgent) controllers.routes.HomeController.showAgent.url
    else         controllers.routes.HomeController.show().url
  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]
  val validSession: PoAAmendmentData = PoAAmendmentData(Some(MainIncomeLower), Some(BigDecimal(1000.00)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    sessionService.setMongoData(None)
    if(isAgent) {
      stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
  }

  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(s"""${if (isAgent) {"/agents" } else ""}$url""", additionalCookies = clientDetailsWithConfirmation)
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


  s"calling GET" should {
    s"return status $OK" when {
      "user has successfully entered a new POA amount" in {

        enable(AdjustPaymentsOnAccount)

        setupGetIncomeSourceDetails()
        setupGetFinancialDetails()

        And("A session exists which contains the new Payment On Account amount and reason")
        sessionService.setMongoData(Some(validSession))

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

        setupGetIncomeSourceDetails()

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }

    s"return $INTERNAL_SERVER_ERROR" when {
      "the Payment On Account Adjustment reason is missing from the session" in {
        enable(AdjustPaymentsOnAccount)

        setupGetIncomeSourceDetails()
        setupGetFinancialDetails()

        And("A session exists which is missing the Payment On Account adjustment reason")
        sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None)))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "the New Payment On Account Amount is missing from the session" in {
        enable(AdjustPaymentsOnAccount)

        setupGetIncomeSourceDetails()
        setupGetFinancialDetails()

        And("A session exists which is missing the New Payment On Account amount")
        sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None)))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "both the New Payment On Account Amount and adjustment reason are missing from the session" in {
        enable(AdjustPaymentsOnAccount)

        setupGetIncomeSourceDetails()
        setupGetFinancialDetails()

        And("A session exists which is missing the New Payment On Account amount")
        sessionService.setMongoData(Some(validSession.copy(poaAdjustmentReason = None, newPoAAmount = None)))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no adjust POA session is found" in {
        enable(AdjustPaymentsOnAccount)

        setupGetIncomeSourceDetails()
        setupGetFinancialDetails()

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "no non-crystallised financial details are found" in {
        enable(AdjustPaymentsOnAccount)

        setupGetIncomeSourceDetails()

        Given("Empty financial details response")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testEmptyFinancialDetailsModelJson
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testEmptyFinancialDetailsModelJson
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(validSession))

        When(s"I call GET")
        val res = get("/adjust-poa/check-your-answers")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
