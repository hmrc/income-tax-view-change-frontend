/*
 * Copyright 2022 HM Revenue & Customs
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

import actors.CalculationPoolingActor.GetCalculationResponseMsg

import javax.inject.Inject
import akka.actor._

import javax.inject._


//class CalculationPoolingActor {
object CalculationPoolingActor {
  case class GetCalculationResponseMsg(calcId: String)
  case class CalculationResponseSuccess(status: String)
  case class CalculationResponseFailed(error: String)
}

/*
  This actor will incorporate state
  state would be represented by lock status: locked/not locked

  Doc ref: https://www.playframework.com/documentation/2.8.x/ScalaAkka
 */
class CalculationPoolingActor @Inject() extends Actor {

  // TODO: move logic from CalculationPollingService.scala

  //val config = configuration.getOptional[String]("my.config").getOrElse("none")

  def receive = {
    case GetCalculationResponseMsg(calcId: String) =>
      //sender() ! config
  }

}
