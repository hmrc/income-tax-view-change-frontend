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

package controllers

import helpers.ComponentSpecBase
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.SEE_OTHER

class ItvcLanguageControllerISpec extends ComponentSpecBase {

  val testRefererRoute:             String = "/test/referer/route"
  val testRefererRouteWithFragment: String = "/test/referer/route#fragment"

  val pathCY = "/switch-to-welsh"
  val pathEN = "/switch-to-english"

  s"GET $pathCY" should {
    "update the PLAY_LANG cookie to cy and return the user where they were when a REFERER is in the headers" in {
      lazy val resultCy = buildGETMTDClient(pathCY, additionalHeaders = Map("REFERER" -> testRefererRoute)).futureValue
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "preserve the fragment in the redirect url" in {
      lazy val resultCy = buildGETMTDClient(
        pathCY + "?fragment=fragment",
        additionalHeaders = Map("REFERER" -> testRefererRoute)
      ).futureValue

      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRouteWithFragment)
      )
    }

    "update the PLAY_LANG cookie to cy and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultCy = buildGETMTDClient(pathCY).futureValue
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.show().url)
      )
    }
  }

  s"GET $pathEN" should {
    "update the PLAY_LANG cookie to en and return the user where they were when a REFERER is in the headers" in {
      lazy val resultEn: WSResponse =
        buildGETMTDClient(pathEN, additionalHeaders = Map("REFERER" -> testRefererRoute)).futureValue
      resultEn.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") shouldBe true
      resultEn should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "update the PLAY_LANG cookie to en and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultEn: WSResponse = buildGETMTDClient(pathEN).futureValue
      resultEn.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") shouldBe true
      resultEn should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.show().url)
      )
    }

  }
}
