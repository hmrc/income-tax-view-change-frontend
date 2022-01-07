/*
 * Copyright 2022 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import testConstants.NinoLookupTestConstants._
import models.core.{NinoResponseError, NinoResponseSuccess}
import org.scalatest.Matchers
import play.api.libs.json.Json
import testUtils.UnitSpec

class NinoResponseModelSpec  extends UnitSpec with Matchers {

  "The NINO model" should {
    "be formatted to JSON correctly" in {
      Json.toJson[NinoResponseSuccess](testNinoModel) shouldBe testNinoModelJson
    }
    "be able to parse a JSON input as a string into the model" in {
      Json.parse(testNinoModelJson.toString).as[NinoResponseSuccess] shouldBe testNinoModel
    }
   }

  it should {
    "have the correct NINO" in {
      testNinoModel.nino shouldBe testNino
    }
  }

  "The Nino Error Model" should {
    "be formatted to Json correctly" in {
      Json.toJson[NinoResponseError](testNinoErrorModel) shouldBe testNinoErrorModelJson
    }
    "be able to parse a JSON input as a string into the model" in {
      Json.parse(testNinoErrorModelJson.toString).as[NinoResponseError] shouldBe testNinoErrorModel
    }
  }

  it should {
    "have the correct status" in {
      testNinoErrorModel.status shouldBe testErrorStatus
    }
    "have the correct reason" in {
      testNinoErrorModel.reason shouldBe testErrorMessage
    }
  }
}
