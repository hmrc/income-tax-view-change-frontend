
package controllers

import helpers.ComponentSpecBase
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.SEE_OTHER
import org.scalatest.Matchers._

class ItvcLanguageControllerISpec extends ComponentSpecBase {

  val testRefererRoute: String = "/test/referer/route"
  val testRefererRouteWithFragment: String = "/test/referer/route#fragment"

  "GET /language/cymraeg" should {
    "update the PLAY_LANG cookie to cy and return the user where they were when a REFERER is in the headers" in {
      lazy val resultCy: WSResponse = getWithHeaders("/switch-to-welsh", "REFERER" -> testRefererRoute)
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "preserve the fragment in the redirect url" in {
      lazy val resultCy: WSResponse = getWithHeaders("/switch-to-welsh?fragment=fragment", "REFERER" -> testRefererRoute)
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRouteWithFragment)
      )
    }

    "update the PLAY_LANG cookie to cy and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultCy: WSResponse = IncomeTaxViewChangeFrontend.get("/switch-to-welsh")
      resultCy.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultCy.headers.toString.contains("PLAY_LANG=cy;") shouldBe true
      resultCy should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.show().url)
      )
    }
  }

  "GET /language/english" should {
    "update the PLAY_LANG cookie to en and return the user where they were when a REFERER is in the headers" in {
      lazy val resultEn: WSResponse = getWithHeaders("/language/english", "REFERER" -> testRefererRoute)
      resultEn.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") shouldBe true
      resultEn should have(
        httpStatus(SEE_OTHER),
        redirectURI(testRefererRoute)
      )
    }

    "update the PLAY_LANG cookie to en and return the user to the overview page when REFERER is not in the headers" in {
      lazy val resultEn: WSResponse = getWithHeaders("/language/english")
      resultEn.headers.isDefinedAt("Set-Cookie") shouldBe true
      resultEn.headers.toString.contains("PLAY_LANG=en;") shouldBe true
      resultEn should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.show().url)
      )
    }
  }
}
