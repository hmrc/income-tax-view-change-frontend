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

package common.helpers.servicemocks

import common.helpers.WiremockHelper
import models.admin.{FeatureSwitch, FeatureSwitchName}
import play.api.http.Status.OK
import play.api.libs.json.{JsValue, Json}

object FeatureSwitchStub {

  def featureSwitchUrl(): String = s"/features"

  def stubGetFeatureSwitches(featureSwitches: List[FeatureSwitchName] = List()): Unit = {
    WiremockHelper.stubGet(featureSwitchUrl(), OK, featureSwitchResponse(featureSwitches).toString())
  }
  
  def featureSwitchResponse(featureSwitches: List[FeatureSwitchName]): JsValue = {
    Json.toJson(FeatureSwitchName.allFeatureSwitches.map { fsName =>
      fsName.name match
        case fs if featureSwitches.map(_.name).contains(fs) => FeatureSwitch(fsName, true)
        case _ => FeatureSwitch(fsName, false)
    }.toList)
  }
}
