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
import assets.TestConstants._
import assets.TestConstants.BusinessDetails._
import config.FrontendAppConfig
import controllers.predicates.AuthenticationPredicate
import mocks.controllers.predicates.{MockAsyncActionPredicate, MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockObligationsService
import models._
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._
import utils.{ImplicitDateFormatter, TestSupport}

class ObligationsControllerSpec extends TestSupport with MockAsyncActionPredicate with MockObligationsService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter {

  class setupTestController(authentication: AuthenticationPredicate)
    extends ObligationsController()(
      fakeApplication.injector.instanceOf[FrontendAppConfig],
      fakeApplication.injector.instanceOf[MessagesApi],
      new asyncActionBuilder(authentication, BusinessIncome),
      mockObligationsService
    )

  "The ObligationsController.getObligations function" when {

    "called with an Authenticated HMRC-MTD-IT user with NINO" which {

      object TestObligationsController extends setupTestController(MockAuthenticated)

      "successfully retrieves a list of Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        def mockSuccess(): Unit = setupMockBusinessObligationsResult(testNino, Some(businessIncomeModel))(
          ObligationsModel(
            List(
              ObligationModel(
                start = "2017-04-06",
                end = "2017-07-05",
                due = "2017-08-05",
                met = true
              ),
              ObligationModel(
                start = "2017-07-06",
                end = "2017-10-05",
                due = "2017-11-05",
                met = true
              ),
              ObligationModel(
                start = "2017-10-06",
                end = "2018-01-05",
                due = "2018-02-05",
                met = false
              ),
              ObligationModel(
                start = "2018-01-06",
                end = "2018-04-05",
                due = "2018-05-06",
                met = false
              )
            )
          )
        )

        def mockPropertySuccess(): Unit = setupMockPropertyObligationsResult(testNino)(
          ObligationsModel(
            List(
              ObligationModel(
                start = "2017-04-06",
                end = "2017-07-05",
                due = "2017-08-05",
                met = true
              ),
              ObligationModel(
                start = "2017-07-06",
                end = "2017-10-05",
                due = "2017-11-05",
                met = true
              ),
              ObligationModel(
                start = "2017-10-06",
                end = "2018-01-05",
                due = "2018-02-05",
                met = false
              ),
              ObligationModel(
                start = "2018-01-06",
                end = "2018-04-05",
                due = "2018-05-06",
                met = false
              )
            )
          )
        )

        "return Status OK (200)" in {
          mockSuccess()
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

      "doesn't retrieve a list of Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(fakeRequestWithActiveSession)
        lazy val document = Jsoup.parse(bodyOf(result))

        def mockBusinessObligationsFail(): Unit = setupMockBusinessObligationsResult(testNino, Some(businessIncomeModel))(
          ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Error Message")
        )
        def mockPropertyObligationsFail(): Unit = setupMockPropertyObligationsResult(testNino)(
          ObligationsErrorModel(Status.INTERNAL_SERVER_ERROR, "Error Message")
        )

        "return Status INTERNAL_SERVER_ERROR (500)" in {
          mockBusinessObligationsFail()
          mockPropertyObligationsFail()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the ISE page" in {
          document.title shouldBe errorMessages.title
        }
      }

    }

    "Called with an Unauthenticated User" should {

      object TestObligationsController extends setupTestController(MockUnauthorised)

      "return redirect SEE_OTHER (303)" in {
        val result = TestObligationsController.getObligations()(fakeRequestNoSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

}
