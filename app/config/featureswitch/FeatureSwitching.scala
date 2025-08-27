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

package config.featureswitch

import auth.MtdItUser
import config.FrontendAppConfig
import models.admin.{FeatureSwitch, FeatureSwitchName}

trait FeatureSwitching {

  val appConfig: FrontendAppConfig

  val FEATURE_SWITCH_ON = "true"
  val FEATURE_SWITCH_OFF = "false"

  def isEnabledFromConfig(featureSwitch: FeatureSwitchName): Boolean = {
    sys.props.get(featureSwitch.name) orElse
      appConfig.config.getOptional[String](s"feature-switch.enable-${featureSwitch.name}") contains FEATURE_SWITCH_ON
  }

  def isEnabled(featureSwitch: FeatureSwitchName)
               (implicit user: MtdItUser[_]): Boolean = {
    isEnabled(featureSwitch, user.featureSwitches)
  }

  def isEnabled(featureSwitch: FeatureSwitchName, fs: List[FeatureSwitch]): Boolean = {
    if (appConfig.readFeatureSwitchesFromMongo) {
      fs.exists(x => x.name.name == featureSwitch.name && x.isEnabled)
    } else {
      isEnabledFromConfig(featureSwitch)
    }
  }
  def areAllEnabled(featureSwitches: FeatureSwitchName*): Boolean = {
    featureSwitches.foldLeft(false) {
      (_, switch) =>
        isEnabledFromConfig(switch)
    }
  }

  def enable(featureSwitch: FeatureSwitchName): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_ON

  def enable(featureSwitches: FeatureSwitchName*): Unit = {
    featureSwitches.foreach(
      fs => sys.props += fs.name -> FEATURE_SWITCH_ON
    )
  }

  def disable(featureSwitch: FeatureSwitchName): Unit =
    sys.props += featureSwitch.name -> FEATURE_SWITCH_OFF

  def setFS(featureSwitchName: FeatureSwitchName, enabled: Boolean): Unit = {
    if (enabled) {
      enable(featureSwitchName)
    } else {
      disable(featureSwitchName)
    }
  }

  protected implicit class FeatureOps(feature: FeatureSwitchName)(implicit user: MtdItUser[_]) {
    def fold[T](ifEnabled: => T, ifDisabled: => T): T = {
      if (isEnabled(feature)) ifEnabled
      else ifDisabled
    }
  }

  def getFSList: List[FeatureSwitch] = {
    FeatureSwitchName.allFeatureSwitches.toList.map { currentFS =>
      FeatureSwitch(currentFS, isEnabled = isEnabledFromConfig(currentFS))
    }
  }

  def getFSListFromConfig: List[FeatureSwitch] = {
    FeatureSwitchName.allFeatureSwitches.toList.map { fs =>
      FeatureSwitch(
        fs,
        isEnabled = appConfig.config.getOptional[Boolean](s"feature-switch.enable-${fs.name}").getOrElse(false)
      )
    }
  }
}
