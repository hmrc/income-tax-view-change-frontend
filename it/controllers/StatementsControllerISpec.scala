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

import assets.BaseIntegrationTestConstants._
import assets.StatementsIntegrationTestConstants._
import assets.messages.{StatementsMessages => messages}
import config.FrontendAppConfig
import helpers.servicemocks._
import helpers.{ComponentSpecBase, GenericStubMethods}
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import utils.ImplicitDateFormatter
import utils.ImplicitCurrencyFormatter

class StatementsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with ImplicitCurrencyFormatter with GenericStubMethods {


  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the StatementsController" when {

    "the statements page feature is disabled" should {

      "redirect to the home page" in {

        appConfig.features.statementsEnabled(false)

        And("I wiremock stub a successful Get Financial Transactions response")
        val statementResponse = Json.toJson(singleFinancialTransactionsModel)
        FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
        val res = IncomeTaxViewChangeFrontend.getStatements

        Then("redirect to the home page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }
    }

    "the statements page feature is disabled" should {

      "isAuthorisedUser with an active enrolment" which {

        "has a single statement with a single charge" should {

          "display the tax year for the statement and the associated charge" in {

            appConfig.features.statementsEnabled(true)

            And("I wiremock stub a successful Get Financial Transactions response")
            val statementResponse = Json.toJson(singleFinancialTransactionsModel)
            FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

            When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
            val res = IncomeTaxViewChangeFrontend.getStatements

            Then("I verify the Financial Transactions response has been wiremocked")
            FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

            Then("The view should have the correct headings and single statement")
            val model = singleChargeTransactionModel
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID(s"$testYear-tax-year")(messages.taxYear(testYearInt)),
              elementTextByID(s"$testYear-still-to-pay")(messages.stillToPay(model.outstandingAmount.get.toCurrencyString)),
              elementTextByID(s"$testYear-charge")(charge2018.amount.get.toCurrencyString),
              isElementVisibleById("earlier-statements")(true),
              isElementVisibleById(s"$testYear-paid-0")(false)
            )

          }

        }

        "has 2 statements - one with a single charge & one with a charge and 2 payments" should {

          "display the tax year for the statements and the associated charge & payments" in {

            appConfig.features.statementsEnabled(true)

            And("I wiremock stub a successful Get Financial Transactions response")
            val statementResponse = Json.toJson(singleFTModel1charge2payments)
            FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

            When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
            val res = IncomeTaxViewChangeFrontend.getStatements

            Then("I verify the Financial Transactions response has been wiremocked")
            FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

            Then("The view should have the correct headings and single statement")
            val statement1Model = singleChargeTransactionModel
            val statement2Model = singleCharge2PaymentsTransactionModel
            val payment = payment2019
            val payment2 = otherPayment2019
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID(s"$testYear-tax-year")(messages.taxYear(testYearInt)),
              elementTextByID(s"$testYear-still-to-pay")(messages.stillToPay(statement1Model.outstandingAmount.get.toCurrencyString)),
              elementTextByID(s"$testYear-charge")(charge2018.amount.get.toCurrencyString),
              isElementVisibleById(s"$testYear-paid-0")(false),
              elementTextByID(s"$testYearPlusOne-tax-year")(messages.taxYear(testYearPlusOneInt)),
              elementTextByID(s"$testYearPlusOne-still-to-pay")(messages.stillToPay(statement2Model.outstandingAmount.get.toCurrencyString)),
              elementTextByID(s"$testYearPlusOne-charge")(charge2019.amount.get.toCurrencyString),
              elementTextByID(s"$testYearPlusOne-paid-0")(messages.paid(payment.paymentAmount.get.toCurrencyString,payment.clearingDate.get.toShortDate)),
              elementTextByID(s"$testYearPlusOne-paid-1")(messages.paid(payment2.paymentAmount.get.toCurrencyString,payment2.clearingDate.get.toShortDate)),
              isElementVisibleById("earlier-statements")(true)
            )

          }

        }

        "has no financial transactions" should {

          "state that the user has no transactions since tey started reporting via software" in {

            appConfig.features.statementsEnabled(true)

            And("I wiremock stub a successful Get Financial Transactions response")
            val statementResponse = Json.toJson(emptyFTModel)
            FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

            When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
            val res = IncomeTaxViewChangeFrontend.getStatements

            Then("I verify the Financial Transactions response has been wiremocked")
            FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

            Then("The view should have the correct headings and single statement")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID("statements-no-transactions")(messages.noTransactions),
              isElementVisibleById("2018-tax-year-section")(false),
              isElementVisibleById("2019-tax-year-section")(false)
            )

          }

        }

        "is returned a FinancialTransactionsErrorModel" should {

          "return an error page" in {

            appConfig.features.statementsEnabled(true)

            And("I wiremock stub a successful Get Financial Transactions response")
            FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.INTERNAL_SERVER_ERROR, Json.obj())

            When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
            val res = IncomeTaxViewChangeFrontend.getStatements

            Then("I verify the Financial Transactions response has been wiremocked")
            FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

            Then("The view should have the correct headings and single statement")
            res should have(
              httpStatus(OK),
              pageTitle(messages.title),
              elementTextByID("page-heading")(messages.error)
            )

          }
        }
      }

      unauthorisedTest("/statements")
    }
  }
}
