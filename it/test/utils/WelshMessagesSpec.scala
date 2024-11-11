/*
 * Copyright 2017 HM Revenue & Customs
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

package utils

import helpers.ComponentSpecBase

class WelshMessagesSpec extends ComponentSpecBase {

  "all english messages should have a welsh translation" in {
    val realMessages = messagesAPI.messages
    val englishMessages = realMessages("default")
    val welshMessages = realMessages("cy")

    // Only to be used whenever welsh isn't available yet. Remove from list when they are added.
    // TODO: Remove once opt in welsh is created
    val exclusions = Seq(
//      "optin.completedOptIn.nymandated.futureReporting.bullet.p2",
//      "optin.completedOptIn.followingVoluntary.heading.desc",
//      "optin.completedOptIn.nymandated.inset",
//      "optin.completedOptIn.nymandated.futureReporting",
//      "optin.completedOptIn.nymandated.futureReporting.desc1",
//      "optin.completedOptIn.nymandated.futureReporting.desc2",
//      "optin.completedOptIn.nymandated.futureReporting.bullet.p1"
    )

    val missingWelshKeys = englishMessages.keySet.filterNot(key => welshMessages.keySet(key) || exclusions.contains(key))

    if (missingWelshKeys.nonEmpty) {
      val failureText = missingWelshKeys.foldLeft(s"There are ${missingWelshKeys.size} missing Welsh translations:") {
        case (failureString, key) =>
          failureString + s"\n$key:${englishMessages(key)}"
      }

      fail(failureText)
    }
  }

}

