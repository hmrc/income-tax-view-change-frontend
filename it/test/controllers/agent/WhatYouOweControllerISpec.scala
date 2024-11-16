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

package controllers.agent

import audit.models.WhatYouOweResponseAuditModel
import auth.MtdItUser
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.admin.{AdjustPaymentsOnAccount, CreditsRefundsRepay, ReviewAndReconcilePoa}
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import services.DateService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.ChargeConstants
import testConstants.FinancialDetailsIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.OutstandingChargesIntegrationTestConstants._
import testConstants.messages.WhatYouOweMessages.underReview
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class WhatYouOweControllerISpec extends ComponentSpecBase  with ChargeConstants with TransactionUtils {


  val testArn: String = "1"

  val testDate: LocalDate = LocalDate.parse("2022-01-01")

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(testDate, testDate.plusYears(1))),
      None,
      None,
      Some(getCurrentTaxYearEnd),
      None,
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = Nil
  )

  override implicit val dateService: DateService = app.injector.instanceOf[DateService]


  implicit val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetailsModel,
    None, Some("1234567890"), None, Some(Agent), Some(testArn)
  )(FakeRequest())

  val configuredChargeItemGetter: List[FinancialDetail] => DocumentDetail => Option[ChargeItem] =
    getChargeItemOpt(reviewAndReconcileEnabled = isEnabled(ReviewAndReconcilePoa))

  val testValidOutStandingChargeResponseJsonWithAciAndBcdCharges: JsValue = Json.parse(
    s"""
       |{
       |  "outstandingCharges": [{
       |         "chargeName": "BCD",
       |         "relevantDueDate": "$testDate",
       |         "chargeAmount": 123456789012345.67,
       |         "tieBreaker": 1234
       |       },
       |       {
       |         "chargeName": "ACI",
       |         "relevantDueDate": "$testDate",
       |         "chargeAmount": 12.67,
       |         "tieBreaker": 1234
       |       }
       |  ]
       |}
       |""".stripMargin)

  val currentTaxYearEnd: Int = getCurrentTaxYearEnd.getYear
  val previousTaxYearEnd: Int = currentTaxYearEnd - 1
  val twoPreviousTaxYearEnd: Int = currentTaxYearEnd - 2

  val testTaxYear: Int = currentTaxYearEnd


  s"GET ${controllers.routes.WhatYouOweController.showAgent.url}" should {
    "SEE_OTHER to " when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = IncomeSourceDetailsModel(
            nino = testNino,
            mtdbsa = testMtditid,
            yearOfMigration = None,
            businesses = List(BusinessDetailsModel(
              "testId",
              incomeSource = Some(testIncomeSource),
              Some(AccountingPeriodModel(testDate, testDate.plusYears(1))),
              None,
              None,
              Some(getCurrentTaxYearEnd),
              None,
              address = Some(address),
              cashOrAccruals = false
            )),
            properties = Nil
          )
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$currentTaxYearEnd - 1-04-06", s"$currentTaxYearEnd-04-05")(OK,
          testValidFinancialDetailsModelJson(
            2000, 2000, currentTaxYearEnd.toString, testDate.toString))
        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        Then("The user is redirected to")
        result should have(
          httpStatus(SEE_OTHER),
        )
      }
    }
  }

  "YearOfMigration exists" when {
    "with a multiple charge from financial details and BCD and ACI charges from CESA" in {

      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK,
        testValidFinancialDetailsModelJson(
          2000, 2000, (currentTaxYearEnd - 2).toString, LocalDate.parse("2022-01-01").toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataWithDataDueIn30DaysIt, dateService).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
        isElementVisibleById("due-0")(expectedValue = true),
        isElementVisibleById("due-1")(expectedValue = true),
        isElementVisibleById("future-payments-heading")(expectedValue = false),
        isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
        isElementVisibleById(s"sa-tax-bill")(expectedValue = true)


      )
    }

    "with a multiple charge, without BCD and ACI charges from CESA" in {
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

      val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, currentTaxYearEnd.toString, testDate.minusDays(15).toString)
      val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05"
      )(OK, financialDetailsResponseJson)

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      val whatYouOweChargesList = {
        val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == currentTaxYearEnd)

        val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
        val chargeItemFromDocumentDetail = configuredChargeItemGetter(financialDetails.financialDetails)
        val chargeItems = financialDetails.documentDetails.flatMap(chargeItemFromDocumentDetail)

        WhatYouOweChargesList(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          chargesList = chargeItems
        )
      }
      AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
        isElementVisibleById("due-0")(expectedValue = true),
        isElementVisibleById("due-1")(expectedValue = true),
        isElementVisibleById("charge-interest-0")(expectedValue = false),
        isElementVisibleById("charge-interest-1")(expectedValue = false),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
        isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
        isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
      )
    }

    "with multiple charges and one charge equals zero" in {
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      val mixedJson = Json.obj(
        "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
        "documentDetails" -> Json.arr(
          documentDetailJson(3400.00, 1000.00, (currentTaxYearEnd - 2), "ITSA- POA 1", transactionId = "transId1"),
          documentDetailJson(1000.00, 100.00, (currentTaxYearEnd - 2), "ITSA- POA 1", transactionId = "transId2", dueDate = testDate.plusDays(1).toString),
          documentDetailJson(1000.00, 0, (currentTaxYearEnd - 2), "ITSA - POA 2", transactionId = "transId3", dueDate = testDate.minusDays(1).toString)
        ),
        "financialDetails" -> Json.arr(
          financialDetailJson((currentTaxYearEnd - 2).toString, transactionId = "transId1"),
          financialDetailJson(taxYear = (currentTaxYearEnd - 2).toString, mainType = "SA Payment on Account 1", mainTransaction="4920", dueDate = testDate.plusDays(1).toString, transactionId = "transId2"),
          financialDetailJson((currentTaxYearEnd - 2).toString, "SA Payment on Account 2", mainTransaction="4910", dueDate = testDate.minusDays(1).toString, transactionId = "transId3")
        ))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        OK, mixedJson)

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
        OK, testValidOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweWithAZeroOutstandingAmount(), dateService).detail)

      Then("The Payment Due what you owe page is returned to the user")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
        isElementVisibleById("due-0")(expectedValue = true),
        isElementVisibleById("due-1")(expectedValue = true),
        isElementVisibleById("charge-interest-0")(expectedValue = false),
        isElementVisibleById("charge-interest-1")(expectedValue = false),
        isElementVisibleById("due-2")(expectedValue = false),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
        isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
        isElementVisibleById(s"sa-tax-bill")(expectedValue = true)

      )
    }

    "redirect to an internal server error page when both connectors return internal server error" in {
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
        s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(
        INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to an internal server error page when financial connector return internal server error" in {
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
        s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(
        OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "redirect to an internal server error page when Outstanding charges connector return internal server error" in {
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
        )
      )

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
        testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
        OK, testValidFinancialDetailsModelJson(
          2000, 2000, currentTaxYearEnd.toString, testDate.toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
        INTERNAL_SERVER_ERROR, testOutstandingChargesErrorModelJson)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")

      Then("the result should have a HTTP status of INTERNAL_SERVER_ERROR(500)")
      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )

    }
  }

  "render the what you owe page with interest accruing on overdue charges" in {
    stubAuthorisedAgentUser(authorised = true)

    Given("I wiremock stub a successful Income Source Details response with multiple business and property")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


    And("I wiremock stub a multiple financial details response")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
      testValidFinancialDetailsModelJsonAccruingInterest(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString))
    IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
      "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


    When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
    val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

    AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges(), dateService).detail)

    verifyIncomeSourceDetailsCall(testMtditid)
    IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
    IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

    Then("the result should have a HTTP status of OK (200) and the payments due page")
    result should have(
      httpStatus(OK),
      pageTitleAgent("whatYouOwe.heading-agent"),
      isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
      isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
      isElementVisibleById("due-0")(expectedValue = true),
      isElementVisibleById("charge-interest-0")(expectedValue = true),
      isElementVisibleById("due-1")(expectedValue = true),
      isElementVisibleById("charge-interest-1")(expectedValue = true),
      isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
      isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
      isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
      isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
    )
  }

  "render the what you owe page with no interest accruing on overdue charges when there is late payment interest" in {
    stubAuthorisedAgentUser(authorised = true)

    Given("I wiremock stub a successful Income Source Details response with multiple business and property")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


    And("I wiremock stub a multiple financial details response")
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
      testValidFinancialDetailsModelJsonAccruingInterest(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, Some(55.50)))
    IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
      "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


    When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
    val res = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

    AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges(), dateService).detail)
    verifyIncomeSourceDetailsCall(testMtditid)
    IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
    IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

    Then("the result should have a HTTP status of OK (200) and the payments due page")
    res should have(
      httpStatus(OK),
      pageTitleAgent("whatYouOwe.heading-agent"),
      isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
      isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
      isElementVisibleById("due-0")(expectedValue = true),
      isElementVisibleById("charge-interest-0")(expectedValue = false),
      isElementVisibleById("due-1")(expectedValue = true),
      isElementVisibleById("charge-interest-1")(expectedValue = false),
      isElementVisibleById("future-payments-heading")(expectedValue = false),
      isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
      isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
      isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
      isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
    )
  }

  "when showing the Dunning Lock content" should {
    "render the what you owe page with no dunningLocks" in {
      stubAuthorisedAgentUser(authorised = true)

      Given("I wiremock stub a successful Income Source Details response with multiple business and property")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

      And("I wiremock stub a multiple financial details response")
      val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = noDunningLock)
      val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
        financialDetailsResponseJson)
      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      val whatYouOweChargesList = {
        val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
        val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
        val chargeItemFromDocumentDetail = configuredChargeItemGetter(financialDetails.financialDetails)
        val chargeItems = financialDetails.documentDetails.flatMap(chargeItemFromDocumentDetail)

        WhatYouOweChargesList(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          chargesList = chargeItems
        )
      }
      AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

      Then("the result should have a HTTP status of OK (200) and the what you owe page with no dunningLocks")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = false),
        elementTextBySelector("tr#over-due-type-1 td:nth-child(2) div:nth-of-type(3)")(""),
        elementTextBySelector("tr#over-due-type-2 td:nth-child(2) div:nth-of-type(3)")("")
      )
    }

    "render the what you owe page with a dunningLocks against a charge" in {
      stubAuthorisedAgentUser(authorised = true)

      Given("I wiremock stub a successful Income Source Details response with multiple business and property")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


      And("I wiremock stub a multiple financial details response with dunning lock present")
      val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = oneDunningLock)
      val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
        financialDetailsResponseJson)
      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      val whatYouOweChargesList = {
        val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
        val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
        val chargeItemFromDocumentDetail = configuredChargeItemGetter(financialDetails.financialDetails)
        val chargeItems = financialDetails.documentDetails.flatMap(chargeItemFromDocumentDetail)

        WhatYouOweChargesList(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          chargesList = chargeItems
        )
      }
      AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

      Then("the result should have a HTTP status of OK (200) and the what you owe page with dunning lock present")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true),

        elementTextBySelector("tr#due-1 td:nth-child(2) div:nth-of-type(2)")(underReview),
        elementTextBySelector("tr#due-2 td:nth-child(2) div:nth-of-type(2)")("")
      )
    }

    "render the what you owe page with multiple dunningLocks" in {
      stubAuthorisedAgentUser(authorised = true)

      Given("I wiremock stub a successful Income Source Details response with multiple business and property")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))


      And("I wiremock stub a multiple financial details response with dunning locks present")
      val financialDetailsResponseJson = testValidFinancialDetailsModelJson(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString, dunningLock = twoDunningLocks)
      val financialDetailsModel = financialDetailsResponseJson.as[FinancialDetailsModel]

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
        financialDetailsResponseJson)
      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      val whatYouOweChargesList = {
        val documentDetailsForTestTaxYear = financialDetailsModel.documentDetails.filter(_.taxYear == testTaxYear)
        val financialDetails = financialDetailsModel.copy(documentDetails = documentDetailsForTestTaxYear)
        val chargeItemFromDocumentDetail = configuredChargeItemGetter(financialDetails.financialDetails)
        val chargeItems = financialDetails.documentDetails.flatMap(chargeItemFromDocumentDetail)

        WhatYouOweChargesList(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
          chargesList = chargeItems
        )
      }
      AuditStub.verifyAuditEvent(WhatYouOweResponseAuditModel(testUser, whatYouOweChargesList, dateService))

      Then("the result should have a HTTP status of OK (200) and the what you owe page with multiple dunningLocks")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("disagree-with-tax-appeal-link")(expectedValue = true),
        elementTextBySelector("tr#due-1 td:nth-child(2) div:nth-of-type(2)")(underReview),
        elementTextBySelector("tr#due-2 td:nth-child(2) div:nth-of-type(2)")(underReview)
      )
    }
  }

  s"return $OK" when {
    "YearOfMigration does not exists" when {
      "with a no charge" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(previousTaxYearEnd, None))


        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06",
          s"$currentTaxYearEnd-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList, dateService).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
          isElementVisibleById("overdueAmount")(expectedValue = false),
          isElementVisibleById("balanceDueWithin30Days")(expectedValue = false),
          isElementVisibleById("totalBalance")(expectedValue = false),
          isElementVisibleById(s"payments-made-bullets")(expectedValue = false),
          isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
        )
      }
    }

    "YearOfMigration exists but not the first year" when {
      "with a no charge" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino,
          s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK, testEmptyFinancialDetailsModelJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, testTaxYear.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList, dateService).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
          isElementVisibleById("overdueAmount")(expectedValue = false),
          isElementVisibleById("balanceDueWithin30Days")(expectedValue = false),
          isElementVisibleById("totalBalance")(expectedValue = false),
          isElementVisibleById(s"payments-made-bullets")(expectedValue = false),
          isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
        )
      }
    }

    "YearOfMigration exists and No valid charges exists" when {
      "with a no charge" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
          "documentDetails" -> Json.arr(
          ),
          "financialDetails" -> Json.arr(
          )
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, currentTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweNoChargeList, dateService).detail)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = true),
          isElementVisibleById("sa-note-migrated")(expectedValue = true),
          isElementVisibleById("outstanding-charges-note-migrated")(expectedValue = true),
          isElementVisibleById("overdueAmount")(expectedValue = false),
          isElementVisibleById("balanceDueWithin30Days")(expectedValue = false),
          isElementVisibleById("totalBalance")(expectedValue = false),
          isElementVisibleById(s"payments-made-bullets")(expectedValue = false),
          isElementVisibleById(s"sa-tax-bill")(expectedValue = false)
        )
      }
    }

    "YearOfMigration exists with Invalid financial details charges and valid outstanding charges" when {
      "only BCD charge" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        val mixedJson = Json.obj(
          "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00),
          "documentDetails" -> Json.arr(
            documentDetailJson(3400.00, 1000.00, currentTaxYearEnd, transactionId = "transId1"),
            documentDetailJson(1000.00, 0.00, currentTaxYearEnd, transactionId = "transId2"),
            documentDetailJson(1000.00, 3000.00, currentTaxYearEnd, transactionId = "transId3")
          ),
          "financialDetails" -> Json.arr(
            financialDetailJson(currentTaxYearEnd.toString, transactionId = "transId1"),
            financialDetailJson(currentTaxYearEnd.toString, transactionId = "transId2"),
            financialDetailJson(currentTaxYearEnd.toString, transactionId = "transId3")
          ))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(OK, mixedJson)

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweOutstandingChargesOnly, dateService).detail)
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = true),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = true),
          isElementVisibleById("future-payments-heading")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = false),
          isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
          isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
          isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
        )
      }
    }

    "YearOfMigration exists with valid financial details charges and invalid outstanding charges" when {
      "only BCD charge" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
          propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)))

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05"
        )(OK, testValidFinancialDetailsModelJson(
          2000, 2000, currentTaxYearEnd.toString, testDate.plusYears(1).toString))

        IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
          "utr", testSaUtr.toLong, previousTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

        val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        AuditStub.verifyAuditContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweFinancialDetailsEmptyBCDCharge, dateService).detail)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")
        IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, previousTaxYearEnd.toString)

        Then("the result should have a HTTP status of OK (200) and the payments due page")
        result should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
          isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
          isElementVisibleById(s"no-payments-due")(expectedValue = false),
          isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
          isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
          isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
          isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
        )
      }
    }

    "render the payments due totals" in {

      Given("Authorised Agent")
      stubAuthorisedAgentUser(authorised = true)

      Given("I wiremock stub a successful Income Source Details response with multiple business and property")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(previousTaxYearEnd.toString)))


      And("I wiremock stub a multiple financial details response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
        testValidFinancialDetailsModelJsonAccruingInterest(2000, 2000, testTaxYear.toString, testDate.minusDays(15).toString))
      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, twoPreviousTaxYearEnd.toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)


      When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      AuditStub.verifyAuditDoesNotContainsDetail(WhatYouOweResponseAuditModel(testUser, whatYouOweDataFullDataWithoutOutstandingCharges(), dateService).detail)
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
      IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, twoPreviousTaxYearEnd.toString)

      Then("the result should have a HTTP status of OK (200) and the payments due page")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
      )
    }

  }

  "YearOfMigration exists with valid coding out charges" when {
    "coding out is enabled" in {
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK,
        propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05"
      )(OK, testValidFinancialDetailsModelJsonCodingOut(2000, 2000, testTaxYear.toString, testDate.plusYears(1).toString))

      IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
        "utr", testSaUtr.toLong, (testTaxYear - 1).toString)(OK, validOutStandingChargeResponseJsonWithoutAciAndBcdCharges)

      val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")
      IncomeTaxViewChangeStub.verifyGetOutstandingChargesResponse("utr", testSaUtr.toLong, (testTaxYear - 1).toString)

      Then("the result should have a HTTP status of OK (200) and the payments due page with coding out in future payments")
      result should have(
        httpStatus(OK),
        pageTitleAgent("whatYouOwe.heading-agent"),
        isElementVisibleById("balancing-charge-type-0")(expectedValue = false),
        isElementVisibleById("balancing-charge-type-1")(expectedValue = false),
        isElementVisibleById(s"no-payments-due")(expectedValue = false),
        isElementVisibleById(s"sa-note-migrated")(expectedValue = true),
        isElementVisibleById(s"outstanding-charges-note-migrated")(expectedValue = true),
        isElementVisibleById(s"payments-made-bullets")(expectedValue = true),
        isElementVisibleById(s"sa-tax-bill")(expectedValue = true)
      )
    }
  }

  "Claim to adjust POA section" should {
    "show" when {
      "The user has valid POAs and the FS is Enabled" in {
        enable(AdjustPaymentsOnAccount)
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString))
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(OK,
          testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString))

        When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("adjust-poa-link")(expectedValue = true))
      }
    }
    "not show" when {
      "The user does not have valid POAs but the FS is Enabled" in {
        enable(AdjustPaymentsOnAccount)
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        And("I wiremock stub financial details for multiple years with NO POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testEmptyFinancialDetailsModelJson)
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(OK,
          testEmptyFinancialDetailsModelJson)

        When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("adjust-poa-link")(expectedValue = false))
      }
      "The user has valid POAs but the FS is Disabled" in {
        disable(AdjustPaymentsOnAccount)
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with multiple business and property")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString)))

        And("I wiremock stub financial details for multiple years with POAs")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")(OK,
          testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString))
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 2}-04-06", s"${testTaxYear - 1}-04-05")(OK,
          testValidFinancialDetailsModelJson(2000, 2000, (testTaxYear - 1).toString, testDate.toString))

        When("I call GET /report-quarterly/income-and-expenses/view/agents/payments-owed")
        val res = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

        res should have(
          httpStatus(OK),
          pageTitleAgent("whatYouOwe.heading-agent"),
          isElementVisibleById("adjust-poa-link")(expectedValue = false))
      }
    }
  }

  "render the money in clients account section when balance details has available credits" in {
    stubAuthorisedAgentUser(authorised = true)
    enable(CreditsRefundsRepay)

    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      status = OK,
      propertyOnlyResponseWithMigrationData(previousTaxYearEnd, Some(currentTaxYearEnd.toString)
      )
    )

    val mixedJson = Json.obj(
      "balanceDetails" -> Json.obj("balanceDueWithin30Days" -> 1.00, "overDueAmount" -> 2.00, "totalBalance" -> 3.00, "availableCredit" -> 300.00),
      "documentDetails" -> Json.arr(
        documentDetailJson(3400.00, 1000.00, currentTaxYearEnd, "ITSA- POA 1", transactionId = "transId1"),
        documentDetailJson(1000.00, 100.00, currentTaxYearEnd, "ITSA- POA 1", transactionId = "transId2"),
        documentDetailJson(1000.00, 0, currentTaxYearEnd, "ITSA - POA 2", transactionId = "transId3")
      ),
      "financialDetails" -> Json.arr(
        financialDetailJson(currentTaxYearEnd.toString, transactionId = "transId1"),
        financialDetailJson(currentTaxYearEnd.toString, "SA Payment on Account 1", testDate.plusDays(1).toString, transactionId = "transId2"),
        financialDetailJson(currentTaxYearEnd.toString, "SA Payment on Account 2", testDate.minusDays(1).toString, transactionId = "transId3")
      ))

    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"$previousTaxYearEnd-04-06", s"$currentTaxYearEnd-04-05")(
      OK, mixedJson)

    IncomeTaxViewChangeStub.stubGetOutstandingChargesResponse(
      "utr", testSaUtr.toLong, (currentTaxYearEnd - 1).toString)(
      OK, validOutStandingChargeResponseJsonWithAciAndBcdCharges)

    val result = IncomeTaxViewChangeFrontend.getPaymentsDue(clientDetailsWithConfirmation)

    Then("The Payment Due what you owe page is returned to the user")
    result should have(
      httpStatus(OK),
      pageTitleAgent("whatYouOwe.heading-agent"),
      isElementVisibleById(s"money-in-your-account")(expectedValue = true),
      elementTextBySelector("#money-in-your-account")(
        messagesAPI("whatYouOwe.moneyOnAccount-agent") + " " +
          messagesAPI("whatYouOwe.moneyOnAccount-1") + " £300.00" + " " +
          messagesAPI("whatYouOwe.moneyOnAccount-agent-2") + " " +
          messagesAPI("whatYouOwe.moneyOnAccount-3") + "."
      )
    )
  }
}
