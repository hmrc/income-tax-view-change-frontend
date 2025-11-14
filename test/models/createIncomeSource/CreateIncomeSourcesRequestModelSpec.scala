/*
 * Copyright 2023 HM Revenue & Customs
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

package models.createIncomeSource

import connectors.helpers.IncomeSourcesDataHelper
import play.api.libs.json.{JsSuccess, Json}
import testUtils.TestSupport

import scala.util.Try

class CreateIncomeSourcesRequestModelSpec extends TestSupport with IncomeSourcesDataHelper {
  "CreateBusinessIncomeSourceRequest" should {
    "read from Json" in {
      Json.fromJson(createBusinessDetailsRequestObjectJson)(CreateBusinessIncomeSourceRequest.format) shouldBe JsSuccess(createBusinessDetailsRequestObject)
    }
    "write to Json" in {
      Json.toJson(createBusinessDetailsRequestObject) shouldBe createBusinessDetailsRequestObjectJson
    }
    "check require field" in {
      Try(CreateBusinessIncomeSourceRequest(mtdbsa = "XIAT00000000000", businessDetails = List())).failed.get.getMessage shouldBe "requirement failed: Only single business can be created at a time"
    }
  }

  "PropertyDetails" should {
    "check require fields" in {
      Try(PropertyDetails("", Some("CASH"), "")).failed.get.getMessage shouldBe "requirement failed: Trading start date must be provided"
      Try(PropertyDetails("2022-01-01", Some(""), "2022-01-01")).failed.get.getMessage shouldBe "requirement failed: Accounting method must be capitalised"
      Try(PropertyDetails("2022-01-02", Some("CASH"), "2022-01-01")).failed.get.getMessage shouldBe "requirement failed: Trading start date and start date must be the same"
    }
  }

  "CreateForeignPropertyIncomeSource" should {
    "read from Json" in {
      Json.fromJson(createForeignPropertyRequestObjectJson)(CreateForeignPropertyIncomeSourceRequest.format) shouldBe JsSuccess(createForeignPropertyRequestObject)
    }
    "write to Json" in {

      Json.toJson(createForeignPropertyRequestObject) shouldBe createForeignPropertyRequestObjectJson
    }
  }

  "CreateUKPropertyIncomeSource" should {
    "read from Json" in {
      Json.fromJson(createUKPropertyRequestObjectJson)(CreateUKPropertyIncomeSourceRequest.format) shouldBe JsSuccess(createUKPropertyRequestObject)
    }
    "write to Json" in {
      Json.toJson(createUKPropertyRequestObject) shouldBe createUKPropertyRequestObjectJson
    }
  }
}