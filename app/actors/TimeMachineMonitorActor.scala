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
import models.admin.TimeMachine
import org.apache.pekko.actor.{Actor, Props}
import org.apache.pekko.pattern.pipe
import play.api.{Configuration, Logger}
import repositories.admin.FeatureSwitchRepository

import scala.concurrent.Future

object TimeMachineMonitorActor {
  def props(config: Configuration, featureSwitchRepository: FeatureSwitchRepository): Props =
    Props[TimeMachineMonitorActor](new TimeMachineMonitorActor(config, featureSwitchRepository))

  private case object Realod

  private case object WaitForNew
}

// TODO: avoid using classical Actors / use Typed version instead
class TimeMachineMonitorActor(val config: Configuration,
                              val featureSwitchRepository: FeatureSwitchRepository) extends Actor {

  import TimeMachineMonitorActor._
  import scala.concurrent.duration._

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  context.system.scheduler.scheduleOnce(5.seconds, self, Realod)(context.dispatcher)

  def getTimeMachineFs(): Future[Unit] = {

    featureSwitchRepository
      .getFeatureSwitch(TimeMachine)
      .flatMap { res =>
        Logger("application").info(s"TimeMachineMonitorActor -> response from FeatureSwitchRepository ${res}")
        if (res.map(_.isEnabled).getOrElse(false)) {
          Logger("application").info("TimeMachineMonitorActor -> attempt to set to True")
          System.setProperty("feature-switch.enable-time-machine", "true")
          ConfigFactory.invalidateCaches()
          val cfg = ConfigFactory.load(config.underlying)
          val newConfig = cfg.getConfig("feature-switch")
          val fsTimeMachine = newConfig.getString("enable-time-machine")
          Logger("application").info(s"TimeMachineMonitorActor -> $fsTimeMachine")
        } else {
          Logger("application").info("TimeMachineMonitorActor -> is False in mongo")
        }
        Future.successful(())
      }
  }

  def receive = {
    case WaitForNew =>
      Logger("application").info("TimeMachineMonitorActor -> waitForNew")
      context.system.scheduler.scheduleOnce(5.seconds, self, Realod)(context.dispatcher)
    case Realod =>
      Logger("application").info("TimeMachineMonitorActor -> reload")
      getTimeMachineFs()
        .map(_ => WaitForNew).pipeTo(self)

    // TODO: call FeatureSwitch Service
    //      System.setProperty("feature-switch.enable-time-machine", "true")
    //      ConfigFactory.invalidateCaches()
    //      val cfg = ConfigFactory.load(config.underlying)
    //      val newConfig = cfg.getConfig("feature-switch")
    //      val fsTimeMachine = newConfig.getString("enable-time-machine")
    //      Logger("application").info(s"TimeMachineMonitorActor -> $fsTimeMachine")


  }

}