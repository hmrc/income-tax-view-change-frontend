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

package helpers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

object WiremockHelper extends Eventually with IntegrationPatience {
  val wiremockPort = 11111
  val wiremockHost = "localhost"
  val url = s"http://$wiremockHost:$wiremockPort"

  def verifyPost(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    verify(postRequest)
  }

  def verifyPostContaining(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(containing(body))
      case None => uriMapping
    }
    verify(postRequest)
  }

  def verifyPostContainingJson(uri:String, bodyPart: Option[JsValue]): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = bodyPart match {
      case Some(js) =>
        val ignoreArrayOrder, ignoreExtraElements = true
        uriMapping.withRequestBody(equalToJson(js.toString, ignoreArrayOrder, ignoreExtraElements))
      case None => uriMapping
    }
    verify(postRequest)
  }

  def verifyGet(uri: String): Unit = {
    verify(getRequestedFor(urlEqualTo(uri)))
  }

  def verifyGetWithHeader(uri: String, headerKey: String, headerValue: String): Unit = {
    verify(getRequestedFor(urlEqualTo(uri)).withHeader(headerKey, equalTo(headerValue)))
  }

  def stubGet(url: String, status: Integer, body: String): StubMapping =
    stubFor(get(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(body)
      )
    )

  def stubPost(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPut(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(put(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPatch(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(patch(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubDelete(url: String, status: Integer, responseBody: String): StubMapping =
    stubFor(delete(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )
}

trait WiremockHelper {
  self: GuiceOneServerPerSuite =>

  import WiremockHelper._

  lazy val ws = app.injector.instanceOf[WSClient]

  lazy val wmConfig = wireMockConfig().port(wiremockPort)
  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock() = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock() = wireMockServer.stop()

  def resetWiremock() = WireMock.reset()

  def buildClient(path: String) = ws.url(s"http://localhost:$port/report-quarterly/income-and-expenses/view$path")
    .withFollowRedirects(false)
}

