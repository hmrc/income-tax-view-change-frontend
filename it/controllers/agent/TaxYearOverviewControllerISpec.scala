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

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcBreakdownIntegrationTestConstants.calculationDataSuccessModel
import testConstants.CalcDataIntegrationTestConstants.estimatedCalculationFullJson
import testConstants.messages.TaxYearOverviewMessages.agentTitle
import audit.models.{NextUpdatesResponseAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import controllers.Assets.INTERNAL_SERVER_ERROR
import controllers.agent.utils.SessionKeys
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditDoesNotContainsDetail, verifyAuditEvent}
import helpers.servicemocks.{IncomeTaxViewChangeStub, IndividualCalculationStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation.{CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.financialDetails.{BalanceDetails, DocumentDetail, FinancialDetail, FinancialDetailsModel, SubItem}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.nextUpdates.{ObligationsModel, NextUpdateModel, NextUpdatesModel}
import play.api.http.Status.{NOT_FOUND, OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.{LocalDate, LocalDateTime}

class TaxYearOverviewControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val clientDetailsWithoutConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid
  )
  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )
  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }
  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
      Some("Test Trading Name"), None, None, None, None, None, None, None,
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        "testId2",
        AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
        None, None, None, None,
        Some(getCurrentTaxYearEnd)
      )
    )
  )
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  import implicitDateFormatter.longDate

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(
    balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
    documentDetails = List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        outstandingAmount = Some(500.00),
        originalAmount = Some(1000.00),
        documentDate = LocalDate.of(2018, 3, 29),
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(100.00)
      )
    ),
    financialDetails = List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Payment on Account 1"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.now.toString))))
      )
    )
  )
  val financialDetailsDunningLockSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    List(
    DocumentDetail(
      taxYear = getCurrentTaxYearEnd.getYear.toString,
      transactionId = "testDunningTransactionId",
      documentDescription = Some("ITSA- POA 1"),
      documentText = Some("documentText"),
      documentDate = LocalDate.of(2018, 3, 29),
      originalAmount = Some(1000.00),
      outstandingAmount = Some(500.00),
      interestOutstandingAmount = Some(0.00),
      interestEndDate = Some(LocalDate.of(2021, 6, 24)),
      latePaymentInterestAmount = Some(100.00)
    ),
    DocumentDetail(
      taxYear = getCurrentTaxYearEnd.getYear.toString,
      transactionId = "testDunningTransactionId2",
      documentDescription = Some("ITSA - POA 2"),
      documentText = Some("documentText"),
      documentDate = LocalDate.of(2018, 3, 29),
      originalAmount = Some(2000.00),
      outstandingAmount = Some(500.00),
      interestOutstandingAmount = Some(0.00),
      interestEndDate = Some(LocalDate.of(2021, 6, 24))
    )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId"),
        mainType = Some("SA Payment on Account 1"),
        items = Some(Seq(SubItem(Some(LocalDate.now.toString), amount = Some(12), dunningLock = Some("Stand over order"), transactionId = Some("testDunningTransactionId"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId2"),
        mainType = Some("SA Payment on Account 2"),
        items = Some(Seq(SubItem(Some(LocalDate.now.toString), amount = Some(12), dunningLock = Some("Dunning Lock"), transactionId = Some("testDunningTransactionId2"))))
      )
    )
  )
  val currentObligationsSuccess: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "testId",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "EOPS",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "EOPS"
        )
      )
    )
  ))
  val previousObligationsSuccess: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "testId2",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd.minusDays(1)),
          periodKey = "#004"
        )
      )
    )
  ))

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "testId",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "EOPS",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "EOPS"
        )
      )
    ),
    NextUpdatesModel(
      identification = "testId2",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd.minusDays(1)),
          periodKey = "#004"
        )
      )
    )
  ))

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))),
    incomeSourceDetailsSuccess, Some("1234567890"), None, Some("Agent"), arn = Some("1")
  )(FakeRequest())

  s"GET ${routes.TaxYearOverviewController.show(getCurrentTaxYearEnd.getYear).url}" should {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
  }

  s"GET ${routes.TaxYearOverviewController.show(getCurrentTaxYearEnd.getYear).url}" should {
    "return the tax year overview page with TxmEventsApproved FS enabled" when {
      "all calls were successful and returned data" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(2)")("6 July 2017"),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td[class=numeric]")("£199,505.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(4)", "td[class=numeric total-section]")("£198,500.00"),
          elementTextBySelectorList("#taxCalculation", "table:nth-of-type(2)", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDate}"
          )
        )

        verifyAuditEvent(TaxYearOverviewResponseAuditModel(testUser, calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, allObligations))
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)

      }

      "should show Tax Year Overview page with payments with and without dunning locks in the payments tab" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsDunningLockSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(2)")("6 July 2017"),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td[class=numeric]")("£199,505.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(4)", "td[class=numeric total-section]")("£198,500.00"),
          elementTextBySelectorList("#taxCalculation", "table:nth-of-type(2)", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2 Payment under review"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Payment on account 1 of 2 Payment under review"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDate}"
          )
        )

        verifyAuditEvent(TaxYearOverviewResponseAuditModel(testUser, calculationDataSuccessModel, financialDetailsDunningLockSuccess.getAllDocumentDetailsWithDueDates, allObligations))
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)

      }
      "Calculation List was not found" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationListNotFound(testNino, calculationTaxYear)

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextByID("no-calc-data-header")("No calculation yet"),
          elementTextByID("no-calc-data-note")("You will be able to see your latest tax year calculation here once you have sent an update and viewed it in your software.")
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
      "Calculation data was not found" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculationNotFound(testNino, "2017-18")

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextByID("no-calc-data-header")("No calculation yet"),
          elementTextByID("no-calc-data-note")("You will be able to see your latest tax year calculation here once you have sent an update and viewed it in your software.")
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)

      }
      "financial details data was not found" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = NOT_FOUND,
          response = Json.obj()
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(2)")("6 July 2017"),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td[class=numeric]")("£199,505.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(4)", "td[class=numeric total-section]")("£198,500.00"),
          elementTextBySelectorList("#taxCalculation", "table:nth-of-type(2)", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "p")("No payments currently due."),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDate}"
          )
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
      "previous obligations data was not found" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(2)")("6 July 2017"),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td[class=numeric]")("£199,505.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(4)", "td[class=numeric total-section]")("£198,500.00"),
          elementTextBySelectorList("#taxCalculation", "table:nth-of-type(2)", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")(LocalDate.of(2021, 6, 24).toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDate}"
          )
        )

        verifyAuditEvent(TaxYearOverviewResponseAuditModel(testUser, calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, currentObligationsSuccess))
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
    }
    "return the tax year overview page with TxmEventsApproved FS disabled" when {
      "all calls were successful and returned data" in {
        disable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(2)")("6 July 2017"),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td[class=numeric]")("£199,505.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(4)", "td[class=numeric total-section]")("£198,500.00"),
          elementTextBySelectorList("#taxCalculation", "table:nth-of-type(2)", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")(LocalDate.of(2021, 6, 24).toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDate}"
          )
        )

        verifyAuditDoesNotContainsDetail(TaxYearOverviewResponseAuditModel(testUser, calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, allObligations).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)

      }
      "previous obligations data was not found" in {
        disable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitle(agentTitle),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(2)")("6 July 2017"),
          elementTextBySelectorList("#content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td[class=numeric]")("£199,505.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(4)", "td[class=numeric total-section]")("£198,500.00"),
          elementTextBySelectorList("#taxCalculation", "table:nth-of-type(2)", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")(LocalDate.of(2021, 6, 24).toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDate}"
          )
        )

        verifyAuditDoesNotContainsDetail(TaxYearOverviewResponseAuditModel(testUser, calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, currentObligationsSuccess).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
    }
    "return a technical difficulties page to the user" when {
      "there was a problem retrieving the client's income sources" in {
        enable(TxmEventsApproved)

        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = INTERNAL_SERVER_ERROR,
          response = incomeSourceDetailsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500 - Business Tax account - GOV.UK")
        )

      }
      "there was a problem retrieving the calculation list for the tax year" in {
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = INTERNAL_SERVER_ERROR,
          body = ListCalculationItems(Seq())
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )
      }
      "there was a problem retrieving the calculation for the tax year" in {
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = INTERNAL_SERVER_ERROR,
          body = estimatedCalculationFullJson
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )

      }
      "there was a problem retrieving financial details for the tax year" in {
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = INTERNAL_SERVER_ERROR,
          response = Json.obj()
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )

      }
      "there was a problem retrieving current obligations" in {
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdatesError(
          nino = testNino
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )

      }
      "there was a problem retrieving previous obligations" in {
        enable(TxmEventsApproved)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(
            CalculationItem("calculationId1", LocalDateTime.of(2020, 4, 6, 12, 0))
          ))
        )

        IndividualCalculationStub.stubGetCalculation(testNino, "calculationId1")(
          status = OK,
          body = estimatedCalculationFullJson
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        IncomeTaxViewChangeStub.stubGetPreviousObligationsError(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, there is a problem with the service - Business Tax account - GOV.UK")
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
    }
  }
}
