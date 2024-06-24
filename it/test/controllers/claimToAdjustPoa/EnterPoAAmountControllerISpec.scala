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
import models.core.{CheckMode, NormalMode}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.PaymentOnAccountSessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testDate, testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testChargeHistoryJson, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson}

class EnterPoAAmountControllerISpec extends ComponentSpecBase {

  val isAgent = false

  def enterPoAAmountUrl = controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent, NormalMode).url

  def checkYourAnswersUrl = controllers.claimToAdjustPoa.routes.CheckYourAnswersController.show(isAgent).url

  def selectReasonUrl = controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, NormalMode).url

  def changePoAAmountUrl = controllers.claimToAdjustPoa.routes.EnterPoAAmountController.show(isAgent, CheckMode).url

  def changeReasonUrl = controllers.claimToAdjustPoa.routes.SelectYourReasonController.show(isAgent, CheckMode).url

  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]

  def msg(key: String) = msgs(s"claimToAdjustPoa.enterPoaAmount.$key")

  def homeUrl: String = if (isAgent) {
    controllers.routes.HomeController.showAgent.url
  } else {
    controllers.routes.HomeController.show().url
  }

  val testTaxYear = 2024
  val sessionService: PaymentOnAccountSessionService = app.injector.instanceOf[PaymentOnAccountSessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.setMongoData(None))
    if (isAgent) {
      stubAuthorisedAgentUser(true, clientMtdId = testMtditid)
    }
  }

  def get(url: String): WSResponse = {
    IncomeTaxViewChangeFrontend.get(s"""${
      if (isAgent) {
        "/agents"
      } else ""
    }${url}""", additionalCookies = clientDetailsWithConfirmation)
  }

  def postEnterPoA(isAgent: Boolean, newPoAAmount: BigDecimal)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    val formData: Map[String, Seq[String]] = {
      Map(
        "poa-amount" -> Seq(newPoAAmount.toString())
      )
    }
    IncomeTaxViewChangeFrontend.post(
      uri =
        s"""${
          if (isAgent) {
            "/agents"
          } else {
            ""
          }
        }/adjust-poa/enter-poa-amount""",
      additionalCookies = additionalCookies
    )(formData)
  }

  def postChangePoA(isAgent: Boolean, newPoAAmount: BigDecimal)(additionalCookies: Map[String, String] = Map.empty): WSResponse = {
    val formData: Map[String, Seq[String]] = {
      Map(
        "poa-amount" -> Seq(newPoAAmount.toString())
      )
    }
    IncomeTaxViewChangeFrontend.post(
      uri =
        s"""${
          if (isAgent) {
            "/agents"
          } else {
            ""
          }
        }/adjust-poa/change-poa-amount""",
      additionalCookies = additionalCookies
    )(formData)
  }


  s"calling GET $enterPoAAmountUrl" should {
    s"return status $OK and render the Enter PoA Amount page" when {
      "User is authorised and has not previously adjusted their PoA" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData())))

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(OK)
        )
        lazy val document: Document = Jsoup.parse(res.body)
        document.getElementsByClass("govuk-table__head").text() shouldBe msg("initialAmount")
      }
      "User is authorised and has not previously adjusted their PoA but PoA amount is populated in session data" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(None, Some(BigDecimal(3333.33))))))

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(OK)
        )
        lazy val document: Document = Jsoup.parse(res.body)
        document.getElementsByClass("govuk-table__head").text() shouldBe msg("initialAmount")
      }
      "User is authorised and has previously adjusted their PoA" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData())))

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
    s"return status $SEE_OTHER and redirect to the You Cannot Go Back page" when {
      "journeyCompleted flag is true and the user tries to access the page" in {
        enable(AdjustPaymentsOnAccount)

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

        And("A session has been created with journeyCompleted flag set to true")
        await(sessionService.setMongoData(Some(PoAAmendmentData(None, None, journeyCompleted = true))))

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(isAgent).url)
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
        await(sessionService.setMongoData(Some(PoAAmendmentData())))

        When(s"I call GET")
        val res = get("/adjust-poa/enter-poa-amount")

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
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData())))

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 1234.56)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(selectReasonUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(None, Some(1234.56))))
      }
    }
    s"return status $SEE_OTHER and redirect to check details page" when {
      "user has increased poa" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData())))

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 2500.00)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(Increase), Some(2500.00))))
      }
      "user was on decrease only journey" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower)))))

        When(s"I call POST")

        val res = postEnterPoA(isAgent, 1.11)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(1.11))))
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
        await(sessionService.setMongoData(Some(PoAAmendmentData())))

        When(s"I call POST")
        val res = postEnterPoA(isAgent, 2000)(clientDetailsWithConfirmation)

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

        val res = postEnterPoA(isAgent, 2000)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling GET $changePoAAmountUrl" should {
    "render the page as normal, with the amount pre-populated" when {
      "User is authorised" in {
        enable(AdjustPaymentsOnAccount)

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

        And("A session exists and has data")
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower), Some(100)))))

        When(s"I call GET")
        val res = get("/adjust-poa/change-poa-amount")

        res should have(
          httpStatus(OK)
        )
        lazy val document: Document = Jsoup.parse(res.body)
        document.getElementsByClass("govuk-table__head").text() shouldBe msg("initialAmount")
        document.getElementsByClass("govuk-input").attr("value") shouldBe "100"
      }
    }
  }
  s"calling POST $changePoAAmountUrl" should {
    s"return status $SEE_OTHER and redirect to check your answers page, and overwrite amount in session" when {
      "user is on decrease only journey, and has entered new amount" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower), Some(1200)))))

        When(s"I call POST")

        val res = postChangePoA(isAgent, 100)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(100))))
      }
      "user is on increase/decrease journey, had previously increased, is still increasing" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower), Some(2500)))))

        When(s"I call POST")

        val res = postChangePoA(isAgent, 2800)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(Increase), Some(2800))))
      }
      "user is on increase/decrease journey, had previously decreased, is now increasing" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower), Some(500)))))

        When(s"I call POST")

        val res = postChangePoA(isAgent, 2800)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(Increase), Some(2800))))
      }
      "user is on increase/decrease journey, had previously decreased, is still decreasing" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(MainIncomeLower), Some(500)))))

        When(s"I call POST")

        val res = postChangePoA(isAgent, 1000)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourAnswersUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(MainIncomeLower), Some(1000))))
      }
    }
    s"return status $SEE_OTHER and redirect to select your reason page" when {
      "user is on increase/decrease journey, had previously increased, is now decreasing" in {
        enable(AdjustPaymentsOnAccount)

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
        await(sessionService.setMongoData(Some(PoAAmendmentData(Some(Increase), Some(2500)))))

        When(s"I call POST")

        val res = postChangePoA(isAgent, 500)(clientDetailsWithConfirmation)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(changeReasonUrl)
        )

        sessionService.getMongo.futureValue shouldBe Right(Some(PoAAmendmentData(Some(Increase), Some(500))))
      }
    }
  }
}
