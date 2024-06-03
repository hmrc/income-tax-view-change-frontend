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
import models.claimToAdjustPoa.{Increase, MainIncomeLower, PoAAmendmentData}
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSResponse
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testChargeHistoryJson, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class EnterPoAAmountControllerISpec extends ComponentSpecBase{

  val isAgent = false

  def enterPoAAmountUrl = controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent).url

  def checkYourAnswersUrl = controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url
  def selectReasonUrl = controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode).url

  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  def msg(key: String) = msgs(s"claimToAdjustPoa.enterPoaAmount.$key")

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

  def postEnterPoA(isAgent: Boolean, newPoAAmount: BigDecimal)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    val formData: Map[String, Seq[String]] = {
      Map(
        "poa-amount" -> Seq(newPoAAmount.toString())
      )
    }
    IncomeTaxViewChangeFrontend.post(
      uri = s"""${
        if (isAgent) {
          "/agents"
        } else {
          ""
        }
      }/adjust-poa/enter-poa-amount""",
      additionalCookies = additionalCookies
    )(formData)
  }


  s"calling GET $enterPoAAmountUrl" should {
    s"return status $OK and render the Enter PoA Amount page" when {
      "User is authorised and has not previously adjusted their PoA" in {
        enableFs(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )

        And("A session has been created")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(OK)
        )
        lazy val document: Document = Jsoup.parse(res.body)
        document.getElementsByClass("govuk-table__head").text() shouldBe msg("initialAmount")
      }
      "User is authorised and has previously adjusted their PoA" in {
        enableFs(AdjustPaymentsOnAccount)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )

        And("I wiremock stub charge history details with a poaAdjustmentReason")
        IncomeTaxViewChangeStub.stubChargeHistoryResponse(testNino, "ABCD1234")(OK, testChargeHistoryJson(testMtditid, "1040000124", 1500))

        And("A session has been created")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(OK)
        )

        lazy val document: Document = Jsoup.parse(res.body)
        document.getElementsByClass("govuk-table__head").text() shouldBe {
          msg("initialAmount") + " " + msg("adjustedAmount")
        }
      }
    }
    s"return status $SEE_OTHER and redirect to the home page" when {
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
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(homeUrl)
        )
      }
    }
    s"return $INTERNAL_SERVER_ERROR" when {

      "no non-crystallised financial details are found" in {
        enableFs(AdjustPaymentsOnAccount)

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
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "no adjust POA session is found" in {
        enableFs(AdjustPaymentsOnAccount)

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
  s"calling POST $enterPoAAmountUrl" should {
    s"return status $SEE_OTHER and redirect to select your reason page" when {
      "user has decreased poa" in {
        enableFs(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs (relevantAmount > totalAmount")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 1000)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(selectReasonUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(None, Some(1000))))
      }
    }
    s"return status $SEE_OTHER and redirect to check details page" when {
      "user has increased poa" in {
        enableFs(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs (relevantAmount > totalAmount")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(3000))
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(PoAAmendmentData()))

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 2500)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(Increase), Some(2500))))
      }
      "user was on decrease only journey" in {
        enableFs(AdjustPaymentsOnAccount)

        Given("Income Source Details with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))
        )

        And("Financial details for multiple years with POAs (relevantAmount <= totalAmount")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString, poaRelevantAmount = Some(2000))
        )

        And("A session has been created and an amount entered")
        sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower))))

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 1500)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(1500))))
      }
    }
    s"return $INTERNAL_SERVER_ERROR" when {
      "no non-crystallised financial details are found" in {
        enableFs(AdjustPaymentsOnAccount)

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
        val res = postEnterPoA(isAgent, 2000)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "no adjust POA session is found" in {
        enableFs(AdjustPaymentsOnAccount)

        And("Financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(
          OK, testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString)
        )

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 2000)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}
