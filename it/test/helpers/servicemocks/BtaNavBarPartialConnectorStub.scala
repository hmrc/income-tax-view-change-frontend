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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.client.WireMock._
import helpers.WiremockHelper
import play.api.libs.json.JsValue

object BtaNavBarPartialConnectorStub {

  def withResponseForNavLinks()(status: Int, optBody: Option[String]): Unit =
    stubFor(get(urlEqualTo(s"/business-account/partial/nav-links")) willReturn {
      val coreResponse = aResponse().withStatus(status)
      optBody match {
        case Some(body) => coreResponse.withBody(body)
        case _ => coreResponse
      }
    })

  def verifyNavlinksContent(count: Int): Unit =
    verify(count, getRequestedFor(urlMatching("/business-account/partial/nav-links")))

  val testNavLinkJson: String =
    """
      |{
      | "home":{
      |         "en" : "Home",
      |         "cy" : "Hafan",
      |         "url": "http://localhost:9020/business-account"
      |       },
      | "account":{
      |           "en" : "Manage account",
      |           "cy" : "Rheoli'r cyfrif",
      |           "url" : "http://localhost:9020/business-account/manage-account"
      |       },
      | "messages":{
      |             "en" : "Messages",
      |             "cy" : "Negeseuon",
      |             "url" : "http://localhost:9020/business-account/messages",
      |             "alerts": 5
      |       },
      | "help":{
      |         "en" : "Help and contact",
      |         "cy" : "Cymorth a chysylltu",
      |         "url" : "http://localhost:9733/business-account/help"
      |       },
      | "forms":{
      |          "en" : "Track your forms{0}",
      |          "cy": "Gwirio cynnydd eich ffurflenni{0}",
      |          "url":"/track/bta",
      |          "alerts": 0
      |       }
      | }""".stripMargin

  val getBtaNavLinksUrl: String = "/business-account/partial/nav-links"

  def stubBtaNavPartialResponse()(status: Int, response: JsValue): Unit =
    WiremockHelper.stubGet(getBtaNavLinksUrl, status, response.toString())

  def verifyBtaNavPartialResponse(): Unit =
    WiremockHelper.verifyGet(getBtaNavLinksUrl)

}
