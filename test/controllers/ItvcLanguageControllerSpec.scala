/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.i18n.Lang
import utils.TestSupport

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
}
