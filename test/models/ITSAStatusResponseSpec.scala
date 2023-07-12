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

package models

import models.itsaStatus.{ITSAStatusResponseModel, StatusDetail}
import org.scalatest.Matchers
import play.api.http.Status
import play.api.libs.json.{JsSuccess, Json}
import testConstants.ITSAStatusTestConstants._
import testUtils.TestSupport

class ITSAStatusResponseSpec extends TestSupport with Matchers {
  "The ITSAStatusResponseModel" should {
    "read response to model " in {
      Json.fromJson(successITSAStatusResponseJson)(ITSAStatusResponseModel.format) shouldBe JsSuccess(successITSAStatusResponseModel)
    }

    "read minimal response to model" in {
      Json.fromJson(successITSAStatusResponseModelMinimalJson)(ITSAStatusResponseModel.format) shouldBe JsSuccess(successITSAStatusResponseModelMinimal)
    }

    "read StatusDetailMinimal response to model" in {
      Json.fromJson(statusDetailMinimalJson)(StatusDetail.format) shouldBe JsSuccess(statusDetailMinimal)
    }
  }

  "The ITSAStatusResponseError" should {
    "have the correct status code in the model" in {
      errorITSAStatusError.status shouldBe Status.BAD_REQUEST
    }
    "have the correct Error Message" in {
      errorITSAStatusError.reason shouldBe "Dummy message"
    }
  }
}
