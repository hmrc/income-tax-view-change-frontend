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

package models.createIncomeSource

import play.api.libs.json.{JsSuccess, Json}
import testUtils.TestSupport

class CreateIncomeSourceResponseSpec extends TestSupport {
  val createIncomeSourcesResponse = CreateIncomeSourceResponse("1234567")
  val createIncomeSourcesResponseJson = Json.obj("incomeSourceId" -> createIncomeSourcesResponse.incomeSourceId)
  "The CreateIncomeSourcesResponseModel" should {
    "read from Json" in {
      Json.fromJson(createIncomeSourcesResponseJson)(CreateIncomeSourceResponse.format) shouldBe JsSuccess(createIncomeSourcesResponse)
    }

    "write to Json" in {
      Json.toJson(createIncomeSourcesResponse) shouldBe createIncomeSourcesResponseJson
    }
  }
}