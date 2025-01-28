/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties.appealInformation

import play.api.libs.json._

object AppealStatusEnum extends Enumeration {

  val Under_Appeal: AppealStatusEnum.Value = Value("A")
  val Upheld: AppealStatusEnum.Value = Value("B")
  val Rejected: AppealStatusEnum.Value = Value("C")
  val Unappealable: AppealStatusEnum.Value = Value("99")
  val AppealRejectedChargeAlreadyReversed: AppealStatusEnum.Value = Value("91")
  val AppealUpheldPointAlreadyRemoved: AppealStatusEnum.Value = Value("92")
  val AppealUpheldChargeAlreadyReversed: AppealStatusEnum.Value = Value("93")
  val AppealRejectedPointAlreadyRemoved: AppealStatusEnum.Value = Value("94")

  implicit val format: Format[AppealStatusEnum.Value] = new Format[AppealStatusEnum.Value] {

    override def writes(o: AppealStatusEnum.Value): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[AppealStatusEnum.Value] = json.as[String].toUpperCase match {
      case "A" => JsSuccess(Under_Appeal)
      case "B" => JsSuccess(Upheld)
      case "C" => JsSuccess(Rejected)
      case "99" => JsSuccess(Unappealable)
      case "91" => JsSuccess(AppealRejectedChargeAlreadyReversed)
      case "92" => JsSuccess(AppealUpheldPointAlreadyRemoved)
      case "93" => JsSuccess(AppealUpheldChargeAlreadyReversed)
      case "94" => JsSuccess(AppealRejectedPointAlreadyRemoved)
      case e => JsError(s"$e not recognised")
    }
  }

}
