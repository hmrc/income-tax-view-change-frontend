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
import org.scalatest.Assertion
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino}
import testConstants.ClaimToAdjustPoaTestConstants.{testTaxYearPoa, validFinancialDetailsResponseBody, validSession}
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesResponse

class YouCannotGoBackControllerISpec extends ComponentSpecBase {

  val isAgent = false

  lazy val youCannotGoBackControllerUrl: String = controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url

  def homeUrl: String =
    if (isAgent) controllers.routes.HomeController.showAgent.url
    else controllers.routes.HomeController.show().url

  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.setMongoData(None))
    if (isAgent) {
      stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
    Given("Income Source Details with multiple business and property")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      OK, multipleBusinessesResponse
    )
  }

  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(
      s"""${
        if (isAgent) {
          "/agents"
        } else ""
      }${url}""", additionalCookies = clientDetailsWithConfirmation)
  }

  def checkPageTitleOk(res: WSResponse): Assertion = {
    if (isAgent) {
      res should have(
        pageTitleAgent("claimToAdjustPoa.youCannotGoBack.heading")
      )
    } else {
      res should have(
        pageTitleIndividual("claimToAdjustPoa.youCannotGoBack.heading")
      )
    }
  }

  def stubFinancialDetailsResponse(response: JsValue = validFinancialDetailsResponseBody(testTaxYearPoa)): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 1}-04-06", s"$testTaxYearPoa-04-05")(OK, response)
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYearPoa - 2}-04-06", s"${testTaxYearPoa - 1}-04-05")(OK, response)
  }

  "calling GET" should {
    s"return status $OK" when {
      s"user visits $youCannotGoBackControllerUrl with the AdjustPaymentsOnAccount FS enabled and journeyCompleted flag is set to false" in {
        enable(AdjustPaymentsOnAccount)

        And("Financial Details for valid POAs exist")
        stubFinancialDetailsResponse()

        And("A session exists which contains the new Payment On Account amount and reason")
        await(sessionService.setMongoData(Some(validSession)))

        When(s"I call GET")
        val res = get("/adjust-poa/poa-updated-cannot-go-back")

        res should have(
          httpStatus(OK)
        )
        checkPageTitleOk(res)
      }

      s"user visits $youCannotGoBackControllerUrl with the AdjustPaymentsOnAccount FS enabled and journeyCompleted flag is set to true" in {
        enable(AdjustPaymentsOnAccount)

        And("Financial Details for valid POAs exist")
        stubFinancialDetailsResponse()

        And("A session exists which contains the new Payment On Account amount and reason")
        await(sessionService.setMongoData(Some(validSession.copy(journeyCompleted = true))))

        When(s"I call GET")
        val res = get("/adjust-poa/poa-updated-cannot-go-back")

        res should have(
          httpStatus(OK)
        )
        checkPageTitleOk(res)
      }
    }
    s"return status $SEE_OTHER" when {
      "the AdjustPaymentsOnAccount FS is disabled" in {
        disable(AdjustPaymentsOnAccount)

        And("Financial Details for valid POAs exist")
        stubFinancialDetailsResponse()

        And("A session exists which contains the new Payment On Account amount and reason")
        await(sessionService.setMongoData(Some(validSession)))

        When(s"I call GET")
        val res = get("/adjust-poa/poa-updated-cannot-go-back")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }
    s"return status $INTERNAL_SERVER_ERROR" when {
      "session is missing" in {
        enable(AdjustPaymentsOnAccount)

        And("Financial Details for valid POAs exist")
        stubFinancialDetailsResponse()

        When(s"I call GET")
        val res = get("/adjust-poa/poa-updated-cannot-go-back")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
      "poa data is missing" in {
        enable(AdjustPaymentsOnAccount)

        And("A session exists which contains the new Payment On Account amount and reason")
        await(sessionService.setMongoData(Some(validSession.copy(journeyCompleted = true))))

        When(s"I call GET")
        val res = get("/adjust-poa/poa-updated-cannot-go-back")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
