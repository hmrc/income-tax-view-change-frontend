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

package viewmodels.adjustPoa.checkAnswers

import play.api.libs.json._

import scala.util.Try

sealed trait SelectYourReason {
  val code: String
}

case object MainIncomeLower extends SelectYourReason {
  override val code: String = "001"
}
case object OtherIncomeLower extends SelectYourReason {
  override val code: String = "002"
}
case object AllowanceOrReliefHigher extends SelectYourReason {
  override val code: String = "003"
}
case object MoreTaxedAtSource extends SelectYourReason {
  override val code: String = "004"
}
case object Increase extends SelectYourReason {
  override val code: String = "005"
}

object SelectYourReason {

  private val codeMapping: Map[String, SelectYourReason] = Seq(
    MainIncomeLower,
    OtherIncomeLower,
    AllowanceOrReliefHigher,
    MoreTaxedAtSource,
    Increase
  ).map(reason => (reason.code -> reason))
    .toMap

  implicit val format: Format[SelectYourReason] = Format(
    Reads {
      case JsString(value) if codeMapping.contains(value) => JsResult.fromTry(Try(codeMapping(value)))
      case value => JsError(s"Could not parse SelectYourReason from value: $value")
    },
    Writes[SelectYourReason] { value =>
      JsString(value.code)
    }
  )
}