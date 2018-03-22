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

import assets.TestConstants._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.core.ErrorModel
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class BusinessDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestBusinessDetailsController extends BusinessDetailsController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[ItvcErrorHandler]
  )

  "The BusinessDetailsController.getBusinessDetails action" when {

    "Called with an Authenticated HMRC-MTD-IT User" that {

      "successfully retrieves a single BusinessModel from the incomeSourceDetailsService" should {

        lazy val result = TestBusinessDetailsController.getBusinessDetails(0)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          TestBusinessDetailsController.config.features.accountDetailsEnabled(true)
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Business Details page" in {
          document.title() shouldBe NewBizDeets.business1.tradingName.get
        }

      }

      "retrieves 'None' from the incomeSourceDetailsService" should {

        lazy val result = TestBusinessDetailsController.getBusinessDetails(0)(fakeRequestWithActiveSession)

        "return Status (500)" in {
          TestBusinessDetailsController.config.features.accountDetailsEnabled(true)
          mockNoIncomeSources()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

      "receives a ErrorModel from the incomeSourceDetailsService" should {

        lazy val result = TestBusinessDetailsController.getBusinessDetails(0)(fakeRequestWithActiveSession)

        "return Status (500)" in {
          mockErrorIncomeSource()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestBusinessDetailsController.getBusinessDetails(0)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }

    "the AccountDetails feature is disabled" should {

      lazy val result = TestBusinessDetailsController.getBusinessDetails(0)(fakeRequestWithActiveSession)

      "return Redirect (303)" in {
        TestBusinessDetailsController.config.features.accountDetailsEnabled(false)
        mockSingleBusinessIncomeSource()
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the ITVC home page" in {
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }

    }

  }

}
