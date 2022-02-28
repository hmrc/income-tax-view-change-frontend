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

import audit.models.{NextUpdatesResponseAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks._
import models.financialDetails._
import models.liabilitycalculation.LiabilityCalculationError
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull
import testConstants.messages.TaxYearOverviewMessages
import testConstants.messages.TaxYearOverviewMessages.taxYearOverviewTitle

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaxYearOverviewControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    None,
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("ITSA- POA 1"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2018, 3, 29),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(500.00),
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(100.00)
      )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Payment on Account 1"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23).toString))))
      )
    )
  )

  val immediatelyRejectedByNps: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    None,
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("Class 2 National Insurance"),
        documentDate = LocalDate.of(2021, 8, 13),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(500.00),
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(0)
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2022, 1, 29),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(0)
      )
    ),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23).toString))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 23).toString))))
      )
    )
  )

  val rejectedByNpsPartWay: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    None,
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("Class 2 National Insurance"),
        documentDate = LocalDate.of(2021, 1, 31),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(500.00),
        interestOutstandingAmount = Some(0.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24)),
        latePaymentInterestAmount = Some(0)
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("Cancelled PAYE Self Assessment"),
        documentDate = LocalDate.of(2021, 8, 31),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(1000.00),
        interestEndDate = Some(LocalDate.of(2021, 6, 24))

      )
    ),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23).toString))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 23).toString))))
      )
    )
  )

  val codingOutPartiallyCollected: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    None,
    List(
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("Class 2 National Insurance"),
        documentDate = LocalDate.of(2021, 9, 13),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(0),
        interestOutstandingAmount = Some(0.00),
        latePaymentInterestAmount = Some(0)
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("documentText"),
        documentDate = LocalDate.of(2021, 8, 31),
        originalAmount = Some(250),
        outstandingAmount = Some(100)
      ),
      DocumentDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = "testTransactionId",
        documentDescription = Some("TRM New Charge"),
        documentText = Some("Cancelled PAYE Self Assessment"),
        documentDate = LocalDate.of(2022, 1, 31),
        originalAmount = Some(1000.00),
        outstandingAmount = Some(0),
        interestEndDate = Some(LocalDate.of(2021, 6, 24))

      )
    ),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2020, 7, 13).toString))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionId2"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 8, 31).toString))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Balancing Charge"),
        transactionId = Some("testTransactionI3"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2022, 1, 31).toString))))
      )
    )
  )

  val financialDetailsDunningLockSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
    None,
    List(DocumentDetail(
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
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23).toString), amount = Some(12), dunningLock = Some("Stand over order"), transactionId = Some("testDunningTransactionId"))))
      ),
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        transactionId = Some("testDunningTransactionId2"),
        mainType = Some("SA Payment on Account 2"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23).toString), amount = Some(12), dunningLock = Some("Dunning Lock"), transactionId = Some("testDunningTransactionId2"))))
      )
    )
  )

  val emptyPaymentsList: List[DocumentDetailWithDueDate] = List.empty

  val currentObligationsSuccess: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "EOPS",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "EOPS"
        ))
    )
  ))

  val previousObligationsSuccess: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd.minusDays(1)),
          periodKey = "#004"
        ))
    )
  ))

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "EOPS",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "EOPS"
        ))
    ),
    NextUpdatesModel(
      identification = "ABC123456789",
      obligations = List(
        NextUpdateModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd.minusDays(1)),
          periodKey = "#004"
        ))
    )
  ))

  unauthorisedTest(s"/calculation/$testYear")

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  s"GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}" when {
    "Tax years overview page" should {
      "should show the updated Tax Year Overview page" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("The expected result is returned")
        val fromDate = LocalDate.of(2021, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2022, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val headingStr = fromDate + " to " + toDate + " " + TaxYearOverviewMessages.heading
        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextBySelector("#calculation-date")("15 February 2019"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("−£17,500.99"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.99"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Overdue Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£100.00"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("4 Apr 2022"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("5 Apr 2022")
        )
      }

      "should show Tax Year Overview page with payments with and without dunning locks in the payments tab" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("The expected result is returned")
        val fromDate = LocalDate.of(2021, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2022, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val headingStr = fromDate + " to " + toDate + " " + TaxYearOverviewMessages.heading
        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextBySelector("#calculation-date")("15 February 2019"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£12,500.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("−£17,500.99"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.99"),

          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Overdue Payment on account 1 of 2 Payment under review"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(4)")("£1,000.00"),

          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Overdue Payment on account 2 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£2,000.00"),

          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Late payment interest for payment on account 1 of 2 Payment under review"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(2)")("24 Jun 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(3)")("Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(3)", "td:nth-of-type(4)")("£100.00"),

          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("4 Apr 2022"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("5 Apr 2022")
        )

        AuditStub.verifyAuditEvent(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None, singleBusinessResponse,
            None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), financialDetailsDunningLockSuccess.getAllDocumentDetailsWithDueDates,
          allObligations, Some(TaxYearOverviewViewModel(liabilityCalculationModelSuccessFull))))
      }


      "should show user has Coding out that is requested and immediately rejected by NPS" in {
        enable(CodingOut)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, calculationTaxYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )


        And("The expected result is returned")
        val fromDate = LocalDate.of(2021, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2022, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Balancing payment"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Overdue Class 2 National Insurance")

        )
      }

      "should show user has Coding out that has been accepted and rejected by NPS part way through the year" in {
        enable(CodingOut)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, calculationTaxYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )


        And("The expected result is returned")
        val fromDate = LocalDate.of(2021, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2022, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Overdue Class 2 National Insurance"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Overdue Cancelled Self Assessment payment (through your PAYE tax code)")


        )
      }

      "should show at crystallization, the user has the coding out requested amount has not been fully collected (partially collected)" in {
        enable(CodingOut)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, calculationTaxYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

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
        val fromDate = LocalDate.of(2021, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2022, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Overdue Balancing payment"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Class 2 National Insurance"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(3)", "td:nth-of-type(1)")("Cancelled Self Assessment payment (through your PAYE tax code)")


        )
      }

      "financial details service returns a not found" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
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

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(
          nino = testNino,
          fromDate = getCurrentTaxYearEnd.minusYears(1).plusDays(1),
          toDate = getCurrentTaxYearEnd,
          deadlines = previousObligationsSuccess
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, getCurrentTaxYearEnd.getYear.toString)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString)

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelector("#payments p")("No payments currently due.")
        )

        AuditStub.verifyAuditEvent(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
            None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), emptyPaymentsList, allObligations, Some(TaxYearOverviewViewModel(liabilityCalculationModelSuccessFull))))
      }

      "financial details service returns an error" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A non crystallised calculation for 2017-18 is returned")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        And("A financial transaction call fails")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

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

        And("A calculation call for 2017-18 responds with http status 404:NOT_FOUND")
        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino,
          "2018")(NOT_FOUND, LiabilityCalculationError(NOT_FOUND, "error"))

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Seq(
            NextUpdatesModel(
              "ABC123456789",
              List(NextUpdateModel(
                LocalDate.of(2017, 12, 28),
                LocalDate.of(2018, 2, 3),
                LocalDate.of(2018, 2, 4),
                "Quarterly",
                Some(LocalDate.of(2018, 2, 2)),
                "#001"
              ))
            )
          ))
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino,
          ObligationsModel(Seq(
            NextUpdatesModel(
              "ABC123456789",
              List(NextUpdateModel(
                LocalDate.of(2017, 11, 28),
                LocalDate.of(2018, 1, 3),
                LocalDate.of(2018, 1, 4),
                "Quarterly",
                Some(LocalDate.of(2018, 1, 2)),
                "#001"
              ))
            )
          ))
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, "2018")

        And("The expected result with right headers are returned")
        val fromDate = LocalDate.of(2017, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2018, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val headingStr = fromDate + " to " + toDate + " " + TaxYearOverviewMessages.heading
        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxYearOverviewTitle),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextByID("no-calc-data-header")(TaxYearOverviewMessages.headingNoCalcData),
          elementTextByID("no-calc-data-note")(TaxYearOverviewMessages.noCalcDataNote)
        )
      }

      "retrieving a calculation failed with INTERNAL_SERVER_ERROR" in {

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A calculation call for 2017-18 fails")
        IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino,
          "2018")(INTERNAL_SERVER_ERROR, LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Seq(
            NextUpdatesModel(
              "ABC123456789",
              List(NextUpdateModel(
                LocalDate.of(2017, 12, 28),
                LocalDate.of(2018, 2, 3),
                LocalDate.of(2018, 2, 4),
                "Quarterly",
                Some(LocalDate.of(2018, 2, 2)),
                "#001"
              ))
            )
          ))
        )

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino,
          ObligationsModel(Seq(
            NextUpdatesModel(
              "ABC123456789",
              List(NextUpdateModel(
                LocalDate.of(2017, 11, 28),
                LocalDate.of(2018, 1, 3),
                LocalDate.of(2018, 1, 4),
                "Quarterly",
                Some(LocalDate.of(2018, 1, 2)),
                "#001"
              ))
            )
          ))
        )

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, "2018")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a previous obligations error" in {
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

        And("current obligations returns a success")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino,
          ObligationsModel(Nil))

        And("previous obligations call failed")
        IncomeTaxViewChangeStub.stubGetPreviousObligationsError(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5))

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, LocalDate.of(2017, 4, 6).toString,
          LocalDate.of(2018, 4, 5).toString)


        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a current obligations error" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Nil))

        And("current obligations call failed")
        IncomeTaxViewChangeStub.stubGetNextUpdatesError(testNino)

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
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

      And("previous obligations returns a success")
      IncomeTaxViewChangeStub.stubGetPreviousObligations(
        nino = testNino,
        fromDate = LocalDate.of(2017, 4, 6),
        toDate = LocalDate.of(2018, 4, 5),
        deadlines = previousObligationsSuccess
      )

      And("current obligations returns a success")
      IncomeTaxViewChangeStub.stubGetNextUpdates(
        nino = testNino,
        deadlines = currentObligationsSuccess
      )

      And("A calculation call for 2017-18 fails")
      IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, "2018")(INTERNAL_SERVER_ERROR, LiabilityCalculationError(INTERNAL_SERVER_ERROR, "Error"))

      When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
      val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

      Then("I check all calls expected were made")
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
      IncomeTaxViewChangeStub.verifyGetPreviousObligations(testNino,
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

      When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
      val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

      Then("I check all calls expected were made")
      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)

      And("Internal server error is returned")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(false, 2,
        () => IncomeTaxViewChangeFrontend.getCalculation(testYear))
    }
  }
}
