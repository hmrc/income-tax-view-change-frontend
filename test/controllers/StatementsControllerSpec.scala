/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.Messages
import config.FrontendAppConfig
import controllers.predicates.SessionTimeoutPredicate
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockFinancialTransactionsService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class StatementsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockFinancialTransactionsService {

  object TestStatementsController extends StatementsController()(
    fakeApplication.injector.instanceOf[FrontendAppConfig],
    fakeApplication.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockFinancialTransactionsService
  )

  "StatementsController.getStatements" when {

    "called with an Authenticated HMRC-MTD-IT user" which {

      "successfully retrieves a Financial Transactions" should {

        lazy val result = TestStatementsController.getStatements(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(contentAsString(result))

        "return OK (200)" in {
          mockFinancialTransactionSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        s"have the title '${Messages.Statements.title}'" in {
          document.title shouldBe Messages.Statements.title
        }
      }

      "returns an error response forFinancial Transactions" should {

        lazy val result = TestStatementsController.getStatements(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(contentAsString(result))

        "return OK (200)" in {
          mockFinancialTransactionFailed()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        s"have the title '${Messages.Statements.title}'" in {
          document.title shouldBe Messages.Statements.title
        }

        s"have a graceful error message '${Messages.Statements.Error.pageHeading}'" in {
          document.getElementById("page-heading").text shouldBe Messages.Statements.Error.pageHeading
        }
      }
    }

    "called with an Unauthenticated user" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestStatementsController.getStatements(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}
