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

import assets.Messages.{ISE => errorMessages, Obligations => messages}
import config.FrontendAppConfig
import mocks.controllers.predicates.MockAsyncActionPredicate
import mocks.services.MockObligationsService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.TestSupport

class ObligationsControllerSpec extends TestSupport with MockAsyncActionPredicate with MockObligationsService {

  object TestObligationsController extends ObligationsController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    MockAsyncActionPredicate,
    mockObligationsService
  )

  "The ObligationsController.getObligations function" when {

    "called with an Authenticated HMRC-MTD-IT user with NINO" which {

      "successfully retrieves a set of Business Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockSingleBusinessIncomeSource()
          mockBusinessSuccess()
          mockNoPropertyIncome()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Obligations page" in {
          document.title shouldBe messages.title
        }
      }

      "successfully retrieves a set of Property Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockPropertyIncomeSource()
          mockNoBusinessIncome()
          mockPropertySuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Obligations page" in {
          document.title shouldBe messages.title
        }
      }

      "successfully retrieves a set of both Business & Property Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockBothIncomeSources()
          mockBusinessSuccess()
          mockPropertySuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Obligations page" in {
          document.title shouldBe messages.title
        }
      }

      "doesn't retrieve any Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        "return Status OK (200)" in {
          mockNoIncomeSources()
          mockNoBusinessIncome()
          mockNoPropertyIncome()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the Obligations page" in {
          document.title shouldBe messages.title
        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

}
