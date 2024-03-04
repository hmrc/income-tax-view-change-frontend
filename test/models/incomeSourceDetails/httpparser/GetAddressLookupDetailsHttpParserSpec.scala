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

package models.incomeSourceDetails.httpparser

import models.incomeSourceDetails.{Address, BusinessAddressModel}
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser._
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

class GetAddressLookupDetailsHttpParserSpec extends UnitSpec{

  val testHttpVerb = "GET"
  val testUri = "/test"

  val testAddressJson: JsObject = Json.obj(
   "lines" -> Seq("address line 1", "address line 2", "address line 3"), "postcode" -> Some("SE1 9DG")
  )
  val testValidJson: JsObject = Json.obj(
    "auditRef" -> "", "address" -> testAddressJson
  )
  //test
  "GetAddressLookupDetailsHttpParserSpec" when {
    "read" should {
      "return an OK status and parse the json data successfully" in {
        val httpResponse = HttpResponse(OK, json = testValidJson, headers = Map.empty)

        lazy val result = getAddressLookupDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)
        result shouldBe Right(Some(BusinessAddressModel(auditRef = "",
          Address(lines = Seq("address line 1", "address line 2", "address line 3"), postcode = Some("SE1 9DG"))
        )))
      }
      "return an OK status but parse invalid json data as a response" in {
        val httpResponse = HttpResponse(OK, json = Json.obj(), headers = Map.empty)

        lazy val result = getAddressLookupDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        result shouldBe Left(InvalidJson)
      }
      "return a NOT FOUND status as None" in {
        val httpResponse = HttpResponse(NOT_FOUND, body = "")

        lazy val result = getAddressLookupDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        result shouldBe Right(None)
      }
      "return UnexpectedGetStatusFailure for any other http status return and not handled above" in {
        val httpResponse = HttpResponse(INTERNAL_SERVER_ERROR, body = "")

        lazy val result = getAddressLookupDetailsHttpReads.read(testHttpVerb, testUri, httpResponse)

        result shouldBe Left(UnexpectedGetStatusFailure(INTERNAL_SERVER_ERROR))
      }
    }
  }

}
