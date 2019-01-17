/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Play
import play.api.http.Status
import play.api.i18n.Lang
import play.api.mvc.Cookie
import play.api.test.Helpers._
import testUtils.TestSupport

class ItvcLanguageControllerSpec extends TestSupport {

  object TestItvcLanguageController extends ItvcLanguageController(
    frontendAppConfig,
    messagesApi
  )

  "Calling LanguageController.langToCall" should {

    "when passed in english as a choice " in {
      val result = TestItvcLanguageController.langToCall.apply("en").toString
      result shouldBe "/report-quarterly/income-and-expenses/view/language/en"
    }


    "when passed in welsh as a choice" in {
      val result = TestItvcLanguageController.langToCall.apply("cy").toString
      result shouldBe "/report-quarterly/income-and-expenses/view/language/cy"
    }
  }

  "The fallback url" should {

    s"be ${controllers.routes.HomeController.home().url}" in {
      val result = TestItvcLanguageController.fallbackURL
      result shouldBe controllers.routes.HomeController.home().url
    }
  }

  "The language map" should {

    "contain welsh and english" in {
      val result = TestItvcLanguageController.languageMap
      result shouldBe Map("english" -> Lang("en"), "cymraeg" -> Lang("cy"))
    }
  }

  "Calling the .switchToLanguage function" when {

    "providing the parameter 'english'" should {

      val result = TestItvcLanguageController.switchToLanguage("english")(fakeRequestNoSession)

      "return a Redirect status (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "use the English language" in {
        cookies(result).get(Play.langCookieName) shouldBe
          Some(Cookie("PLAY_LANG", "en", None, "/", None, secure = false, httpOnly = true))
      }
    }

    "providing the parameter 'cymraeg'" should {

      val result = TestItvcLanguageController.switchToLanguage("cymraeg")(fakeRequestNoSession)

      "return a Redirect status (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "use the Welsh language" in {
        cookies(result).get(Play.langCookieName) shouldBe
          Some(Cookie("PLAY_LANG", "cy", None, "/", None, secure = false, httpOnly = true))
      }
    }

    "providing an unsupported language parameter" should {

      TestItvcLanguageController.switchToLanguage("english")(fakeRequestNoSession)
      lazy val result = TestItvcLanguageController.switchToLanguage("orcish")(fakeRequestNoSession)

      "return a Redirect status (303)" in {
        status(result) shouldBe Status.SEE_OTHER
      }

      "keep the current language" in {
        cookies(result).get(Play.langCookieName) shouldBe
          Some(Cookie("PLAY_LANG", "en", None, "/", None, secure = false, httpOnly = true))
      }
    }
  }
}
