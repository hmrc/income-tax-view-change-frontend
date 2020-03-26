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

import java.time.LocalDateTime

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.FinancialTransactionsIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.messages.TaxYearOverviewMessages
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import play.api.http.Status._

class CalculationControllerISpec extends ComponentSpecBase {

  unauthorisedTest(s"/calculation/$testYear")

  s"GET ${controllers.routes.CalculationController.renderCalculationPage(testYearInt).url}" when {
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

        When(s"I call GET ${controllers.routes.CalculationController.renderCalculationPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading(testYearInt - 1, testYearInt)),
          elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDate("6 July 2017")),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500"),
          elementTextBySelector("#income-deductions-table tr:nth-child(3) td[class=numeric]")("£198,500"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td[class=numeric]")("£90,500"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(2) td[class=numeric]")("£0"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(3) td[class=numeric]")("£90,500")
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

        When(s"I call GET ${controllers.routes.CalculationController.renderCalculationPage(testYearInt).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculation(testYear)

        Then("I check all calls expected were made")
        verifyIncomeSourceDetailsCall(testMtditid)
        IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
        IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
        FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

        And("The expected result is returned")
        res should have(
          httpStatus(OK),
          pageTitle(TaxYearOverviewMessages.title),
          elementTextBySelector("h1")(TaxYearOverviewMessages.heading(testYearInt - 1, testYearInt)),
          elementTextBySelector("#calculation-date")(TaxYearOverviewMessages.calculationDate("6 July 2017")),
          elementTextBySelector("#income-deductions-table tr:nth-child(1) td[class=numeric]")("£199,505"),
          elementTextBySelector("#income-deductions-table tr:nth-child(2) td[class=numeric]")("-£500"),
          elementTextBySelector("#income-deductions-table tr:nth-child(3) td[class=numeric]")("£198,500"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(1) td[class=numeric]")("£90,500"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(2) td[class=numeric]")("-£2,000"),
          elementTextBySelector("#taxdue-payments-table tr:nth-child(3) td[class=numeric]")("£1,000")
        )
      }

      "retrieving a calculation failed" in {
        Given("Business details returns a successful response back")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("A calculation call for 2017-18 fails")
        IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "2017-18")

        When(s"I call GET ${controllers.routes.CalculationController.renderCalculationPage(testYearInt).url}")
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

        When(s"I call GET ${controllers.routes.CalculationController.renderCalculationPage(testYearInt).url}")
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

}
