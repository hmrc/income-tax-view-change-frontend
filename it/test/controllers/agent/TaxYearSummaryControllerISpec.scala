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

import audit.models.{NextUpdatesResponseAuditModel, TaxYearSummaryResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditEvent}
import helpers.servicemocks.AuthStub.{titleInternalServer, titleProbWithService}
import helpers.servicemocks.{CalculationListStub, IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.admin.{AdjustPaymentsOnAccount, ReviewAndReconcilePoa}
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import models.taxyearsummary.TaxYearSummaryChargeItem
import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.CalculationListIntegrationTestConstants.successResponseNonCrystallised
import testConstants.IncomeSourceIntegrationTestConstants.{singleBusinessResponseWoMigration, testEmptyFinancialDetailsModelJson, testValidFinancialDetailsModelJson, testValidFinancialDetailsModelReviewAndReconcileDebitsJson}
import testConstants.NewCalcBreakdownItTestConstants.{liabilityCalculationModelDeductionsMinimal, liabilityCalculationModelSuccessful}
import testConstants.messages.TaxYearSummaryMessages._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class TaxYearSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  lazy val fixedDate: LocalDate = LocalDate.of(2023, 11, 29)

  val emptyCTAModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = false, poaTaxYear = None)

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      Some("Test Trading Name"),
      None,
      Some(getCurrentTaxYearEnd),
      None,
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = List(
      PropertyDetailsModel(
        "testId2",
        Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
        None,
        None,
        Some(getCurrentTaxYearEnd),
        None,
        cashOrAccruals = false
      )
    )
  )
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  import implicitDateFormatter.longDate

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(100.00),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 6, 24)),
        documentDueDate = Some(LocalDate.of(2021, 6, 24))
      )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Payment on Account 1"),
        mainTransaction = Some("4920"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(fixedDate))))
      )
    )
  )

  val financialDetailsDunningLockSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testDunningTransactionId",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(100.00),
        effectiveDateOfPayment = Some(fixedDate),
        documentDueDate = Some(fixedDate)
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testDunningTransactionId2",
        documentDescription = Some("ITSA - POA 2"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 2000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        effectiveDateOfPayment = Some(fixedDate.plusDays(2)),
        documentDueDate = Some(fixedDate.plusDays(2))
      )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId"),
        mainType = Some("SA Payment on Account 1"),
        mainTransaction = Some("4920"),
        items = Some(Seq(SubItem(Some(fixedDate), amount = Some(12), dunningLock = Some("Stand over order"), transactionId = Some("testDunningTransactionId"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId2"),
        mainType = Some("SA Payment on Account 2"),
        mainTransaction = Some("4930"),
        items = Some(Seq(SubItem(Some(fixedDate), amount = Some(12), dunningLock = Some("Dunning Lock"), transactionId = Some("testDunningTransactionId2"))))
      )
    )
  )

  val financialDetailsMFADebits: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testMFA1",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 1234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testMFA2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = 2234.00,
        outstandingAmount = 0,
        interestOutstandingAmount = None,
        interestEndDate = None,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 22)),
        documentDueDate = Some(LocalDate.of(2021, 4, 22))
      )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testMFA1"),
        mainType = Some("ITSA PAYE Charge"),
        mainTransaction = Some("4000"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), transactionId = Some("testMFA1"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testMFA2"),
        mainType = Some("ITSA Calc Error Correction"),
        mainTransaction = Some("4001"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 22)), amount = Some(12), transactionId = Some("testMFA2"))))
      )
    )
  )

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "testId",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusFulfilled
        )
      )
    ),
    GroupedObligationsModel(
      identification = "testId2",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd.minusDays(1)),
          periodKey = "#004",
          StatusFulfilled
        )
      )
    )
  ))

  val quarterlyToPeriod: String = s"Quarterly period from ${getCurrentTaxYearEnd.minusMonths(3).toLongDateShort} " +
    s"to ${getCurrentTaxYearEnd.toLongDateShort}"


  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetailsSuccess,
    None, Some("1234567890"), None, Some(Agent), arn = Some("1")
  )(FakeRequest())

  s"[IT-AGENT-TEST-1] GET ${controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}" should {
    s" [IT-AGENT-TEST-1.1] redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn.url}" when {
      " [IT-AGENT-TEST-1.1.1] the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn.url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn.url)
        )
      }
    }
    s" [IT-AGENT-TEST-1.2] return $OK with technical difficulties" when {
      " [IT-AGENT-TEST-1.2.1] the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }
    s" [IT-AGENT-TEST-1.3] return $SEE_OTHER" when {
      " [IT-AGENT-TEST-1.3.1] the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }
      " [IT-AGENT-TEST-1.3.2] the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show.url)
        )
      }
    }
  }

  s"[IT-AGENT-TEST-2] GET ${controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}" should {
    " [IT-AGENT-TEST-2.1] return the tax year summary page" when {
      " [IT-AGENT-TEST-2.1.1] all calls were successful and returned data" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )


        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )


        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("tax-year-summary.heading"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-child(2)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "a")(poa1),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£1,000.00"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "a")(poa1Lpi),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£100.00"),
          elementTextBySelectorList("#updates", "div", "h3")(updateTabDue(getCurrentTaxYearEnd.toLongDate)),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"$quarterlyToPeriod"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(propertyIncome),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          ),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(propertyIncome),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          )
        )

        verifyAuditEvent(TaxYearSummaryResponseAuditModel(testUser, messagesAPI,
          TaxYearSummaryViewModel(
            Some(CalculationSummary(liabilityCalculationModelSuccessful)),
            financialDetailsSuccess.documentDetails.map(dd => ChargeItem.fromDocumentPair(dd, financialDetailsSuccess.financialDetails, true, true)).map(TaxYearSummaryChargeItem.fromChargeItem),
            allObligations, codingOutEnabled = true, reviewAndReconcileEnabled = true, showForecastData = true, ctaViewModel = emptyCTAModel)))
        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }


      }

      " [IT-AGENT-TEST-2.1.2] should show Tax Year Summary page with payments with and without dunning locks in the payments tab" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsDunningLockSuccess)
        )

        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("tax-year-summary.heading"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-child(2)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$poa1Lpi $underReview"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£100.00"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "th")(s"$poa1 $underReview"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")(fixedDate.toLongDateShort),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div", "h3")(updateTabDue(getCurrentTaxYearEnd.toLongDate)),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"$quarterlyToPeriod"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(propertyIncome),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          ),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(propertyIncome),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          )
        )

        verifyAuditEvent(TaxYearSummaryResponseAuditModel(testUser,
          messagesAPI,
          TaxYearSummaryViewModel(
            Some(CalculationSummary(liabilityCalculationModelSuccessful)),
            financialDetailsDunningLockSuccess.documentDetails.
              map(dd => ChargeItem.fromDocumentPair(dd, financialDetailsDunningLockSuccess.financialDetails, true, true)).map(TaxYearSummaryChargeItem.fromChargeItem),
            allObligations,
            codingOutEnabled = true, reviewAndReconcileEnabled = true, showForecastData = true, ctaViewModel = emptyCTAModel)))
        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }


      }
      " [IT-AGENT-TEST-2.1.3] Calculation List was not found" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = NO_CONTENT,
          body = LiabilityCalculationError(NO_CONTENT, "not found")
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("tax-year-summary.heading"),
          elementTextByID("no-calc-data-header")(noCalcHeading),
          elementTextByID("no-calc-data-note")(noCalcNote)
        )

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }
      }
      " [IT-AGENT-TEST-2.1.5] financial details data was not found" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = NOT_FOUND,
          response = Json.obj()
        )

        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("tax-year-summary.heading"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(1)", "dd:nth-child(2)")("15 February 2019"),
          elementTextBySelectorList("#main-content", "dl", "div:nth-of-type(2)", "dd:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(1)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(2)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(3)", "td:nth-of-type(1)")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#payments", "p")(noPaymentsDue),
          elementTextBySelectorList("#updates", "div", "h3")(updateTabDue(getCurrentTaxYearEnd.toLongDate)),
          elementTextBySelectorList("#updates", "div", "table", "caption")(
            expectedValue = s"$quarterlyToPeriod"
          ),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(propertyIncome),
          elementTextBySelectorList("#updates", "div", "table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          ),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")(propertyIncome),
          elementTextBySelectorList("#updates", "div", "table:eq(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")(
            expectedValue = s"${getCurrentTaxYearEnd.minusDays(1).toLongDateShort}"
          )
        )

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }

      }
    }
    " [IT-AGENT-TEST-2.3] return a technical difficulties page to the user" when {
      " [IT-AGENT-TEST-2.3.1] there was a problem retrieving the client's income sources" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = INTERNAL_SERVER_ERROR,
          response = incomeSourceDetailsSuccess
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual(titleProbWithService, isErrorPage = true)
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

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
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

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )

      }
      " [IT-AGENT-TEST-2.3.5] there was a problem retrieving obligations" in {
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

        IncomeTaxViewChangeStub.stubGetAllObligationsError(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd
        )

        val result = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent(titleInternalServer, isErrorPage = true)
        )
      }
    }
    "MFA Debits" when {
      def testMFADebits(): Any = {
        disable(AdjustPaymentsOnAccount)
        stubAuthorisedAgentUser(authorised = true)
        setupMFADebitsTests()

        And("The expected result is returned")
        verifyMFADebitsResults(IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation))
      }

      def setupMFADebitsTests(): Unit = {
        Given("A successful getIncomeSourceDetails call is made")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
          status = OK,
          response = incomeSourceDetailsSuccess
        )

        And(s"A non crystallised calculation for ${getCurrentTaxYearEnd.getYear.toString} is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A successful getFinancialDetails call is made")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsMFADebits)
        )

        And("A obligations call is made")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )
      }

      def verifyMFADebitsResults(result: WSResponse): Any = {
        val auditDD = financialDetailsMFADebits.getAllDocumentDetailsWithDueDates()

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        verifyAuditEvent(TaxYearSummaryResponseAuditModel(testUser, messagesAPI,
          TaxYearSummaryViewModel(
            Some(CalculationSummary(liabilityCalculationModelSuccessful)),
            auditDD.map(dd => ChargeItem.fromDocumentPair(dd.documentDetail, financialDetailsMFADebits.financialDetails, true, true)).map(TaxYearSummaryChargeItem.fromChargeItem),
            allObligations, codingOutEnabled = true, reviewAndReconcileEnabled = true, showForecastData = true, ctaViewModel = emptyCTAModel)))

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }
        result should have(
          httpStatus(OK),
          pageTitleAgent("tax-year-summary.heading"),
          elementTextBySelector("#calculation-date")("15 February 2019"),
          elementTextBySelectorList("#payments-table", "tbody", "tr:nth-of-type(1)", "th")(s"$hmrcAdjustment"),
          elementTextBySelectorList("#payments-table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("22 Apr 2021"),
          elementTextBySelectorList("#payments-table", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£2,234.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "th")(s"$hmrcAdjustment"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£1,234.00"),
          elementCountBySelector("#payments-table", "tbody", "tr")(2)
        )
      }

      "should show Tax Year Summary page with MFA Debits on the Payment Tab" in {
        testMFADebits()
      }
    }

    "Review and Reconcile debit charges should" should {
      "render in the charges table" when {
        "the user has Review and Reconcile debit charges and ReviewAndReconcilePoa FS is enabled" in {
          enable(ReviewAndReconcilePoa)
          stubAuthorisedAgentUser(authorised = true)

          Given("A successful getIncomeSourceDetails call is made")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
            testValidFinancialDetailsModelReviewAndReconcileDebitsJson(2000, 2000, testYear2023.toString, futureDate.toString))

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

          val document = Jsoup.parse(res.body)

          document.getElementById("paymentTypeLink-0").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(testYear2023, "1040000123").url
          document.getElementById("paymentTypeLink-1").attr("href") shouldBe controllers.routes.ChargeSummaryController.showAgent(testYear2023, "1040000124").url

          res should have(
            httpStatus(OK),
            pageTitleAgent("tax-year-summary.heading"),
            elementTextByID("paymentTypeLink-0")("First payment on account: extra amount from your tax return"),
            elementTextByID("paymentTypeLink-1")("Second payment on account: extra amount from your tax return"),
            isElementVisibleById("accrues-interest-tag")(expectedValue = true))
        }
      }
    }

    "Claim to adjust POA section" should {
      "show" when {
        "The user has amendable POAs for the given tax year and the FS is Enabled" in {
          enable(AdjustPaymentsOnAccount)
          stubAuthorisedAgentUser(authorised = true)

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
            testValidFinancialDetailsModelJson(2000, 2000, testYear2023.toString, testDate.toString))

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

          res should have(
            httpStatus(OK),
            pageTitleAgent("tax-year-summary.heading"),
            isElementVisibleById("adjust-poa-link")(expectedValue = true))
        }
      }
      "not show" when {
        "The user has amendable POAs for the given tax year but the FS is Disabled" in {
          disable(AdjustPaymentsOnAccount)
          stubAuthorisedAgentUser(authorised = true)

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
            testValidFinancialDetailsModelJson(2000, 2000, testYear2023.toString, testDate.toString))

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

          res should have(
            httpStatus(OK),
            pageTitleAgent("tax-year-summary.heading"),
            isElementVisibleById("adjust-poa-link")(expectedValue = false))
        }
        "The user has no amendable POAs and the FS is Enabled" in {
          enable(AdjustPaymentsOnAccount)
          stubAuthorisedAgentUser(authorised = true)

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $getCurrentTaxYearEnd is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK, testEmptyFinancialDetailsModelJson)

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation)

          res should have(
            httpStatus(OK),
            pageTitleAgent("tax-year-summary.heading"),
            isElementVisibleById("adjust-poa-link")(expectedValue = false))
        }
      }
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(false, 2,
        () => IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear)(clientDetailsWithConfirmation))
    }
  }
}
