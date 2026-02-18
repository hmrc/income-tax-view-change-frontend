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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import connectors.constants.ITSAStatusUpdateConnectorConstants._
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseError, ITSAStatusResponseModel}
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import testConstants.ITSAStatusTestConstants.{successITSAStatusResponseJson, successITSAStatusResponseModel}

class ITSAStatusConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: ITSAStatusConnector = app.injector.instanceOf[ITSAStatusConnector]

  "ITSAStatusUpdateConnector" when {

    ".getITSAStatusDetail()" when {

      "happy path" should {

        "return a successful response" in {

          val responseBody = Json.arr(successITSAStatusResponseJson)
          val year = "2020"
          val futureYears = true
          val history = true
          val url = s"/income-tax-obligations/itsa-status/status/$taxableEntityId/$year?futureYears=${futureYears.toString}&history=${history.toString}"

          WiremockHelper.stubGet(url, OK, responseBody.toString())

          val result: Either[ITSAStatusResponse, List[ITSAStatusResponseModel]] =
            connector.getITSAStatusDetail(taxableEntityId, "2020", futureYears = true, history = true).futureValue
          result shouldBe Right(List(successITSAStatusResponseModel))

          WiremockHelper.verifyGet(
            uri = url
          )
        }
      }

      "unhappy path" when {

        "return a fail response" in {
          val year = "2020"
          val futureYears = true
          val history = true
          val url = s"/income-tax-obligations/itsa-status/status/$taxableEntityId/$year?futureYears=${futureYears.toString}&history=${history.toString}"

          WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, "")

          val result: Either[ITSAStatusResponse, List[ITSAStatusResponseModel]] =
            connector.getITSAStatusDetail(taxableEntityId, "2020", futureYears = true, history = true).futureValue
          result shouldBe Left(ITSAStatusResponseError(INTERNAL_SERVER_ERROR, ""))

          WiremockHelper.verifyGet(
            uri = url
          )
        }
      }
    }
  }
}