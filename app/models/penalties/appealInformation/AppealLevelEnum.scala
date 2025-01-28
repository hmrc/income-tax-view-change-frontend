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

object AppealLevelEnum extends Enumeration {

  val HMRC: AppealLevelEnum.Value = Value("01")
  val Tribunal: AppealLevelEnum.Value = Value("02")

  implicit val format: Format[AppealLevelEnum.Value] = new Format[AppealLevelEnum.Value] {

    override def writes(o: AppealLevelEnum.Value): JsValue = JsString(o.toString)

    override def reads(json: JsValue): JsResult[AppealLevelEnum.Value] = json.as[String].toUpperCase match {
      case "01" => JsSuccess(HMRC)
      case "02" => JsSuccess(Tribunal)
      case e => JsError(s"$e not recognised")
    }
  }

}
