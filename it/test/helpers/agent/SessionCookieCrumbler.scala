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

package helpers.agent

import play.api.libs.crypto.DefaultCookieSigner
import play.api.libs.ws.{WSCookie, WSResponse}
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, Crypted, SymmetricCryptoFactory}

trait SessionCookieCrumbler {

  private val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="

  val cookieSigner: DefaultCookieSigner

  private def crumbleCookie(cookie: WSCookie) = {
    val crypted = Crypted(cookie.value)
    val decrypted = SymmetricCryptoFactory.aesGcmCrypto(cookieKey).decrypt(crypted).value

    def decode(data: String): Map[String, String] = {
      // this part is hard coded because we are not certain at this time which hash algorithm is used by default
      val mac = data.substring(0, 40)
      val map = data.substring(41, data.length)

      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes

      if (cookieSigner.sign(map, key) != mac) {
        throw new RuntimeException("Cookie MAC didn't match content, this should never happen")
      }
      val Regex = """(.*)=(.*)""".r
      map.split("&").view.map {
        case Regex(k, v) => Map(k -> v)
        case _ => Map(""->"")
      }.view.reduce(_ ++ _)
    }

    decode(decrypted)
  }

  def getSessionMap(wSResponse: WSResponse): Map[String, String] =
    wSResponse.cookie("mdtp").fold(Map.empty: Map[String, String])(data => crumbleCookie(data))

}
