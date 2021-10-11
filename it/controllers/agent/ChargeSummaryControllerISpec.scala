/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDate
import assets.BaseIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, propertyOnlyResponse, testValidFinancialDetailsModelJson}
import audit.models.ChargeSummaryAudit
import auth.MtdItUser
import config.featureswitch.{ChargeHistory, FeatureSwitching, PaymentAllocation, TxmEventsApproved}
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.DocumentDetailsStub.docDateDetail
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsModel, SubItem}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import assets.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndPropertyResponse, testChargeHistoryJson, testValidFinancialDetailsModelJson, twoDunningLocks, twoInterestLocks}


class ChargeSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val clientDetails: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val currentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val currentYear: Int = LocalDate.now().getYear
  val testArn: String = "1"

  s"GET ok" should {
    "load the page with right data for Payments Breakdown" in {
      Given("I wiremock stub a successful Income Source Details response with property only")
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      And("I wiremock stub a single financial transaction response")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(OK, testValidFinancialDetailsModelJson(10.34, 1.2,
        dunningLock = twoDunningLocks, interestLocks = twoInterestLocks))

      stubChargeHistorySuccess()



      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        "2018", "1040000124", clientDetails
      )

      Then("the result should have a HTTP status of OK (200) and load the correct page")
      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK"),
        elementTextBySelector("#heading-payment-breakdown")("Payment breakdown"),
        elementTextBySelector("article dl:nth-of-type(2) dd span")("Under review"),
        elementTextBySelector("article dl:nth-of-type(2) dd div")("We are not currently charging interest on this payment")
      )
    }

    s"return $OK with correct page title and audit events when TxEventsApproved FS is enabled" in {

      enable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      stubGetFinancialDetailsSuccess(Some("ITSA NI"))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        currentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetail(LocalDate.now().toString, "ITSA- POA 1"),
        agentReferenceNumber = Some("1")
      ))

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK"),
        elementTextBySelector("main h2")("Payment breakdown")
      )
    }

    s"return $OK with correct page title and no audit events when TxEventsApproved FS is disabled" in {

      disable(TxmEventsApproved)
      stubAuthorisedAgentUser(authorised = true)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

      stubGetFinancialDetailsSuccess(Some("ITSA NI"))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        currentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      AuditStub.verifyAuditDoesNotContainsDetail(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetail(LocalDate.now().toString, "ITSA- POA 1"),
        agentReferenceNumber = Some("1")
      ).detail)

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK"),
        elementTextBySelector("main h2")("Payment breakdown")
      )
    }

    s"return $OK with correct page title and audit events when TxEventsApproved and ChargeHistory and PaymentAllocation FSs are enabled" in {
      enable(TxmEventsApproved)
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()
      stubChargeHistorySuccess()

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        currentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK"),
        elementTextBySelector("main h2")("Payment history")
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetail(LocalDate.now().toString, "ITSA- POA 1"),
        agentReferenceNumber = Some("1")
      ))
    }

    s"return $OK with correct page title and audit events when TxEventsApproved ChargeHistory and PaymentAllocation FSs are enabled and LPI set to true" in {
      enable(TxmEventsApproved)
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      val result = IncomeTaxViewChangeFrontend.getChargeSummaryLatePayment(
        currentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      result should have(
        httpStatus(OK),
        pageTitle("Late payment interest on payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK"),
        elementTextBySelector("main h2")("Payment history")
      )

      AuditStub.verifyAuditEvent(ChargeSummaryAudit(
        MtdItUser(
          testMtditid, testNino, None,
          multipleBusinessesAndPropertyResponse, Some("1234567890"), None, Some("Agent"), Some(testArn)
        )(FakeRequest()),
        docDateDetail(LocalDate.now().toString, "ITSA- POA 1"),
        agentReferenceNumber = Some("1")
      ))
    }

    s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $NOT_FOUND" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(NOT_FOUND, Json.parse(
        """
          |{
          |   "code": "NO_DATA_FOUND",
          |   "reason": "The remote endpoint has indicated that no match found for the reference provided."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        currentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK")
      )
    }

    s"return $OK with correct page title and ChargeHistory FS is enabled and the charge history details API responds with a $FORBIDDEN" in {
      enable(ChargeHistory)
      enable(PaymentAllocation)
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
      stubGetFinancialDetailsSuccess()

      IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(FORBIDDEN, Json.parse(
        """
          |{
          |   "code": "REQUEST_NOT_PROCESSED",
          |   "reason": "The remote endpoint has indicated that request could not be processed."
          |}
          |""".stripMargin))

      val result = IncomeTaxViewChangeFrontend.getChargeSummary(
        currentTaxYearEnd.getYear.toString, "testId", clientDetails
      )

      result should have(
        httpStatus(OK),
        pageTitle("Payment on account 1 of 2 - Your client’s Income Tax details - GOV.UK")
      )
    }

    "return a technical difficulties page to the user" when {
      "ChargeHistory FS is enabled and the charge history details API responded with an error" in {
        enable(ChargeHistory)
        enable(PaymentAllocation)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        stubGetFinancialDetailsSuccess()

        IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(INTERNAL_SERVER_ERROR, Json.parse(
          """
            |{
            |   "code": "SERVER_ERROR",
            |   "reason": "DES is currently experiencing problems that require live service intervention."
            |}
            |""".stripMargin))

        val result = IncomeTaxViewChangeFrontend.getChargeSummary(
          currentTaxYearEnd.getYear.toString, "testId", clientDetails
        )

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500 - Business Tax account - GOV.UK")
        )
      }
    }

  }

  private def stubGetFinancialDetailsSuccess(chargeType: Option[String] = None): Unit = {
    IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
      nino = testNino,
      from = currentTaxYearEnd.minusYears(1).plusDays(1).toString,
      to = currentTaxYearEnd.toString
    )(
      status = OK,
      response = Json.toJson(FinancialDetailsModel(
        balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
        documentDetails = List(
          DocumentDetail(
            taxYear = currentTaxYearEnd.getYear.toString,
            transactionId = "testId",
            documentDescription = Some("ITSA- POA 1"),
            outstandingAmount = Some(1.2),
            originalAmount = Some(10.34),
            documentDate = LocalDate.of(2018, 3, 29),
            interestFromDate = Some(LocalDate.of(2018, 3, 29)),
            interestEndDate = Some(LocalDate.of(2018, 3, 29)),
            latePaymentInterestAmount = Some(100.0),
            interestOutstandingAmount = Some(80.0)
          )
        ),
        financialDetails = List(
          FinancialDetail(
            taxYear = currentTaxYearEnd.getYear.toString,
            transactionId = Some("testId"),
            mainType = Some("SA Payment on Account 1"),
            chargeType = chargeType,
            originalAmount = Some(10.34),
            items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
          )
        )
      ))
    )
  }

  private def stubChargeHistorySuccess() = {
    IncomeTaxViewChangeStub.stubChargeHistoryResponse(testMtditid, "testId")(OK, Json.obj(
      "idType" -> "MTDBSA",
      "idValue" -> testMtditid,
      "regimeType" -> "ITSA",
      "chargeHistoryDetails" -> Json.arr(
        Json.obj(
          "taxYear" -> currentTaxYearEnd.getYear.toString,
          "documentId" -> "testId",
          "documentDate" -> "2018-03-29",
          "documentDescription" -> "ITSA- POA 1",
          "totalAmount" -> 123456789012345.67,
          "reversalDate" -> "2020-02-24",
          "reversalReason" -> "amended return"
        ))))
  }

}
