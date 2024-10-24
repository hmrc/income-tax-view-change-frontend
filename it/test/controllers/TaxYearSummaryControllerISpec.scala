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

package controllers

import audit.models.{NextUpdatesResponseAuditModel, TaxYearSummaryResponseAuditModel}
import auth.MtdItUser
import config.featureswitch._
import enums.CodingOutType._
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.{verifyAuditContainsDetail, verifyAuditEvent}
import helpers.servicemocks._
import models.admin._
import models.financialDetails._
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.{CalculationSummary, TYSClaimToAdjustViewModel, TaxYearSummaryViewModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import models.taxyearsummary.TaxYearSummaryChargeItem
import org.jsoup.Jsoup
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants.successResponseNonCrystallised
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants._
import testConstants.messages.TaxYearSummaryMessages._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class TaxYearSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(NavBarFs)
  }

  val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

  val emptyCTAModel: TYSClaimToAdjustViewModel = TYSClaimToAdjustViewModel(adjustPaymentsOnAccountFSEnabled = false, poaTaxYear = None)

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
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Payment on Account 1"),
        mainTransaction = Some("4920"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)))))
      )
    )
  )

  val immediatelyRejectedByNps: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CLASS2_NICS),
        documentDate = LocalDate.of(2021, 8, 13),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(0),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2022, 1, 29),
        originalAmount = 1000.00,
        outstandingAmount = 0,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 1, 23))
      )
    ),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        mainTransaction = Some("4910"),
        transactionId = Some("testTransactionId2"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 23)))))
      )
    )
  )

  val rejectedByNpsPartWay: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CLASS2_NICS),
        documentDate = LocalDate.of(2021, 1, 31),
        originalAmount = 1000.00,
        outstandingAmount = 500.00,
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(0),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CANCELLED),
        documentDate = LocalDate.of(2021, 8, 31),
        originalAmount = 1000.00,
        outstandingAmount = 1000.00,
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      )
    ),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 23)))))
      )
    )
  )

  val codingOutPartiallyCollected: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId1",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CLASS2_NICS),
        documentDate = LocalDate.of(2021, 9, 13),
        originalAmount = 1000.00,
        outstandingAmount = 0,
        interestOutstandingAmount = Some(0.00),
        latePaymentInterestAmount = Some(0),
        effectiveDateOfPayment = Some(LocalDate.of(2021, 8, 23)),
        documentDueDate = Some(LocalDate.of(2021, 8, 23))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId2",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2021, 8, 31),
        originalAmount = 250,
        outstandingAmount = 100,
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 30)),
        documentDueDate = Some(LocalDate.of(2021, 4, 30))
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear,
        transactionId = "testTransactionId3",
        documentDescription = Some("TRM New Charge"),
        documentText = Some(CODING_OUT_CANCELLED),
        documentDate = LocalDate.of(2022, 1, 31),
        originalAmount = 1000.00,
        outstandingAmount = 0,
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        effectiveDateOfPayment = Some(LocalDate.of(2022, 1, 31)),
        documentDueDate = Some(LocalDate.of(2022, 1, 31))
      )
    ),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId1"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2020, 7, 13)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 8, 31)))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId3"),
        mainTransaction = Some("4910"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 31)))))
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
      effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
      documentDueDate = Some(LocalDate.of(2021, 4, 23))
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
        effectiveDateOfPayment = Some(LocalDate.of(2021, 4, 23)),
        documentDueDate = Some(LocalDate.of(2021, 4, 23))
      )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId"),
        mainType = Some("SA Payment on Account 1"),
        mainTransaction = Some("4920"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), dunningLock = Some("Stand over order"), transactionId = Some("testDunningTransactionId"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId2"),
        mainType = Some("SA Payment on Account 2"),
        mainTransaction = Some("4930"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23)), amount = Some(12), dunningLock = Some("Dunning Lock"), transactionId = Some("testDunningTransactionId2"))))
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

  val emptyPaymentsList: List[TaxYearSummaryChargeItem] = List.empty

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "ABC123456789",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusFulfilled
        ))
    ),
    GroupedObligationsModel(
      identification = "ABC123456789",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#004",
          StatusFulfilled
        ))
    )
  ))

  unauthorisedTest(s"/tax-year-summary/$testYear")

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())

  s"GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}" when {
    "ForecastCalculation feature" should {
      def testForecast(): Unit = {
        And("Income Source Details is stubbed")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessfulNotCrystallised
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        And("The expected result is returned")
        val tableText = "Forecast Section Amount Income £12,500.00 Allowances and deductions £4,200.00 Total income on which tax is due £12,500.00 " +
          "Forecast Self Assessment tax amount £5,000.99"
        val forecastTabHeader = messagesAPI("tax-year-summary.forecast")
        val forecastTotal = s"${
          messagesAPI("tax-year-summary.forecast_total_title", (getCurrentTaxYearEnd.getYear - 1).toString,
            getCurrentTaxYearEnd.getYear.toString)
        } £5,000.99"
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelector("#calculation-date")("15 February 2019"),
          elementTextBySelector("#forecast_total")(forecastTotal),
          elementTextBySelector("""a[href$="#forecast"]""")(forecastTabHeader),
          elementTextBySelector(".forecast_table")(tableText)
        )
      }

      "should show the forecast calculation tab" in {
        testForecast()
      }
    }

    "Tax years overview page" should {
      "should show the updated Tax Year summary page" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        And("all obligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }

        And("The expected result is returned")

        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelector("#calculation-date")("15 February 2019"),
          elementTextBySelector("#income-deductions-contributions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
          elementTextBySelector("#income-deductions-contributions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $poa1"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£1,000.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "a")(poa1Lpi),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£100.00"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString)
        )
      }

      "should show Tax Year Summary page with payments with and without dunning locks in the payments tab" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsDunningLockSuccess)
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelector("#calculation-date")("15 February 2019"),
          elementTextBySelector("#income-deductions-contributions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
          elementTextBySelector("#income-deductions-contributions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
          elementTextBySelectorList("#income-deductions-contributions-table", "tbody", "tr:nth-child(4)", "td:nth-of-type(1)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $poa1 $underReview"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("£1,000.00"),

          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "th")(s"$overdue $poa2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("£2,000.00"),

          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "th")(s"$poa1Lpi $underReview"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")("£100.00"),

          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "th:nth-of-type(1)")(quarterlyUpdate),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "table:eq(1) tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("5 Apr " + getCurrentTaxYearEnd.getYear.toString),
        )

        AuditStub.verifyAuditEvent(TaxYearSummaryResponseAuditModel(
          MtdItUser(testMtditid, testNino, None, singleBusinessResponse,
            None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
          )(FakeRequest()),
          messagesAPI, TaxYearSummaryViewModel(Some(CalculationSummary(liabilityCalculationModelSuccessfulExpected)),
            financialDetailsDunningLockSuccess.documentDetails.map(dd => ChargeItem.fromDocumentPair(dd, financialDetailsDunningLockSuccess.financialDetails, true, true)).map(TaxYearSummaryChargeItem.fromChargeItem),
            allObligations, codingOutEnabled = true, reviewAndReconcileEnabled = true, showForecastData = true, ctaViewModel = emptyCTAModel)))
      }


      "should show user has Coding out that is requested and immediately rejected by NPS" in {
        enable(CodingOut)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(immediatelyRejectedByNps)
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )


        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "a")(balancingPayment),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "th")(s"$overdue $class2Nic")

        )
      }

      "should show user has Coding out that has been accepted and rejected by NPS part way through the year" in {
        enable(CodingOut)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(rejectedByNpsPartWay)
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )


        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $class2Nic"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "th")(s"$overdue $cancelledPayeSA")


        )
      }

      "should show at crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in {
        enable(CodingOut)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(codingOutPartiallyCollected)
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          getCurrentTaxYearEnd.toString)


        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "th")(s"$overdue $balancingPayment"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "a")(class2Nic),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(3)", "a")(cancelledPayeSA)
        )
      }

      "financial details service returns a not found" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And(s"A financial transaction call returns a $NOT_FOUND")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = NOT_FOUND,
          response = Json.obj()
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString)

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }

        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelector("#payments p")(noPaymentsDue)
        )

        AuditStub.verifyAuditEvent(TaxYearSummaryResponseAuditModel(
          MtdItUser(testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
            None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
          )(FakeRequest()),
          messagesAPI,
          TaxYearSummaryViewModel(
            Some(CalculationSummary(liabilityCalculationModelSuccessful)),
            emptyPaymentsList,
            allObligations,
            codingOutEnabled = true, reviewAndReconcileEnabled = true, showForecastData = true, ctaViewModel = emptyCTAModel
          )))
      }

      "financial details service returns an error" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A non crystallised calculation for 2017-18 is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        And("A financial transaction call fails")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)


        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a calculation failed" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A calculation call for 2017-18 responds with http status 404:NO_CONTENT")
        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino,
          "2018")(NO_CONTENT, LiabilityCalculationError(NO_CONTENT, "error"))

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Seq(
            GroupedObligationsModel(
              "ABC123456789",
              List(SingleObligationModel(
                LocalDate.of(2017, 12, 28),
                LocalDate.of(2018, 2, 3),
                LocalDate.of(2018, 2, 4),
                "Quarterly",
                Some(LocalDate.of(2018, 2, 2)),
                "#001",
                StatusFulfilled
              ))
            )
          ))
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, "2018")

        And("The expected result with right headers are returned")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextByID("no-calc-data-header")(noCalcHeading),
          elementTextByID("no-calc-data-note")(noCalcNote)
        )
      }

      "retrieving a calculation failed with INTERNAL_SERVER_ERROR" in {

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A calculation call for 2017-18 fails")
        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino,
          "2018")(INTERNAL_SERVER_ERROR, LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Seq(
            GroupedObligationsModel(
              "ABC123456789",
              List(SingleObligationModel(
                LocalDate.of(2017, 12, 28),
                LocalDate.of(2018, 2, 3),
                LocalDate.of(2018, 2, 4),
                "Quarterly",
                Some(LocalDate.of(2018, 2, 2)),
                "#001",
                StatusFulfilled
              ))
            )
          ))
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, "2018")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a getAllObligations error" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(codingOutPartiallyCollected)
        )

        And("getAllObligations call failed")
        IncomeTaxViewChangeStub.stubGetAllObligationsError(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5))

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, LocalDate.of(2017, 4, 6).toString,
          LocalDate.of(2018, 4, 5).toString)


        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "calculation response contain error messages" in {

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        And("Calculation could not be completed due to errors")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelErrorMessages
        )

        And("A financial transaction call returns a success")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )(
          status = OK,
          response = Json.toJson(financialDetailsSuccess)
        )

        And("getAllObligations returns a success")
        IncomeTaxViewChangeStub.stubGetAllObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = allObligations
        )

        When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }

        And("The expected result is returned")
        val errMessages = liabilityCalculationModelErrorMessagesFormatted.messages.get.errorMessages

        res should have(
          httpStatus(OK),
          pageTitleIndividual("tax-year-summary.heading"),
          elementTextBySelector("dl")(""),
          elementTextBySelector("#forecast_total")(""),
          elementTextBySelector("#calculation-date")(""),
          elementTextBySelector("""a[href$="#forecast"]""")(""),
          elementTextBySelector(".forecast_table")(""),
          elementTextBySelectorList("#taxCalculation", "div h2")(messagesAPI("tax-year-summary.message.header")),
          elementTextBySelectorList("#taxCalculation", "div strong")("Warning " + messagesAPI("tax-year-summary.message.action")),
          elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(1)")(errMessages.head.text),
          elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(2)")(errMessages(1).text),
          elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(3)")(errMessages(2).text),
          elementTextBySelectorList("#taxCalculation", "ul > li:nth-child(4)")(errMessages(3).text),
        )

      }
    }

    "retrieving a calculation failed" in {
      Given("Business details returns a successful response back")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

      And("A financial transaction call returns a success")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
        nino = testNino,
        from = LocalDate.of(2017, 4, 6).toString,
        to = LocalDate.of(2018, 4, 5).toString
      )(
        status = OK,
        response = Json.toJson(codingOutPartiallyCollected)
      )

      And("getAllObligations returns a success")
      IncomeTaxViewChangeStub.stubGetAllObligations(
        nino = testNino,
        fromDate = LocalDate.of(2017, 4, 6),
        toDate = LocalDate.of(2018, 4, 5),
        deadlines = allObligations
      )

      And("A calculation call for 2017-18 fails")
      IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, "2018")(INTERNAL_SERVER_ERROR, LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Error"))

      When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
      val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear)

      Then("I check all calls expected were made")
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetAllObligations(testNino,
        LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)

      And("Internal server error is returned")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "retrieving a financial transaction failed" in {
      Given("Business details returns a successful response back")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

      And("A financial transaction call fails")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

      When(s"I call GET ${controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url}")
      val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear)

      Then("I check all calls expected were made")
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)

      And("Internal server error is returned")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "MFA Debits" should {
      def testMFADebits(): Any = {
        setupMFADebitsTests()
        verifyMFADebitsResults(IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString))
      }

      def setupMFADebitsTests(): Unit = {
        Given("A successful getIncomeSourceDetails call is made")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
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

        And("A getAllObligations call is made")
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
        verifyAuditEvent(TaxYearSummaryResponseAuditModel(
          MtdItUser(testMtditid, testNino, None, singleBusinessResponse,
            None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
          )(FakeRequest()),
          messagesAPI, TaxYearSummaryViewModel(
            Some(CalculationSummary(liabilityCalculationModelSuccessful)),

            auditDD.map(dd => ChargeItem.fromDocumentPair(dd.documentDetail, financialDetailsMFADebits.financialDetails, true, true)).map(TaxYearSummaryChargeItem.fromChargeItem), allObligations,
            codingOutEnabled = true,
            reviewAndReconcileEnabled = true,
            showForecastData = true, ctaViewModel = emptyCTAModel)))

        allObligations.obligations.foreach {
          obligation => verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, obligation.identification, obligation.obligations).detail)
        }
          result should have(
            httpStatus(OK),
            pageTitleIndividual("tax-year-summary.heading"),
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

      "should show Tax Year Summary page with MFA Debits on the Payment Tab with FS ENABLED" in {
        testMFADebits()
      }
    }

    "Review and Reconcile debit charges should" should {
      "render in the charges table" when {
        "the user has Review and Reconcile debit charges and ReviewAndReconcilePoa FS is enabled" in {
          enable(ReviewAndReconcilePoa)
          Given("A successful getIncomeSourceDetails call is made")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $calculationTaxYear is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $calculationTaxYear is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
            testValidFinancialDetailsModelReviewAndReconcileDebitsJson(2000, 2000, testYear2023.toString, futureDate.toString))

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

          val document = Jsoup.parse(res.body)

          document.getElementById("paymentTypeLink-0").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(testYear2023, "1040000123").url
          document.getElementById("paymentTypeLink-1").attr("href") shouldBe controllers.routes.ChargeSummaryController.show(testYear2023, "1040000124").url

          res should have(
            httpStatus(OK),
            pageTitleIndividual("tax-year-summary.heading"),
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

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $calculationTaxYear is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $calculationTaxYear is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
            testValidFinancialDetailsModelJson(2000, 2000, testYear2023.toString, testDate.toString))

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

          res should have(
            httpStatus(OK),
            pageTitleIndividual("tax-year-summary.heading"),
            isElementVisibleById("adjust-poa-link")(expectedValue = true))
        }
      }
      "not show" when {
        "The user has amendable POAs for the given tax year but the FS is Disabled" in {
          disable(AdjustPaymentsOnAccount)

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $calculationTaxYear is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $calculationTaxYear is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK,
            testValidFinancialDetailsModelJson(2000, 2000, testYear2023.toString, testDate.toString))

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

          res should have(
            httpStatus(OK),
            pageTitleIndividual("tax-year-summary.heading"),
            isElementVisibleById("adjust-poa-link")(expectedValue = false))
        }
        "The user has no amendable POAs and the FS is Enabled" in {
          enable(AdjustPaymentsOnAccount)

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $calculationTaxYear is returned")
          IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(status = OK, body = liabilityCalculationModelDeductionsMinimal)

          And(s"A non crystallised calculation for $calculationTaxYear is returned from calc list")
          CalculationListStub.stubGetCalculationList(testNino, "22-23")(successResponseNonCrystallised.toString)

          And("I wiremock stub financial details for TY22/23 with POAs")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino, s"${testYear2023 - 1}-04-06", s"$testYear2023-04-05")(OK, testEmptyFinancialDetailsModelJson)

          val res = IncomeTaxViewChangeFrontend.getTaxYearSummary(getCurrentTaxYearEnd.getYear.toString)

          res should have(
            httpStatus(OK),
            pageTitleIndividual("tax-year-summary.heading"),
            isElementVisibleById("adjust-poa-link")(expectedValue = false))
        }
      }
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(false, 2,
        () => IncomeTaxViewChangeFrontend.getTaxYearSummary(testYear))
    }
  }


}
