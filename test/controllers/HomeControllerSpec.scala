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

import assets.Messages
import config.{FeatureSwitchConfig, FrontendAppConfig, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.config.MockFeatureSwitchConfig
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.MockServiceInfoPartialService
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers._

class HomeControllerSpec extends MockAuthenticationPredicate with MockServiceInfoPartialService with MockFeatureSwitchConfig {

  object TestHomeController extends HomeController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    mockServiceInfoPartialService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    mockFeatureSwitchConfig,
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi]
  )

  "navigating to the home page" when {

    "the home page feature is disabled" should {

      lazy val result = TestHomeController.home(fakeRequestWithActiveSession)

      "return Redirect (303)" in {
        setupMockHomePageEnabled(false)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the BTA home page" in {
        redirectLocation(result) shouldBe Some(TestHomeController.config.businessTaxAccount)
      }
    }

    "the home page feature is enabled" should {

      lazy val result = TestHomeController.home(fakeRequestWithActiveSession)

      "return OK (200)" in {
        setupMockHomePageEnabled(true)
        status(result) shouldBe Status.SEE_OTHER
      }

      "redirect to the Income Tax Home Page" in {
        Jsoup.parse(bodyOf(result)).title shouldBe Some(Messages.HomePage.title)
      }
    }

  }

}
