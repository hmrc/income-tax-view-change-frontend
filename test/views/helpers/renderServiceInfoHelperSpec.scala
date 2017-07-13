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

package views.helpers

import utils.TestSupport
import views.html.helpers.renderServiceInfoHelper
import assets.TestConstants.{testMtdItUser, testMtdItUserNoUserDetails, testUserName}
import auth.MtdItUser
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html

class renderServiceInfoHelperSpec extends TestSupport {

  def html(user: Option[MtdItUser]): Html = renderServiceInfoHelper(user)(
    applicationMessages,
    fakeApplication.injector.instanceOf[FrontendAppConfig]
  )

  "The renderServiceInfoHelper" when {

    "user details are passed to it" should {

      lazy val document = Jsoup.parse(html(Some(testMtdItUser)).body)

      "render the user name" in {
        document.getElementById("service-info-user-name").text() shouldBe testUserName
      }
    }

    "no user details are passed to it" should {

      lazy val document = Jsoup.parse(html(Some(testMtdItUserNoUserDetails)).body)

      "Not render the service-info-user-name" in {
        assertThrows[NullPointerException] {
          document.getElementById("service-info-user-name").text()
        }
      }
    }
  }
}
