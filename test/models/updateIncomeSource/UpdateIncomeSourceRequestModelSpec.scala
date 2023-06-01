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

package models.updateIncomeSource

import org.scalatest.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testConstants.UpdateIncomeSourceTestConstants.{request, requestJson, requestTaxYearSpecific, requestTaxYearSpecificJson}
import testUtils.TestSupport

class UpdateIncomeSourceRequestModelSpec extends TestSupport with Matchers {
  "The updateIncomeSourceRequestModel" should {
    "read cessation request from Json" in {
      Json.fromJson(requestJson)(UpdateIncomeSourceRequestModel.format) shouldBe JsSuccess(request)
    }

    "read TaxYearSpecific request from Json" in {
      Json.fromJson(requestTaxYearSpecificJson)(UpdateIncomeSourceRequestModel.format) shouldBe JsSuccess(requestTaxYearSpecific)
    }

    "write cessation request to Json" in {
      Json.toJson(request) shouldBe requestJson
    }

    "write TaxYearSpecific request to Json" in {
      Json.toJson(requestTaxYearSpecific) shouldBe requestTaxYearSpecificJson
    }
  }
}
