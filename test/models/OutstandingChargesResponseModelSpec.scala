/*
 * Copyright 2021 HM Revenue & Customs
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

package models

import assets.OutstandingChargesTestConstants._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testUtils.UnitSpec

class OutstandingChargesResponseModelSpec extends UnitSpec with Matchers {

  "The OutstandingChargesModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[OutstandingChargesModel](testValidOutstandingChargesModel) shouldBe testValidOutStandingChargeModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[OutstandingChargesModel](testValidOutStandingChargeModelJson).fold(
        invalid => invalid,
        valid => valid) shouldBe testValidOutstandingChargesModel
    }

  }

  "The OutstandingChargesErrorModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[OutstandingChargesErrorModel](testOutstandingChargesErrorModel) shouldBe testOutstandingChargesErrorModelJson
    }

    "be able to parse a JSON into the Model" in {
      Json.fromJson[OutstandingChargesErrorModel](testOutstandingChargesErrorModelJson) shouldBe JsSuccess(testOutstandingChargesErrorModel)
    }
  }

}