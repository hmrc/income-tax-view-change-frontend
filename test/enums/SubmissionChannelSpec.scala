/*
 * Copyright 2026 HM Revenue & Customs
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

package enums

import models.liabilitycalculation.SubmissionChannel
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json._
import testUtils.TestSupport

class SubmissionChannelSpec extends TestSupport {

  "SubmissionTrigger JSON format" should {

    "write all submission channel to their correct string values" in {
      SubmissionChannel.allSubmissionChannels.foreach { trigger =>
        Json.toJson(trigger) mustBe JsString(trigger.toString)
      }
    }

    "read all valid submission channel strings" in {
      SubmissionChannel.allSubmissionChannels.foreach { trigger =>
        Json.fromJson[SubmissionChannel](JsString(trigger.toString)) mustBe JsSuccess(trigger)
      }
    }

    "fail to read an unknown submission trigger" in {
      val result =
        Json.fromJson[SubmissionChannel](JsString("unknownChannel"))

      result mustBe JsError("Unknown SubmissionChannel: unknownChannel")
    }

    "fail to read a non-string JSON value" in {
      val result =
        Json.fromJson[SubmissionChannel](JsNumber(123))

      result mustBe JsError("SubmissionChannel must be a string")
    }
  }
}
