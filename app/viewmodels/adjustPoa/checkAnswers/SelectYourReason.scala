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

import play.api.libs.json.{JsPath, JsString, Reads, Writes}

sealed trait SelectYourReason

case object MainIncomeLower extends SelectYourReason
case object OtherIncomeLower extends SelectYourReason
case object AllowanceOrReliefHigher extends SelectYourReason
case object MoreTaxedAtSource extends SelectYourReason

object SelectYourReason {

  implicit val reads: Reads[SelectYourReason] = JsPath.read[String].map {
    case "MainIncomeLower" => MainIncomeLower
    case "OtherIncomeLower" => OtherIncomeLower
    case "AllowanceOrReliefHigher" => AllowanceOrReliefHigher
    case "MoreTaxedAtSource" => MoreTaxedAtSource
  }

  implicit val writes: Writes[SelectYourReason] = Writes[SelectYourReason](reason => JsString(reason.toString))
}

