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

import java.time.{LocalDate, LocalDateTime}

import assets.BaseIntegrationTestConstants._
import assets.CalcBreakdownIntegrationTestConstants.calculationDataSuccessModel
import assets.CalcDataIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.messages.TaxYearOverviewMessages
import audit.models.{ReportDeadlinesRequestAuditModel, ReportDeadlinesResponseAuditModel, TaxYearOverviewRequestAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, TxmEventsApproved}
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import models.financialDetails._
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.FakeRequest

class CalculationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(List(
    DocumentDetail(
      taxYear = getCurrentTaxYearEnd.getYear.toString,
      transactionId = "testTransactionId",
      documentDescription = Some("ITSA- POA 1"),
      documentDate = LocalDate.of(2018, 3, 29),
      originalAmount = Some(1000.00),
      outstandingAmount = Some(500.00)
    )),
    List(
      FinancialDetail(
        taxYear = getCurrentTaxYearEnd.getYear.toString,
        mainType = Some("SA Payment on Account 1"),
        items = Some(Seq(SubItem(Some(LocalDate.of(2021, 4, 23).toString))))
      )
    )
  )

  val emptyPaymentsList: List[DocumentDetailWithDueDate] = List.empty

  val currentObligationsSuccess: ObligationsModel = ObligationsModel(Seq(
    ReportDeadlinesModel(
      identification = "ABC123456789",
      obligations = List(
        ReportDeadlineModel(
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
    ReportDeadlinesModel(
      identification = "ABC123456789",
      obligations = List(
        ReportDeadlineModel(
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
    ReportDeadlinesModel(
      identification = "ABC123456789",
      obligations = List(
        ReportDeadlineModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "EOPS",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "EOPS"
        ))
    ),
    ReportDeadlinesModel(
      identification = "ABC123456789",
      obligations = List(
        ReportDeadlineModel(
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

  s"GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}" when {

    "TaxYearOverviewUpdate FS is enabled" should {
      "should show the updated Tax Year Overview page" in {
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

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
        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Overdue Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("23 April 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£1,000.00"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("4 Apr 2022"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Annual Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(2)", "td:nth-of-type(3)")("5 Apr 2022")
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            singleBusinessResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None,
            singleBusinessResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None, calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, allObligations).detail)
      }

      "financial details service returns a not found" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

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
        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString)

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("#payments p")("No payments currently due.")
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None, calculationDataSuccessModel, emptyPaymentsList, allObligations).detail)
      }

      "financial details service returns an error" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

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
        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino)


        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a calculation failed" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("A calculation call for 2017-18 responds with http status 404:NOT_FOUND")
        IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "2017-18")

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Seq(
            ReportDeadlinesModel(
              "ABC123456789",
              List(ReportDeadlineModel(
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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino,
          ObligationsModel(Seq(
            ReportDeadlinesModel(
              "ABC123456789",
              List(ReportDeadlineModel(
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

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")

        And("The expected result with right headers are returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading),
          elementTextByID("no-calc-data-header")(TaxYearOverviewMessages.headingNoCalcData),
          elementTextByID("no-calc-data-note")(TaxYearOverviewMessages.noCalcDataNote)
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a calculation failed with INTERNAL_SERVER_ERROR" in {
        enable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("A calculation call for 2017-18 fails")
        IndividualCalculationStub.stubGetCalculationListInternalServerError(testNino, "2017-18")

        And("previous obligations returns a success")
        IncomeTaxViewChangeStub.stubGetPreviousObligations(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5),
          ObligationsModel(Seq(
            ReportDeadlinesModel(
              "ABC123456789",
              List(ReportDeadlineModel(
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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino,
          ObligationsModel(Seq(
            ReportDeadlinesModel(
              "ABC123456789",
              List(ReportDeadlineModel(
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

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a previous obligations error" in {
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testNino,
          ObligationsModel(Nil))

        And("previous obligations call failed")
        IncomeTaxViewChangeStub.stubGetPreviousObligationsError(testNino,
          LocalDate.of(2017, 4, 6),
          LocalDate.of(2018, 4, 5))

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a current obligations error" in {
        enable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

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
        IncomeTaxViewChangeStub.stubGetReportDeadlinesError(testNino)

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        AuditStub.verifyAuditContainsDetail(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }
    }

    "TaxYearOverviewUpdate FS is enabled and with TxmEventsApproved FS disabled" should {
      "should show the updated Tax Year Overview page" in {
        disable(TxmEventsApproved)

        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

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
        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(getCurrentTaxYearEnd.getYear).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString
        )

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric no-wrap]")("−£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(1)")("Overdue Payment on account 1 of 2"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(2)")("23 April 2021"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(3)")("Part Paid"),
          elementTextBySelectorList("#payments", "table", "tr:nth-of-type(2)", "td:nth-of-type(4)")("£1,000.00"),
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
          )(FakeRequest()), None, calculationDataSuccessModel, financialDetailsSuccess.getAllDocumentDetailsWithDueDates, allObligations).detail)
      }

      "financial details service returns a not found" in {
        disable(TxmEventsApproved)
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

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
        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(
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
        IncomeTaxViewChangeStub.stubGetReportDeadlines(
          nino = testNino,
          deadlines = currentObligationsSuccess
        )

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(getCurrentTaxYearEnd.getYear.toString)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, calculationTaxYear)
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(
          nino = testNino,
          from = getCurrentTaxYearEnd.minusYears(1).plusDays(1).toString,
          to = getCurrentTaxYearEnd.toString)

        verifyAuditContainsDetail(ReportDeadlinesRequestAuditModel(testUser).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", previousObligationsSuccess.obligations.flatMap(_.obligations)).detail)
        verifyAuditContainsDetail(ReportDeadlinesResponseAuditModel(testUser, "ABC123456789", currentObligationsSuccess.obligations.flatMap(_.obligations)).detail)

        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("#payments p")("No payments currently due.")
        )

        AuditStub.verifyAuditDoesNotContainsDetail(TaxYearOverviewResponseAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None, calculationDataSuccessModel, emptyPaymentsList, allObligations).detail)
      }
    }

        "retrieving a calculation failed" in {
          Given("Business details returns a successful response back")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

          And("A calculation call for 2017-18 fails")
          IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "2017-18")

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
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
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

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
          IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(INTERNAL_SERVER_ERROR, testFinancialDetailsErrorModelJson())

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          Then("I check all calls expected were made")
          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino)


          And("Internal server error is returned")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
}
