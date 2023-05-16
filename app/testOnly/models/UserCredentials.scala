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

package testOnly.models

import testOnly.utils.{DelegatedEnrolmentValues, EnrolmentValues}
import utils.DelegatedEnrolmentValues

import scala.util.Random


case class UserCredentials(credId: String,
                           affinityGroup: String,
                           confidenceLevel: Int,
                           credentialStrength: String,
                           Role: String,
                           enrolmentData : EnrolmentValues,
                           delegatedEnrolmentData: Option[DelegatedEnrolmentValues])

object UserCredentials{
  def credId: String = Random.alphanumeric.take(16).mkString("")
}
