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

package connectors

import _root_.helpers.servicemocks.AuditStub
import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock
import models.core.NormalMode
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.UnexpectedGetStatusFailure
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import models.incomeSourceDetails.{Address, BusinessAddressModel}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.Injecting
import testConstants.AddressLookupTestConstants

class AddressLookupConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: AddressLookupConnector = app.injector.instanceOf[AddressLookupConnector]

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  "AddressLookupConnector" when {
    ".initialiseAddressLookup()" when {
      "sending a request (Individual)" should {
        
        "return a successful response" in {

          WiremockHelper.stubPostWithRequest(s"/api/v2/init", AddressLookupTestConstants.ukRequestBodyIndividual, ACCEPTED, "{}")

          val result = connector.initialiseAddressLookup(isAgent = false, mode = NormalMode, false, ukOnly = true).futureValue

          result shouldBe Right(PostAddressLookupSuccessResponse(None))
          WiremockHelper.verifyPost("/api/v2/init")
        }

        "return a successful response - ukOnly = false" in {
          WiremockHelper.stubPostWithRequest(s"/api/v2/init", AddressLookupTestConstants.internationalRequestBodyInvididual, ACCEPTED, "{}")

          val result = connector.initialiseAddressLookup(isAgent = false, mode = NormalMode, false, ukOnly = false).futureValue

          result shouldBe Right(PostAddressLookupSuccessResponse(None))
          WiremockHelper.verifyPost("/api/v2/init")
        }

        "return an error when the request fails" in {

          WiremockHelper.stubPostWithRequest(s"/api/v2/init", AddressLookupTestConstants.ukRequestBodyIndividual, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.initialiseAddressLookup(isAgent = false, mode = NormalMode, false, ukOnly = true).futureValue

          result shouldBe Left(UnexpectedPostStatusFailure(INTERNAL_SERVER_ERROR))
          WiremockHelper.verifyPost("/api/v2/init")
        }
      }

      "sending a request (Agent)" should {

        "return a successful response - ukOnly = true" in {
          WiremockHelper.stubPostWithRequest(s"/api/v2/init", AddressLookupTestConstants.ukRequestBodyAgent, ACCEPTED, "{}")

          val result = connector.initialiseAddressLookup(isAgent = true, mode = NormalMode, false, ukOnly = true).futureValue

          result shouldBe Right(PostAddressLookupSuccessResponse(None))
          WiremockHelper.verifyPost("/api/v2/init")
        }

        "return a successful response - ukOnly = false" in {
          WiremockHelper.stubPostWithRequest(s"/api/v2/init", AddressLookupTestConstants.internationalRequestBodyAgent, ACCEPTED, "{}")

          val result = connector.initialiseAddressLookup(isAgent = true, mode = NormalMode, false, ukOnly = false).futureValue

          result shouldBe Right(PostAddressLookupSuccessResponse(None))
          WiremockHelper.verifyPost("/api/v2/init")
        }

        "return an error when the request fails" in {
          WiremockHelper.stubPostWithRequest("/api/v2/init", AddressLookupTestConstants.ukRequestBodyAgent, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.initialiseAddressLookup(isAgent = true, mode = NormalMode, false, ukOnly = true).futureValue

          result shouldBe Left(UnexpectedPostStatusFailure(INTERNAL_SERVER_ERROR))
          WiremockHelper.verifyPost("/api/v2/init")
        }
      }
    }
    ".getAddressDetails()" when {
      "sending a request" should {
        "return a successful response" in {
          val responseBody =
            """
              |{
              |  "auditRef": "auditRef",
              |  "address": {
              |    "lines": ["Line 1", "Line 2"],
              |    "postcode": "AA1 1AA"
              |  }
              |}
              |""".stripMargin

          val id = "1234"

          WiremockHelper.stubGet(s"/api/v2/confirmed?id=$id", OK, responseBody)

          val result = connector.getAddressDetails(id)(hc).futureValue

          result shouldBe Right(Some(BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))))
          WiremockHelper.verifyGet(s"/api/v2/confirmed?id=$id")
        }
        "return an error when getting address details fails" in {

          val id = "1234"

          WiremockHelper.stubGet(s"/api/v2/confirmed?id=$id", INTERNAL_SERVER_ERROR, "{}")

          val result = connector.getAddressDetails(id)(hc).futureValue

          result shouldBe Left(UnexpectedGetStatusFailure(500))
          WiremockHelper.verifyGet(s"/api/v2/confirmed?id=$id")
        }
      }
    }
  }
}
