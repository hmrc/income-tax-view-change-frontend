/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.agent

import helpers.agent.ComponentSpecBase
import play.api.libs.ws.WSResponse
import play.api.http.Status.SEE_OTHER


class AgentLanguageControllerISpec extends ComponentSpecBase {

  val testRefererRoute: String = "/test/referer/route"

  "GET /agents/language/cymraeg" should {
    "update the PLAY_LANG cookie to cy and return the user where they were when a REFERER is in the headers" in {
      lazy val resultCy: WSResponse = IncomeTaxViewChangeFrontend.getWithHeaders("/language/cymraeg", ("REFERER" -> testRefererRoute))
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "update the PLAY_LANG cookie to cy and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultCy: WSResponse = IncomeTaxViewChangeFrontend.get("/language/cymraeg")
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
      )
    }
  }

  "GET /agents/language/english" should {
    "update the PLAY_LANG cookie to en and return the user where they were when a REFERER is in the headers" in {
      lazy val resultEn: WSResponse = IncomeTaxViewChangeFrontend.getWithHeaders("/language/english", ("REFERER" -> testRefererRoute))
      resultEn.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") shouldBe true
      resultEn should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "update the PLAY_LANG cookie to en and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultEn: WSResponse = IncomeTaxViewChangeFrontend.get("/language/english")
      resultEn.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") shouldBe true
      resultEn should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.agent.routes.EnterClientsUTRController.show.url)
      )
    }
  }

}
