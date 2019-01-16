/*
 * Copyright 2019 HM Revenue & Customs
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
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import testUtils.TestSupport

class AccountDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestAccountDetailsController extends AccountDetailsController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate
  )

  lazy val messages = Messages.AccountDetails

  "The AccountDetailsController.getAccountDetails action" when {

    "Called with an Authenticated HMRC-MTD-IT User and AccountDetails feature is enabled" that {

      "successfully retrieves income sources from the incomeSourceDetailsPredicate" should {

        lazy val result = TestAccountDetailsController.getAccountDetails()(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          TestAccountDetailsController.config.features.accountDetailsEnabled(true)
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Account Details page" in {
          document.title() shouldBe messages.title
        }

      }
    }

    "the AccountDetails feature is disabled" should {

      lazy val result = TestAccountDetailsController.getAccountDetails()(fakeRequestWithActiveSession)

      "return Redirect (303)" in {
        TestAccountDetailsController.config.features.accountDetailsEnabled(false)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the ITVC home page" in {
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }

    }
  }

}
