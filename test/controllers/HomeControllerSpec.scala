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

import auth.MockAuthenticationPredicate
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, _}
import utils.TestSupport
import assets.Messages.{Home => messages}
import models.{EstimatedTaxLiability, EstimatedTaxLiabilityError}
import play.api.i18n.MessagesApi
import assets.TestConstants._

class HomeControllerSpec extends TestSupport with MockAuthenticationPredicate with MockEstimatedLiabilityService {

  "The HomeController.home action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      object TestHomeController extends HomeController()(
        fakeApplication.injector.instanceOf[FrontendAppConfig],
        MockAuthenticated,
        mockEstimatedLiabilityService,
        fakeApplication.injector.instanceOf[MessagesApi]
      )

      "successfully retrieves an Estimated Tax Liability amount from the EstimatedTaxLiability Service" should {

        lazy val result = TestHomeController.home()(FakeRequest())
        lazy val document = Jsoup.parse(bodyOf(result))
        def mockSuccess(): Unit = setupMockEstimatedTaxLiabilityResult(testMtditid)(EstimatedTaxLiability(
          total = 1000.0,
          nic2 = 500.0,
          nic4 = 300.0,
          incomeTax = 200.0
        ))

        "return Status OK (200)" in {
          mockSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          mockSuccess()
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Home page" in {
          mockSuccess()
          document.title() shouldBe messages.title
        }
      }

      "receives an Error from the EstimatedTaxLiability Service" should {

        lazy val result = TestHomeController.home()(FakeRequest())
        def mockError(): Unit = setupMockEstimatedTaxLiabilityResult(testMtditid)(EstimatedTaxLiabilityError(
          status = Status.INTERNAL_SERVER_ERROR,
          message = "Internal Server Error Message"
        ))

        "return Internal Server Error (500)" in {
          mockError()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return HTML" in {
          mockError()
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }
      }


    }

    "Called with an Unauthenticated User" should {

      object TestHomeController extends HomeController()(
        fakeApplication.injector.instanceOf[FrontendAppConfig],
        MockUnauthorised,
        mockEstimatedLiabilityService,
        fakeApplication.injector.instanceOf[MessagesApi]
      )
      "return redirect SEE_OTHER (303)" in {
        val result = TestHomeController.home()(FakeRequest())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}
