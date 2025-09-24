/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.SEE_OTHER
import play.api.i18n.{Lang, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.play.language.LanguageUtils

class LocalLanguageControllerSpec extends PlaySpec with GuiceOneAppPerSuite   {

  implicit val messagesAPI: MessagesApi = app.injector.instanceOf[MessagesApi]
  private val languageUtils: LanguageUtils = app.injector.instanceOf[LanguageUtils]
  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  private val controller = new LocalLanguageController(
    languageUtils,
    mcc,
    messagesAPI
  )

  "LocalLanguageController.languageMap" should {
    "contain english and cymraeg mappings" in {
      val map = controller.languageMap
      map must contain ("english" -> Lang("en"))
      map must contain ("cymraeg" -> Lang("cy"))
      map.size mustBe 2
    }
  }

  "LocalLanguageController.fallbackURL" should {
    "return the home controller URL" in {
      controller.fallbackURL mustBe controllers.routes.HomeController.show().url
    }
  }

  "LocalLanguageController.switchToLanguage" should {
    "switch to english and set the language cookie" in {
      val result = controller.switchToLanguage("english")(FakeRequest())
      Helpers.cookies(result).get(messagesAPI.langCookieName).map(_.value) mustBe Some("en")
    }

    "switch to cymraeg and set the language cookie" in {
      val result = controller.switchToLanguage("cymraeg")(FakeRequest())
      Helpers.cookies(result).get(messagesAPI.langCookieName).map(_.value) mustBe Some("cy")
    }

    "redirect to fallbackURL if an invalid language is requested" in {
      val result = controller.switchToLanguage("french")(FakeRequest())
      status(result) mustBe SEE_OTHER
      redirectLocation(result) mustBe Some(controller.fallbackURL)
    }
  }
}
