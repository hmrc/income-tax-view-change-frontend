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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcBreakdownIntegrationTestConstants.calculationDataSuccessModel
import testConstants.CalcDataIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.messages.TaxYearOverviewMessages
import audit.models.{NextUpdatesResponseAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitching, TxmEventsApproved}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import models.financialDetails._
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest

class TaxYearOverviewControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
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

  val financialDetailsDunningLockSuccess: FinancialDetailsModel = FinancialDetailsModel(
    BalanceDetails(1.00, 2.00, 3.00),
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
    testMtditid, testNino, None,
    multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
  )(FakeRequest())

  s"GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}" when {

    "TxmEventsApproved FS is enabled" should {
      "should show the updated Tax Year Overview page" in {
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(2020, 4, 6, 12, 0))))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
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
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
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
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("−£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00"),
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
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(2020, 4, 6, 12, 0))))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
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
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
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
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("−£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00"),

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
          MtdItUser(testMtditid, testNino, None,
            singleBusinessResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), calculationDataSuccessModel, financialDetailsDunningLockSuccess.getAllDocumentDetailsWithDueDates, allObligations))
      }



        "should show user has Coding out that has been accepted and rejected by NPS part way through the year" in {
          enable(CodingOut)

          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

          And(s"A non crystallised calculation for $calculationTaxYear is returned")
          IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(2020, 4, 6, 12, 0))))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = estimatedCalculationFullJson
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
          IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino,
            from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
            to = getCurrentTaxYearEnd.toString
          )


          And("The expected result is returned")
          val fromDate = LocalDate.of(2021, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          val toDate = LocalDate.of(2022, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
          val headingStr = fromDate + " to " + toDate + " " + TaxYearOverviewMessages.heading
          res should have(
            httpStatus(OK),
            pageTitle(TaxYearOverviewMessages.title),
            elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Balancing payment"),
            elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Overdue Class 2 National Insurance")

          )
        }

      "financial details service returns a not found" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(2020, 4, 6, 12, 0))))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
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
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString)

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("#payments p")("No payments currently due.")
        )

        AuditStub.verifyAuditEvent(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), calculationDataSuccessModel, emptyPaymentsList, allObligations))
      }

      "financial details service returns an error" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A non crystallised calculation for 2017-18 is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = crystallisedCalculationFullJson
        )

        And("A financial transaction call fails")
        IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

        When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino)


        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a calculation failed" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A calculation call for 2017-18 responds with http status 404:NOT_FOUND")
        IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "2017-18")

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
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")

        And("The expected result with right headers are returned")
        val fromDate = LocalDate.of(2017, 4, 6).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val toDate = LocalDate.of(2018, 4, 5).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
        val headingStr = fromDate + " to " + toDate + " " + TaxYearOverviewMessages.heading
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextByID("no-calc-data-header")(TaxYearOverviewMessages.headingNoCalcData),
          elementTextByID("no-calc-data-note")(TaxYearOverviewMessages.noCalcDataNote)
        )
      }

      "retrieving a calculation failed with INTERNAL_SERVER_ERROR" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A calculation call for 2017-18 fails")
        IndividualCalculationStub.stubGetCalculationListInternalServerError(testNino, "2017-18")

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
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a previous obligations error" in {
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A non crystallised calculation for 2017-18 is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
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
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }

      "retrieving a current obligations error" in {
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And("A non crystallised calculation for 2017-18 is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
        )

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
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }

    "TxmEventsApproved FS is disabled" should {
      "should show the Tax Year Overview page" in {
        disable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(2020, 4, 6, 12, 0))))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
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
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
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
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1.govuk-heading-xl")(headingStr),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=govuk-table__cell govuk-table__cell--numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=govuk-table__cell govuk-table__cell--numeric]")("−£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00"),

          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Overdue Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("23 Apr 2021"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("Part paid"),
          elementTextBySelectorList("#payments", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(4)")("£1,000.00"),

          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("4 Apr 2022"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("5 Apr 2022")
        )

        AuditStub.verifyAuditDoesNotContainsDetail(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None,
            singleBusinessResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, allObligations).detail)
      }

      "financial details service returns a not found" in {
        disable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponseWoMigration)

        And(s"A non crystallised calculation for $calculationTaxYear is returned")
        IndividualCalculationStub.stubGetCalculationList(testNino, calculationTaxYear)(
          status = OK,
          body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(2020, 4, 6, 12, 0))))
        )
        IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
          status = OK,
          body = estimatedCalculationFullJson
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
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString)

        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("#payments p")("No payments currently due.")
        )

        AuditStub.verifyAuditDoesNotContainsDetail(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), calculationDataSuccessModel, emptyPaymentsList, allObligations).detail)
      }
    }

    "retrieving a calculation failed" in {
      Given("Business details returns a successful response back")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

      And("A calculation call for 2017-18 fails")
      IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "2017-18")

      When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
      val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

      Then("I check all calls expected were made")
      verifyIncomeSourceDetailsCall(testMtditid)
      IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")

      And("Internal server error is returned")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "retrieving a financial transaction failed" in {
      Given("Business details returns a successful response back")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponseWoMigration)

      And("A non crystallised calculation for 2017-18 is returned")
      IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
        status = OK,
        body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
      )
      IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
        status = OK,
        body = crystallisedCalculationFullJson
      )

      And("A financial transaction call fails")
      IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

      When(s"I call GET ${controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url}")
      val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

      Then("I check all calls expected were made")
      verifyIncomeSourceDetailsCall(testMtditid)
      IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
      IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
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
