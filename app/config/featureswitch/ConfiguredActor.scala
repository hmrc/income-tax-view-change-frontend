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

package config.featureswitch

import akka.actor.{Actor, Props}
import com.google.inject.Inject
import models.admin.AdjustPaymentsOnAccount
import services.admin.FeatureSwitchServiceImpl

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object ConfiguredActor {

  def props(featureSwitchService: FeatureSwitchServiceImpl)(implicit ec: ExecutionContext) =
    Props[ConfiguredActor](new ConfiguredActor(featureSwitchService = featureSwitchService))
  case object GetTimeMachineFS
  case object FetchFSValue
}

class ConfiguredActor @Inject()(featureSwitchService: FeatureSwitchServiceImpl)(implicit val ec: ExecutionContext) extends Actor {
  import ConfiguredActor._

  private val fsValue = new AtomicReference[Boolean](false)

  context.system.scheduler.scheduleWithFixedDelay(0.seconds, 5.seconds, self, FetchFSValue)(context.dispatcher)

  fetchValue()

  def receive = {
    case FetchFSValue =>
      fetchValue()
    case GetTimeMachineFS =>
      sender() ! fsValue
  }

  private def fetchValue(): Unit = {
    featureSwitchService.get(AdjustPaymentsOnAccount).map { value =>
      fsValue.set(value.isEnabled)
    }
  }

  def getFeatureSwitchValue: Boolean = fsValue.get()

}