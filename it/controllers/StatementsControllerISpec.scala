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

import helpers.{ComponentSpecBase, GenericStubMethods}
import utils.ImplicitDateFormatter
import helpers.IntegrationTestConstants._
import helpers.servicemocks._

class StatementsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with GenericStubMethods {

  "Calling the ReportDeadlinesController" when {

    "isAuthorisedUser with an active enrolment" which {

      "has a single statement with a single charge" should {

        "display the tax year for the statement and the associated charge" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Financial Transactions response")
          val statementResponse = GetStatementsData.singleFinancialTransactionsModel
          FinancialTransactionsStub.stubGetFinancialTransactions(testNino, statementResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/view-statement")
          val res = IncomeTaxViewChangeFrontend.getStatements

          FinancialTransactionsStub.verifyGetFinancialTransactions()

        }

      }

    }
  }

}
