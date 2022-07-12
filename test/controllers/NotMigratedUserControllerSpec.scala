/*
 * Copyright 2022 HM Revenue & Customs
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

import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import play.api.mvc.MessagesControllerComponents
import services.PaymentHistoryService
import views.html.notMigrated.NotMigratedUser
import play.api.mvc.Result

import scala.concurrent.Future
import play.api.test.Helpers._
import play.api.http.Status
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess

class NotMigratedUserControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with MockItvcErrorHandler {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  trait Setup {
    val paymentHistoryService: PaymentHistoryService = mock[PaymentHistoryService]

    val controller = new NotMigratedUserController(
      app.injector.instanceOf[NotMigratedUser],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      mockAuthService,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      app.injector.instanceOf[ItvcErrorHandler],
      mockItvcErrorHandler,
      mockIncomeSourceDetailsService,
      app.injector.instanceOf[NavBarPredicate]
    )(ec,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig])

  }

  "User not migrated to ETMP" when {

    "hit how-to-claim-refund page" should {

      "show user content" in new Setup {
        mockSingleBusinessIncomeSource(userMigrated = false)
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some(HTML)
      }

      "show agent content" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource(userMigrated = false)
        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some(HTML)
      }
    }
  }

  "User migrated to ETMP" when {

    "hit how-to-claim-refund page" should {

      "throw an error for user" in new Setup {
        mockSingleBusinessIncomeSource(userMigrated = true)
        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "throw an error for agent" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource(userMigrated = true)
        val result: Future[Result] = controller.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

}
