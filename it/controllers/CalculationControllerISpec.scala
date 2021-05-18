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
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, TaxYearOverviewUpdate}
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel}
import play.api.http.Status._

class CalculationControllerISpec extends ComponentSpecBase with FeatureSwitching {

  unauthorisedTest(s"/calculation/$testYear")

  s"GET ${controllers.routes.CalculationController.renderTaxYearOverviewPage(testYearInt).url}" when {

    "TaxYearOverviewUpdate FS is enabled" should {
      "should show the updated Tax Year Overview page" in {
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
          ObligationsModel(Seq(
            ReportDeadlinesModel(
              "ABC123456789",
              List(ReportDeadlineModel(
                LocalDate.of(2017, 12, 28),
                LocalDate.of(2018, 2, 3),
                LocalDate.of(2018, 2,4),
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
                LocalDate.of(2018, 1,4),
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
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading),
          elementTextBySelector("#calculation-date")("6 July 2017"),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505.00"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500.00"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td:nth-child(2)")("£90,500.00"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(1)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("2 Jan 2018"),
          elementTextBySelectorList("#updates", "div:nth-of-type(2)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(1)")("Quarterly Update"),
          elementTextBySelectorList("#updates", "div:nth-of-type(2)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(2)")("business"),
          elementTextBySelectorList("#updates", "div:nth-of-type(2)", "tbody", "tr:nth-of-type(1)", "td:nth-of-type(3)")("2 Feb 2018")
        )
      }

      s"financial details service returns a $NOT_FOUND" in {
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
        IncomeTaxViewChangeStub.stubGetFinancialDetailsResponse(testNino)(NOT_FOUND, testFinancialDetailsErrorModelJson())

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
                LocalDate.of(2018, 2,4),
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
                LocalDate.of(2018, 1,4),
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
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        IncomeTaxViewChangeStub.verifyGetFinancialDetails(testNino)


        And("Page is displayed with no payments due")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("#payments p")("No payments currently due.")
        )
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
      }

      "retrieving a calculation failed" in {
        enable(TaxYearOverviewUpdate)
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
                LocalDate.of(2018, 2,4),
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
                LocalDate.of(2018, 1,4),
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
      }

      "retrieving a calculation failed with INTERNAL_SERVER_ERROR" in {
        enable(TaxYearOverviewUpdate)
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
                LocalDate.of(2018, 2,4),
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
                LocalDate.of(2018, 1,4),
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
