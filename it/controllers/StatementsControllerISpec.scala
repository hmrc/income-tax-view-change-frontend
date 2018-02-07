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

import helpers.IntegrationTestConstants._
import helpers.servicemocks._
import helpers.{ComponentSpecBase, GenericStubMethods}
import play.api.http.Status
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.json.Json
import utils.ImplicitCurrencyFormatter._
import utils.ImplicitDateFormatter

class StatementsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with GenericStubMethods {

  "Calling the StatementsController" when {

    "isAuthorisedUser with an active enrolment" which {

      "has a single statement with a single charge" should {

        "display the tax year for the statement and the associated charge" in {

          isAuthorisedUser(true)

          And("I wiremock stub a successful Get Financial Transactions response")
          val statementResponse = Json.toJson(GetStatementsData.singleFinancialTransactionsModel)
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
          val res = IncomeTaxViewChangeFrontend.getStatements

          Then("I verify the Financial Transactions response has been wiremocked")
          FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

          Then("The view should have the correct headings and single statement")
          val model = GetStatementsData.singleChargeTransactionModel
          res should have(
            httpStatus(OK),
            pageTitle("Income Tax Statement"),
            elementTextByID(s"$testYear-tax-year")(s"Tax year: ${testYearInt - 1}-$testYear"),
            elementTextByID(s"$testYear-still-to-pay")(s"Still to pay: ${model.outstandingAmount.get.toCurrencyString}"),
            elementTextByID(s"$testYear-charge")(GetStatementsData.charge2018.amount.get.toCurrencyString),
            isElementVisibleById(s"$testYear-paid-0")(false)
          )

        }

      }

      "has 2 statements - one with a single charge & one with a charge and 2 payments" should {

        "display the tax year for the statements and the associated charge & payments" in {

          isAuthorisedUser(true)

          And("I wiremock stub a successful Get Financial Transactions response")
          val statementResponse = Json.toJson(GetStatementsData.singleFTModel1charge2payments)
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
          val res = IncomeTaxViewChangeFrontend.getStatements

          Then("I verify the Financial Transactions response has been wiremocked")
          FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

          Then("The view should have the correct headings and single statement")
          val statement1Model = GetStatementsData.singleChargeTransactionModel
          val statement2Model = GetStatementsData.singleCharge2PaymentsTransactionModel
          val payment = GetStatementsData.payment2019
          val payment2 = GetStatementsData.otherPayment2019
          res should have(
            httpStatus(OK),
            pageTitle("Income Tax Statement"),
            elementTextByID(s"$testYear-tax-year")(s"Tax year: ${testYearInt - 1}-$testYear"),
            elementTextByID(s"$testYear-still-to-pay")(s"Still to pay: ${statement1Model.outstandingAmount.get.toCurrencyString}"),
            elementTextByID(s"$testYear-charge")(GetStatementsData.charge2018.amount.get.toCurrencyString),
            isElementVisibleById(s"$testYear-paid-0")(false),
            elementTextByID(s"$testYearPlusOne-tax-year")(s"Tax year: ${testYearPlusOneInt - 1}-$testYearPlusOne"),
            elementTextByID(s"$testYearPlusOne-still-to-pay")(s"Still to pay: ${statement2Model.outstandingAmount.get.toCurrencyString}"),
            elementTextByID(s"$testYearPlusOne-charge")(GetStatementsData.charge2019.amount.get.toCurrencyString),
            elementTextByID(s"$testYearPlusOne-paid-0")(s"You paid " + payment.paymentAmount.get.toCurrencyString + " on " + payment.clearingDate.get.toShortDate),
            elementTextByID(s"$testYearPlusOne-paid-1")(s"You paid " + payment2.paymentAmount.get.toCurrencyString + " on " + payment2.clearingDate.get.toShortDate)
          )

        }

      }

      "has no financial transactions" should {

        "state that the user has no transactions since tey started reporting via software" in {

          isAuthorisedUser(true)

          And("I wiremock stub a successful Get Financial Transactions response")
          val statementResponse = Json.toJson(GetStatementsData.emptyFTModel)
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.OK, statementResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
          val res = IncomeTaxViewChangeFrontend.getStatements

          Then("I verify the Financial Transactions response has been wiremocked")
          FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

          Then("The view should have the correct headings and single statement")
          res should have(
            httpStatus(OK),
            pageTitle("Income Tax Statement"),
            elementTextByID("statements-no-transactions")(s"You've had no transactions since you started reporting through accounting software."),
            isElementVisibleById("2018-tax-year-section")(false),
            isElementVisibleById("2019-tax-year-section")(false)
          )

        }

      }

      "is returned a FinancialTransactionsErrorModel" should {

        "return an error page" in {

          isAuthorisedUser(true)

          And("I wiremock stub a successful Get Financial Transactions response")
          FinancialTransactionsStub.stubGetFinancialTransactions(testMtditid)(Status.INTERNAL_SERVER_ERROR, Json.obj())

          When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
          val res = IncomeTaxViewChangeFrontend.getStatements

          Then("I verify the Financial Transactions response has been wiremocked")
          FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)

          Then("The view should have the correct headings and single statement")
          res should have(
            httpStatus(OK),
            pageTitle("Income Tax Statement"),
            elementTextByID("page-heading")("We can't show your statement right now")
          )

        }
      }
    }

    "is Unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view/statements")
        val res = IncomeTaxViewChangeFrontend.getStatements

        Then("redirect to the Sign In Url")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }
}
