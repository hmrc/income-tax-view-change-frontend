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

import forms.adjustPoa.SelectYourReasonFormProvider
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PoAAmendmentData, SelectYourReason}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class SelectYourReasonControllerISpec extends ComponentSpecBase {

  val isAgent = false
  def enterPOAAmountUrl: String = controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent).url
  def poaCyaUrl: String = controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url
  def homeUrl: String = if(isAgent){
    controllers.routes.HomeController.showAgent.url
  } else {
    controllers.routes.HomeController.show().url
  }
  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    sessionService.setMongoData(None)
    if(isAgent) {
      stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
  }
  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(s"""${if (isAgent) {"/agents" } else ""}${url}""", additionalCookies = clientDetailsWithConfirmation)
  }

  def postSelectYourReason(isAgent: Boolean, answer: Option[SelectYourReason])(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    val formProvider = app.injector.instanceOf[SelectYourReasonFormProvider]
    IncomeTaxViewChangeFrontend.post(
      uri = s"""${if(isAgent) {"/agents"} else {""}}/adjust-poa/select-your-reason""",
      additionalCookies = additionalCookies
    )(
      answer.fold(Map.empty[String, Seq[String]])(
        selection => {
          formProvider.apply()
            .fill(selection)
            .data.map { case (k, v) => (k, Seq(v)) }
        }
      )
    )
  }

  s"calling GET" should {

    s"return status $OK" when {

      "user has entered an amount lower than current amount" in {

        enable(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )

        And("A session has been created")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call GET")
        val res = get("/adjust-poa/select-your-reason")

        res should have(
          httpStatus(OK)
        )
      }
    }

    s"return status $SEE_OTHER" when {

      "user has entered an amount higher than current amount" in {

        enable(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(500, 500, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(500, 500, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(PoAAmendmentData(newPoAAmount = Some(1500.0))))

        When(s"I call GET")
        val res = get("/adjust-poa/select-your-reason")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(poaCyaUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(
          PoAAmendmentData(
            newPoAAmount = Some(1500.0),
            poaAdjustmentReason = Some(Increase))))
      }

      "AdjustPaymentsOnAccount FS is disabled" in {

          disable(AdjustPaymentsOnAccount)

          Given("I wiremock stub a successful Income Source Details response with multiple business and property")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
          )

          And("I wiremock stub financial details for multiple years with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
            OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
          )
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
            OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
          )

          When(s"I call GET")
          val res = get("/adjust-poa/select-your-reason")

          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(homeUrl)
          )
      }
    }

    s"return $INTERNAL_SERVER_ERROR" when {

      "no non-crystallised financial details are found" in {
        enable(AdjustPaymentsOnAccount)

        And("I wiremock stub empty financial details response")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testEmptyFinancialDetailsModelJson
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testEmptyFinancialDetailsModelJson
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call GET")
        val res = get("/adjust-poa/select-your-reason")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "no adjust POA session is found" in {
        enable(AdjustPaymentsOnAccount)

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )

        When(s"I call GET")
        val res = get("/adjust-poa/select-your-reason")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

    }
  }

  s"calling POST" should {

    s"return status $SEE_OTHER" when {

      s"when originalAmount >= relevantAmount should redirect to Enter the Amount page" in {

        enable(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call POST")

        val res = postSelectYourReason(isAgent, Some(MainIncomeLower))(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(enterPOAAmountUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(MainIncomeLower), None)))
      }

    "when originalAmount < relevantAmount redirect to Check Your Answers page" in {
        enable(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )

      And("A session has been created")
      sessionService.setMongoData(Some(PoAAmendmentData()))

      When(s"I call POST")
        val res = postSelectYourReason(isAgent, Some(MainIncomeLower))(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(poaCyaUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(MainIncomeLower), None)))
      }

      "AdjustPaymentsOnAccount FS is disabled and redirect to the home page" in {

        disable(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )

        And("A session has been created")
        sessionService.setMongoData(Some(PoAAmendmentData()))


        When(s"I call POST")

        val res = postSelectYourReason(isAgent, Some(MainIncomeLower))(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }

    s"return $INTERNAL_SERVER_ERROR" when {
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
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call POST")
        val res = postSelectYourReason(isAgent, Some(MainIncomeLower))(clientDetailsWithConfirmation)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "no adjust POA session is found" in {
        enable(AdjustPaymentsOnAccount)

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )

        When(s"I call POST")

        val res = postSelectYourReason(isAgent, Some(MainIncomeLower))(clientDetailsWithConfirmation)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
