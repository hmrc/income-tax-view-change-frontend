/*
 * Copyright 2026 HM Revenue & Customs
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

package models.liabilitycalculation

import play.api.libs.json._

sealed trait SubmissionChannel {
  def asString: String
}

case object IsMTD extends SubmissionChannel {
  val asString = "IsMTD"
}

case object IsLegacyWithCesa extends SubmissionChannel {
  val asString = "IsLegacyWithCesa"
}

case object IsLegacy extends SubmissionChannel {
  val asString = "IsLegacy"
}

case object UnableToDetermineSubmissionChannel extends SubmissionChannel {
  val asString = "UnableToDetermineSubmissionChannel"
}

object SubmissionChannel {

  val allSubmissionChannels: List[SubmissionChannel] =
    List(
      IsMTD,
      IsLegacyWithCesa,
      IsLegacy,
      UnableToDetermineSubmissionChannel
    )

  private val byString: Map[String, SubmissionChannel] =
    allSubmissionChannels.map(c => c.asString -> c).toMap

  implicit val reads: Reads[SubmissionChannel] =
    Reads {
      case JsString(value) =>
        byString
          .get(value)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Unknown SubmissionChannel: $value"))

      case _ =>
        JsError("SubmissionChannel must be a string")
    }

  implicit val writes: Writes[SubmissionChannel] =
    Writes(c => JsString(c.asString))
}
