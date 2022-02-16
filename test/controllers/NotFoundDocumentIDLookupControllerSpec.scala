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

import controllers.errors.NotFoundDocumentIDLookupController
import org.jsoup.Jsoup
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testConstants.MessagesLookUp.{CustomNotFound => customNotFound}
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError

class NotFoundDocumentIDLookupControllerSpec extends TestSupport {

  object NotFoundDocumentIDLookupController extends NotFoundDocumentIDLookupController(
    app.injector.instanceOf[CustomNotFoundError])(
    app.injector.instanceOf[MessagesControllerComponents]
  )

  "the NotFoundDocumentIDLookupController.show() action" should {

    lazy val result = NotFoundDocumentIDLookupController.show(fakeRequestNoSession)
    lazy val document = Jsoup.parse(contentAsString(result))

    "return OK (200)" in {
      status(result) shouldBe Status.OK
    }

    "show the CustomNotFound page" in {
      document.getElementsByTag("h1").text() shouldBe customNotFound.heading
    }

  }

}
