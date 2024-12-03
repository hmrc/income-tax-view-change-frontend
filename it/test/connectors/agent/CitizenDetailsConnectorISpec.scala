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

package connectors.agent

import com.github.tomakehurst.wiremock.client.WireMock
import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import _root_.helpers.servicemocks.AuditStub
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Injecting

class CitizenDetailsConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: CitizenDetailsConnector = app.injector.instanceOf[CitizenDetailsConnector]

  val saUtr = "testUtr"

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  "CitizenDetailsConnector" when {
    "sending a request" should {
      "return a successful response" in {
        WiremockHelper.stubGet(s"/citizen-details/sautr/$saUtr", OK, "{}")

        val result = connector.getCitizenDetailsBySaUtr(saUtr).futureValue

        result shouldBe ""
      }
      "return an error when the request fails" in {
        WiremockHelper.stubGet(s"/citizen-details/sautr/$saUtr", INTERNAL_SERVER_ERROR, "{}")

        val result = connector.getCitizenDetailsBySaUtr(saUtr).futureValue

        result shouldBe ""
      }
    }
  }
}
