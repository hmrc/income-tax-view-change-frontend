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
import assets.CalcDataIntegrationTestConstants._
import assets.FinancialTransactionsIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.messages.TaxYearOverviewMessages
import audit.models.{TaxYearOverviewRequestAuditModel, TaxYearOverviewResponseAuditModel}
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, TaxYearOverviewUpdate}
import helpers.ComponentSpecBase
import helpers.servicemocks._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.calculation.{CalculationItem, ListCalculationItems}
import models.financialDetails.{Charge, FinancialDetailsModel, SubItem}
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.test.FakeRequest

class CalculationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  import implicitDateFormatter.longDate

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }

  val calculationTaxYear: String = s"${getCurrentTaxYearEnd.getYear - 1}-${getCurrentTaxYearEnd.getYear.toString.drop(2)}"

  val financialDetailsSuccess: FinancialDetailsModel = FinancialDetailsModel(List(
    Charge(
      taxYear = getCurrentTaxYearEnd.getYear.toString,
      transactionId = "testTransactionId",
      transactionDate = Some(getCurrentTaxYearEnd.toString),
      `type` = None,
      totalAmount = Some(1000.00),
      originalAmount = Some(1000.00),
      outstandingAmount = Some(500.00),
      clearedAmount = Some(500.00),
      chargeType = Some("POA1"),
      mainType = Some("SA Payment on Account 1"),
      items = Some(Seq(SubItem(None, None, None, None, None, None, None, Some(LocalDate.of(2021, 4, 23).toString), None, None)))
    )
  ))

  val emptyPaymentsList: List[Charge] = List.empty

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

  s"GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}" when {

    "TaxYearOverviewUpdate FS is enabled" should {
      "show the updated Tax Year Overview page" in {
        enable(TaxYearOverviewUpdate)

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

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
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
          )(FakeRequest()), None, calculationDataSuccessModel, financialDetailsSuccess.financialDetails, allObligations).detail)
      }

      "financial details service returns a not found" in {
        enable(TaxYearOverviewUpdate)
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
        enable(TaxYearOverviewUpdate)
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

        AuditStub.verifyAuditContains(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a calculation failed" in {
        enable(TaxYearOverviewUpdate)
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

        AuditStub.verifyAuditContains(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a previous obligations error" in {
        enable(TaxYearOverviewUpdate)

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

        AuditStub.verifyAuditContains(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }

      "retrieving a current obligations error" in {
        enable(TaxYearOverviewUpdate)

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

        And("Internal server error is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        AuditStub.verifyAuditContains(TaxYearOverviewRequestAuditModel(
          MtdItUser(testMtditid, testNino, None,
            multipleBusinessesAndPropertyResponse, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
          )(FakeRequest()), None).detail)
      }
    }

    "TaxYearOverviewUpdate is disabled" should {
      "show the old Tax Year Overview page" in {
        disable(TaxYearOverviewUpdate)
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

        When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.titleOld(testYearInt - 1, testYearInt)),
          elementTextBySelector("h1")(TaxYearOverviewMessages.headingOld(testYearInt - 1, testYearInt)),
          elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDateOld("6 July 2017")),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00")
        )
      }
    }

    "NewFinancialDetailsApi is disabled" when {
      "the user is authorised with an active enrolment" when {
        "a non-crystallised calculation is returned" in {
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

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          Then("I check all calls expected were made")
          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

          And("The expected result is returned")
          res should have(
            httpStatus(OK),
            pageTitle(TaxYearOverviewMessages.titleOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("h1")(TaxYearOverviewMessages.headingOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDateOld("6 July 2017")),
            elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
            elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
            elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00")
          )
        }

        "a crystallised calculation and financial transaction is returned " in {
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

          And("A financial transaction is returned")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(OK, financialTransactionsJson(1000.00))

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          Then("I check all calls expected were made")
          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

          And("The expected result is returned")
          res should have(
            httpStatus(OK),
            pageTitle(TaxYearOverviewMessages.titleOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("h1")(TaxYearOverviewMessages.headingOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDateOld("6 July 2017")),
            elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
            elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
            elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00")
          )
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
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(INTERNAL_SERVER_ERROR, financialTransactionsSingleErrorJson())

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          Then("I check all calls expected were made")
          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)


          And("Internal server error is returned")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }

    "NewFinancialDetailsApi is enabled" when {
      "the user is authorised with an active enrolment" when {
        "a non-crystallised calculation is returned" in {
          enable(NewFinancialDetailsApi)
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

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          Then("I check all calls expected were made")
          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

          And("The expected result is returned")
          res should have(
            httpStatus(OK),
            pageTitle(TaxYearOverviewMessages.titleOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("h1")(TaxYearOverviewMessages.headingOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDateOld("6 July 2017")),
            elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
            elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
            elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00")
          )
        }

        "a crystallised calculation and financial transaction is returned " in {
          enable(NewFinancialDetailsApi)
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

          And("A financial transaction is returned")
          IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(OK, testValidFinancialDetailsModelJson(1000.00, 1000.00))

          When(s"I call GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

          Then("I check all calls expected were made")
          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino)

          And("The expected result is returned")
          res should have(
            httpStatus(OK),
            pageTitle(TaxYearOverviewMessages.titleOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("h1")(TaxYearOverviewMessages.headingOld(testYearInt - 1, testYearInt)),
            elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDateOld("6 July 2017")),
            elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
            elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
            elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00")
          )
        }

        "retrieving a calculation failed" in {
          enable(NewFinancialDetailsApi)
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
          enable(NewFinancialDetailsApi)
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
  }

}
