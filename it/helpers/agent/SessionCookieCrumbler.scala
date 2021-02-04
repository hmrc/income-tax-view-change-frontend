
package helpers.agent

import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.{WSCookie, WSResponse}
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Crypted}

trait SessionCookieCrumbler {

  private val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="

  val cookieSigner: DefaultCookieSigner

  private def crumbleCookie(cookie: WSCookie) = {
    val crypted = Crypted(cookie.value)
    val decrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).decrypt(crypted).value

    def decode(data: String): Map[String, String] = {
      // this part is hard coded because we are not certain at this time which hash algorithm is used by default
      val mac = data.substring(0, 40)
      val map = data.substring(41, data.length)

      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes

      if(cookieSigner.sign(map, key) != mac) {
        throw new RuntimeException("Cookie MAC didn't match content, this should never happen")
      }
      val Regex = """(.*)=(.*)""".r
      map.split("&").view.map {
        case Regex(k, v) => Map(k -> v)
      }.view.reduce(_ ++ _)
    }

    decode(decrypted)
  }

  def getSessionMap(wSResponse: WSResponse): Map[String, String] =
    wSResponse.cookie("mdtp").fold(Map.empty: Map[String, String])(data => crumbleCookie(data))

}
