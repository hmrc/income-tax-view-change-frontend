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

package actors

import models.admin.{FeatureSwitch, FeatureSwitchName}
import org.apache.pekko.actor.{Actor, Props}

object FeatureSwitchActor {
  def props = Props[FeatureSwitchActor]()
  case class GetFs(fs: FeatureSwitchName)
  case class SetFs(fs: FeatureSwitch)
}

class FeatureSwitchActor extends Actor {
  private var fss: Map[FeatureSwitchName, FeatureSwitch] = Map()

  import actors.FeatureSwitchActor._

  def receive = {
    case GetFs(fsn: FeatureSwitchName) =>
      if (fss.contains(fsn))
        sender() ! fss.get(fsn).get
      else
        sender() ! FeatureSwitch( fsn, false)
    case SetFs(fsn: FeatureSwitch) =>
      fss += (fsn.name -> fsn)
      sender() ! true
  }


  //await( featureSwitchService.set(IncomeSources, true) )

}
