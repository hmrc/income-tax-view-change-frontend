
package helpers.agent

import java.net.URLEncoder
import java.time.LocalDateTime

import play.api.libs.crypto.DefaultCookieSigner
import uk.gov.hmrc.crypto.{CompositeSymmetricCrypto, PlainText}
import uk.gov.hmrc.http.SessionKeys

trait SessionCookieBaker {

  private val cookieKey = "gvBoGdgzqG1AarzF1LY0zQ=="

  val cookieSigner: DefaultCookieSigner

  private def cookieValue(sessionData: Map[String, String]) = {
    def encode(data: Map[String, String]): PlainText = {
      val encoded = data.map {
        case (k, v) => URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
      }.mkString("&")
      val key = "yNhI04vHs9<_HWbC`]20u`37=NGLGYY5:0Tg5?y`W<NoJnXWqmjcgZBec@rOxb^G".getBytes
      PlainText(cookieSigner.sign(encoded, key) + "-" + encoded)
    }

    val encodedCookie = encode(sessionData)
    val encrypted = CompositeSymmetricCrypto.aesGCM(cookieKey, Seq()).encrypt(encodedCookie).value

    s"""mdtp="$encrypted"; Path=/; HTTPOnly"; Path=/; HTTPOnly"""
  }

  private def cookieData(additionalData: Map[String, String], timeStampRollback: Long): Map[String, String] = {

    val timeStamp = new java.util.Date().getTime
    val rollbackTimestamp = (timeStamp - timeStampRollback).toString

    Map(
      SessionKeys.lastRequestTimestamp -> rollbackTimestamp,
      SessionKeys.authToken -> "auth",
    ) ++ additionalData
  }

  def bakeSessionCookie(additionalData: Map[String, String] = Map(), timeStampRollback: Long = 0): String = {
    cookieValue(cookieData(additionalData, timeStampRollback))
  }
}
