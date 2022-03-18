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

import audit.models.{NextUpdatesResponseAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditEvent}
import helpers.servicemocks.AuthStub.{titleInternalServer, titleTechError}
import helpers.servicemocks.{IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SEE_OTHER}
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull
import testConstants.messages.TaxYearOverviewMessages.taxYearOverviewTitle
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class TaxYearOverviewControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
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
    ),
    codingDetails = None
  )
  val financialDetailsDunningLockSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    None,
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
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetailsSuccess,
    None, Some("1234567890"), None, Some("Agent"), arn = Some("1")
  )(FakeRequest())

  s"[IT-AGENT-TEST-1] GET ${routes.TaxYearOverviewController.show(getCurrentTaxYearEnd.getYear).url}" should {
    s" [IT-AGENT-TEST-1.1] redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      " [IT-AGENT-TEST-1.1.1] the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s" [IT-AGENT-TEST-1.2] return $OK with technical difficulties" when {
      " [IT-AGENT-TEST-1.2.1] the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer)
        )
      }
    }
    s" [IT-AGENT-TEST-1.3] return $SEE_OTHER" when {
      " [IT-AGENT-TEST-1.3.1] the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      " [IT-AGENT-TEST-1.3.2] the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }
  }

  s"[IT-AGENT-TEST-2] GET ${routes.TaxYearOverviewController.show(getCurrentTaxYearEnd.getYear).url}" should {
    " [IT-AGENT-TEST-2.1] return the tax year overview page" when {
      " [IT-AGENT-TEST-2.1.1] all calls were successful and returned data" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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
          pageTitleAgent(taxYearOverviewTitle),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(1)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(2)")("−£17,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#taxdue-payments-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDateShort}"
          )
        )

        verifyAuditEvent(TaxYearOverviewResponseAuditModel(testUser, financialDetailsSuccess.getAllDocumentDetailsWithDueDates,
          allObligations, Some(TaxYearOverviewViewModel(liabilityCalculationModelSuccessFull))))
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)

      }

      " [IT-AGENT-TEST-2.1.2] should show Tax Year Overview page with payments with and without dunning locks in the payments tab" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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
          pageTitleAgent(taxYearOverviewTitle),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(1)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(2)")("−£17,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#taxdue-payments-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2 Payment under review"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Payment on account 1 of 2 Payment under review"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDateShort}"
          )
        )

        verifyAuditEvent(TaxYearOverviewResponseAuditModel(testUser, financialDetailsDunningLockSuccess.getAllDocumentDetailsWithDueDates,
          allObligations, Some(TaxYearOverviewViewModel(liabilityCalculationModelSuccessFull))))
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)

      }
      " [IT-AGENT-TEST-2.1.3] Calculation List was not found" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = NOT_FOUND,
          body = LiabilityCalculationError(NOT_FOUND, "not found")
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
          pageTitleAgent(taxYearOverviewTitle),
          elementTextByID("no-calc-data-header")("No calculation yet"),
          elementTextByID("no-calc-data-note")("You will be able to see your latest tax year calculation here once you have sent an update and viewed it in your software.")
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
      " [IT-AGENT-TEST-2.1.5] financial details data was not found" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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
          pageTitleAgent(taxYearOverviewTitle),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(1)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(2)")("−£17,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#taxdue-payments-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£90,500.99"),
          elementTextBySelectorList("#payments", "p")("No payments currently due."),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Property income"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDateShort}"
          )
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId2", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
      " [IT-AGENT-TEST-2.1.6] previous obligations data was not found" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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
          pageTitleAgent(taxYearOverviewTitle),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-of-type(1)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(2)")("−£17,500.99"),
          elementTextBySelectorList("#income-deductions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(2)")("£12,500.00"),
          elementTextBySelectorList("#taxdue-payments-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(2)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(LocalDate.of(2021, 6, 24).toLongDateShort),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")(LocalDate.now.toLongDateShort),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(s"Due ${getCurrentTaxYearEnd.toLongDate}"),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"${getCurrentTaxYearEnd.minusMonths(3).toLongDate} to ${getCurrentTaxYearEnd.toLongDate}"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("Test Trading Name"),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")(
            expectedValue = s"${getCurrentTaxYearEnd.toLongDateShort}"
          )
        )

        verifyAuditEvent(TaxYearOverviewResponseAuditModel(testUser, financialDetailsSuccess.getAllDocumentDetailsWithDueDates,
          currentObligationsSuccess, Some(TaxYearOverviewViewModel(liabilityCalculationModelSuccessFull))))
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
    }
    " [IT-AGENT-TEST-2.3] return a technical difficulties page to the user" when {
      " [IT-AGENT-TEST-2.3.1] there was a problem retrieving the client's income sources" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = INTERNAL_SERVER_ERROR,
          response = incomeSourceDetailsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual(titleTechError)
        )

      }
      " [IT-AGENT-TEST-2.3.2] there was a problem retrieving the calculation list for the tax year" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = INTERNAL_SERVER_ERROR,
          body = LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error")
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent(titleInternalServer)
        )
      }
      " [IT-AGENT-TEST-2.3.4] there was a problem retrieving financial details for the tax year" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
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
          pageTitleAgent(titleInternalServer)
        )

      }
      " [IT-AGENT-TEST-2.3.5] there was a problem retrieving current obligations" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
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
          pageTitleAgent(titleInternalServer)
        )

      }
      " [IT-AGENT-TEST-2.3.6] there was a problem retrieving previous obligations" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
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
          pageTitleAgent(titleInternalServer)
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)
      }
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(false, 2,
        () => IncomeTaxViewChangeFrontend.getTaxYearOverview(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation))
    }
  }
}
