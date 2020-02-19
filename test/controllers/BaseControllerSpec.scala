/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Inject
import mocks.controllers.predicates.MockAuthenticationPredicate
import play.api.http.HeaderNames
import play.api.i18n.MessagesApi

class BaseControllerSpec extends MockAuthenticationPredicate {

  class TestBaseController @Inject()(implicit val messagesApi: MessagesApi) extends BaseController

  object TestBaseController extends TestBaseController()(fakeApplication.injector.instanceOf[MessagesApi])

  "The BaseController.hc() method" when {

    "A referrer is provided on the request header" should {

      "include the Referrer as part of the HeaderCarrier" in {
        val hc = TestBaseController.hc(fakeRequestWithActiveSession)
        hc.headers.find(_._1 == HeaderNames.REFERER).map(_._2) shouldBe Some("/test/url")
      }
    }

    "No referrer is provided on the request header" should {

      "NOT include the Referrer as part of the HeaderCarrier" in {
        val hc = TestBaseController.hc(fakeRequestNoSession)
        hc.headers.find(_._1 == HeaderNames.REFERER).map(_._2) shouldBe None
      }
    }
  }

}
