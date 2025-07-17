/*
 * Copyright 2025 HM Revenue & Customs
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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import models.penalties.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed, GetPenaltyDetailsResponse}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status._
import testConstants.PenaltiesIntegrationTestConstants._
class GetPenaltyDetailsConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  val connector: GetPenaltyDetailsConnector = app.injector.instanceOf[GetPenaltyDetailsConnector]
  val url: String = "/penalties/ITSA/etmp/penalties/MTDITID/123456789"
  val mtditid: String = "123456789"



  "GetPenaltyDetailsConnector" should {

    "return a successful OK response when called" in {

      WiremockHelper.stubGet(url, OK, getPenaltyDetailsJson.toString())
      val result: GetPenaltyDetailsResponse = connector.getPenaltyDetails(mtditid).futureValue
      result.isRight shouldBe true
    }

    "return an OK but with a GetPenaltyDetailsMalformed response when the JSON returned is malformed" in {
      WiremockHelper.stubGet(url, OK, malformedBodyJson.toString())
      val result: GetPenaltyDetailsResponse = connector.getPenaltyDetails(mtditid).futureValue
      result.isLeft shouldBe true
      result shouldBe Left(GetPenaltyDetailsMalformed)
    }

    "return a NotFound response when no data was found" in {
      WiremockHelper.stubGet(url, NOT_FOUND, "{}")
      val result: GetPenaltyDetailsResponse = connector.getPenaltyDetails(mtditid).futureValue
      result.isRight shouldBe true
    }

    "return an InternalServerError response when an unexpected error has occurred" in {
      WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, "{}")
      val result: GetPenaltyDetailsResponse = connector.getPenaltyDetails(mtditid).futureValue
      result.isLeft shouldBe true
      result shouldBe Left(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR))
    }

  }

}
