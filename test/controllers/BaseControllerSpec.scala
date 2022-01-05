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

import mocks.controllers.predicates.MockAuthenticationPredicate
import play.api.http.HeaderNames
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport

import scala.concurrent.ExecutionContext

class BaseControllerSpec extends TestSupport with MockAuthenticationPredicate {

  object TestBaseController extends BaseController()(
    app.injector.instanceOf[MessagesControllerComponents]) {
    override implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  }

  "The BaseController.hc() method" when {

    "A referrer is provided on the request header" should {

      "include the Referrer as part of the HeaderCarrier" in {
        val hc = TestBaseController.hc(fakeRequestWithActiveSession)
        hc.headers(Seq(HeaderNames.REFERER)).map(_._2) shouldBe List("/test/url")
      }
    }

    "No referrer is provided on the request header" should {

      "NOT include the Referrer as part of the HeaderCarrier" in {
        val hc = TestBaseController.hc(fakeRequestNoSession)
        hc.headers(Seq(HeaderNames.REFERER)).map(_._2) shouldBe List()
      }
    }
  }

}
