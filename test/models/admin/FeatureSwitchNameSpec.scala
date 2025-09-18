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

package models.admin

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsString, Json}

class FeatureSwitchNameSpec extends PlaySpec{

  "FeatureSwitchName.reads" should {
    "deserialize a valid FeatureSwitchName from JSON string" in {
      val json = JsString(ITSASubmissionIntegration.name)
      json.as[FeatureSwitchName] mustBe ITSASubmissionIntegration
    }

    "deserialize another valid FeatureSwitchName" in {
      val json = JsString(NavBarFs.name)
      json.as[FeatureSwitchName] mustBe NavBarFs
    }

    "deserialize all known FeatureSwitchNames successfully (round-trip test)" in {
      FeatureSwitchName.allFeatureSwitches.foreach { fs =>
        val json = Json.toJson(fs) // uses writes
        json.as[FeatureSwitchName] mustBe fs // uses reads
      }
    }

    "return InvalidFS for an unknown feature switch" in {
      val json = JsString("NotARealFeatureSwitch")
      json.as[FeatureSwitchName] mustBe InvalidFS
    }
  }

}
