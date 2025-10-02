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
import com.github.tomakehurst.wiremock.common.ConsoleNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.servicemocks.MTDAgentAuthStub.bakeSessionCookie
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WSResponse}
import testConstants.BaseIntegrationTestConstants.testSessionId

import scala.concurrent.Future

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

  def verifyPost(uri: String, optBody: Option[String], headers: (String, String)*): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    val postRequestWithHeaders = headers.foldLeft(postRequest)(
      (request, header) => request.withHeader(header._1, equalTo(header._2)))
    verify(postRequestWithHeaders)
  }

  def verifyPut(uri: String, optRequestBody: Option[String] = None): Unit = {
    val uriMapping = putRequestedFor(urlEqualTo(uri))
    val putRequest = optRequestBody match {
      case Some(body) => uriMapping.withRequestBody(equalTo(body))
      case None => uriMapping
    }
    verify(putRequest)
  }

  def verifyPostContaining(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(containing(body))
      case None => uriMapping
    }
    verify(postRequest)
  }

  def verifyPostDoesNotContain(uri: String, optBody: Option[String] = None): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = optBody match {
      case Some(body) => uriMapping.withRequestBody(containing(body))
      case None => uriMapping
    }
    verify(0, postRequest)
  }

  def verifyPostContainingJson(uri: String, bodyPart: Option[JsValue]): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = bodyPart match {
      case Some(js) =>
        val ignoreArrayOrder, ignoreExtraElements = true
        uriMapping.withRequestBody(equalToJson(js.toString, ignoreArrayOrder, ignoreExtraElements))
      case None =>
        uriMapping
    }
    verify(postRequest)
  }

  def verifyPostDoesNotContainJson(uri: String, bodyPart: Option[JsValue]): Unit = {
    val uriMapping = postRequestedFor(urlEqualTo(uri))
    val postRequest = bodyPart match {
      case Some(js) =>
        val ignoreArrayOrder, ignoreExtraElements = true
        uriMapping.withRequestBody(equalToJson(js.toString, ignoreArrayOrder, ignoreExtraElements))
      case None => uriMapping
    }
    verify(0, postRequest)
  }

  def verifyGet(uri: String, noOfCalls: Int = 1): Unit = {
    verify(exactly(noOfCalls), getRequestedFor(urlEqualTo(uri)))
  }

  def verifyGetWithHeaders(uri: String, headerKey: String, headerValue: String): Unit = {
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

  def stubPostWithRequest(url: String, requestBody: JsValue, status: Integer, responseBody: String): StubMapping =
    stubFor(post(urlEqualTo(url))
      .withRequestBody(equalToJson(requestBody.toString()))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPostWithRequestAndResponseHeaders(url: String, requestBody: JsValue, status: Integer, responseHeaders: Map[String, String] = Map()): StubMapping =
    stubFor(post(urlEqualTo(url))
      .withRequestBody(equalToJson(requestBody.toString()))
      .willReturn(
        aResponse().
          withStatus(status).
          withHeaders(toHttpHeaders(responseHeaders))
      )
    )

  def stubPostWithRequestResponseHeadersAndBody(url: String, requestBody: JsValue, status: Integer, responseHeaders: Map[String, String] = Map()): StubMapping =
    stubFor(post(urlEqualTo(url))
      .withRequestBody(equalToJson(requestBody.toString()))
      .willReturn(
        aResponse()
          .withBody("")
          .withStatus(status)
          .withHeaders(toHttpHeaders(responseHeaders))
      )
    )

  def stubPostWithHeader(url: String, status: Integer, key: String, header: String): StubMapping =
    stubFor(post(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withHeader(key, header)
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

  // for now overload the stubPut because there are quite a lot of other tests which do not have the request body supplied
  def stubPut(url: String, status: Integer, expectedRequestBody: String, responseBody: String): StubMapping =
    stubFor(put(urlEqualTo(url))
      .withRequestBody(equalToJson(expectedRequestBody)) // Ensure that the request body matches
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody)
      )
    )

  def stubPutWithHeaders(url: String, status: Integer, responseBody: String, headers: Map[String, String] = Map()): StubMapping = {

    stubFor(put(urlEqualTo(url))
      .willReturn(
        aResponse().
          withStatus(status).
          withBody(responseBody).
          withHeaders(toHttpHeaders(headers))
      )
    )
  }

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

  private def toHttpHeaders(toConvert: Map[String, String]): HttpHeaders = {
    val headersList = toConvert.map { case (key, value) =>
      new HttpHeader(key, value)
    }.toSeq
    new HttpHeaders(headersList: _*)
  }
}

trait WiremockHelper {
  self: GuiceOneServerPerSuite =>

  import WiremockHelper._

  lazy val ws = app.injector.instanceOf[WSClient]

  lazy val wmConfig = wireMockConfig().port(wiremockPort).notifier(new ConsoleNotifier(false)) // for more verbose logging

  lazy val wireMockServer = new WireMockServer(wmConfig)

  def startWiremock() = {
    wireMockServer.start()
    WireMock.configureFor(wiremockHost, wiremockPort)
  }

  def stopWiremock() = wireMockServer.stop()

  def resetWiremock() = WireMock.reset()

  def buildClient(path: String) = ws.url(s"http://localhost:$port/report-quarterly/income-and-expenses/view$path")
    .withFollowRedirects(false)

  def buildMTDClient(path: String,
                     additionalCookies: Map[String, String] = Map.empty,
                     optBody: Option[Map[String, Seq[String]]] = None): Future[WSResponse] = {
    optBody match {
      case Some(body) => buildPOSTMTDPostClient(path, additionalCookies, body)
      case _ => buildGETMTDClient(path, additionalCookies)
    }
  }

  def buildGETMTDClient(
                         path: String,
                         additionalCookies: Map[String, String] = Map.empty,
                         isCY: Boolean = false,
                         additionalHeaders: Map[String, String] = Map.empty
                       ): Future[WSResponse] = {
    val defaultHeader = Map(HeaderNames.COOKIE -> bakeSessionCookie(Map.empty ++ additionalCookies),
      "X-Session-ID" -> testSessionId)
    val defaultAndAdditionalHeaders = defaultHeader ++ additionalHeaders
    val headers = if (isCY) defaultAndAdditionalHeaders ++ Map(HeaderNames.ACCEPT_LANGUAGE -> "cy") else defaultAndAdditionalHeaders
    buildClient(path)
      .withHttpHeaders(headers.toSeq: _*)
      .get()
  }

  def buildPOSTMTDPostClient(
                              path: String,
                              additionalCookies: Map[String, String] = Map.empty,
                              body: Map[String, Seq[String]]
                            ): Future[WSResponse] =
    buildClient(path)
      .withMethod("POST")
      .withHttpHeaders(HeaderNames.COOKIE -> bakeSessionCookie(additionalCookies),
        "Csrf-Token" -> "nocheck",
        "X-Session-ID" -> testSessionId,
        "X-Session-Id" -> testSessionId,
        "sessionId-qqq" -> testSessionId
      ).post(body)
}

