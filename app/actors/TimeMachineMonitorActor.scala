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

import com.typesafe.config.ConfigFactory
import org.apache.pekko.actor.{Actor, Props}
import play.api.{Configuration, Logger}
import repositories.admin.FeatureSwitchRepository

object TimeMachineMonitorActor {
  def props(config: Configuration, featureSwitchRepository: FeatureSwitchRepository): Props =
    Props[TimeMachineMonitorActor](new TimeMachineMonitorActor(config, featureSwitchRepository))
  private case object Realod
}

// TODO: avoid using classical Actors / use Typed version instead
class TimeMachineMonitorActor(val config: Configuration) extends Actor  {
  import TimeMachineMonitorActor._
  import scala.concurrent.duration._

  context.system.scheduler.scheduleOnce(5.seconds, self, Realod)(context.dispatcher)

  def receive = {
    case Realod  =>
      Logger("application").info("TimeMachineMonitorActor -> reload")

      // TODO: call FeatureSwitch Service
      System.setProperty("feature-switch.enable-time-machine", "true")
      ConfigFactory.invalidateCaches()
      val cfg = ConfigFactory.load(config.underlying)
      val newConfig = cfg.getConfig("feature-switch")
      val fsTimeMachine = newConfig.getString("enable-time-machine")
      Logger("application").info(s"TimeMachineMonitorActor -> $fsTimeMachine")



      context.system.scheduler.scheduleOnce(5.seconds, self, Realod)(context.dispatcher)
  }

  /*

System.setProperty("a.b.c", "100")
ConfigFactory.invalidateCaches()
val config = ConfigFactory.load("sample1")
config.getDouble("a.b.c") should be (100.0)

   */
}