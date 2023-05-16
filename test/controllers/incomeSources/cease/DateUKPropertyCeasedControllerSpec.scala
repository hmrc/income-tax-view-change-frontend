/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.cease

import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.cease.DateUKPropertyCeasedForm
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.DateUKPropertyCeased

import scala.concurrent.Future

class DateUKPropertyCeasedControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with FeatureSwitching {

  object TestDateUKPropertyCeasedController extends DateUKPropertyCeasedController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    app.injector.instanceOf[DateUKPropertyCeasedForm],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NinoPredicate],
    app.injector.instanceOf[DateUKPropertyCeased],
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.dateUKPropertyCeased.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.dateUKPropertyCeased.heading"))}"
    val heading: String = messages("incomeSources.dateUKPropertyCeased.heading")
    
  }

  "Individual - DateUKPropertyCeasedController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        enable(IncomeSources)
        mockPropertyIncomeSource()
        val result: Future[Result] = TestDateUKPropertyCeasedController.show()(fakeRequestWithActiveSession)
        val document: Document = Jsoup.parse(contentAsString(result))

        println(document)
        status(result) shouldBe Status.OK
        document.title shouldBe TestDateUKPropertyCeasedController.title
        document.select("h1").text shouldBe TestDateUKPropertyCeasedController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestDateUKPropertyCeasedController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestDateUKPropertyCeasedController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestDateUKPropertyCeasedController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
  
  
}
