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

import java.time.LocalDate

import assets.Messages.{Obligations => messages}
import assets.TestConstants._
import auth.MockAuthenticationPredicate
import config.FrontendAppConfig
import models._
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ImplicitDateFormatter.localDate
import utils.TestSupport
import views.Helpers

class ObligationsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockObligationsService {

  "The ObligationsController.getObligations function" when {

    "called with an Authenticated HMRC-MTD-IT user with NINO" which {

      val testDate = localDate("2018-05-05")

      object MockHelpers {
        def fakeObligationsModel(start: LocalDate, end: LocalDate, due: LocalDate, met: Boolean): ObligationModel = {
          ObligationModel(start,end,due,met)
//          Mockito.when()
        }
      }

      object TestObligationsController extends ObligationsController()(
        fakeApplication.injector.instanceOf[FrontendAppConfig],
        fakeApplication.injector.instanceOf[MessagesApi],
        MockAuthenticated,
        mockObligationsService
      )

      "successfully retrieves a list of Obligations from the Obligations service" should {

        lazy val result = TestObligationsController.getObligations()(FakeRequest())
        lazy val document = Jsoup.parse(bodyOf(result))

        def mockSuccess(): Unit = setupMockObligationsResult(testNino)(
          ObligationsModel(
            List(
              ObligationModel(
                start = localDate("2017-04-06"),
                end = localDate("2017-07-05"),
                due = localDate("2017-08-05"),
                met = true
              ),
              ObligationModel(
                start = localDate("2017-07-06"),
                end = localDate("2017-10-05"),
                due = localDate("2017-11-05"),
                met = true
              ),
              ObligationModel(
                start = localDate("2017-10-06"),
                end = localDate("2018-01-05"),
                due = localDate("2018-02-05"),
                met = false
              ),
              ObligationModel(
                start = localDate("2018-01-06"),
                end = localDate("2018-04-05"),
                due = localDate("2018-05-06"),
                met = false
              )
            )
          )
        )

        "return Status OK (200)" in {
          mockSuccess()
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
  }

}
