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

case class AppealInformationType(
                                  appealStatus: Option[AppealStatusEnum.Value],
                                  appealLevel: Option[AppealLevelEnum.Value],
                                  appealDescription: Option[String]
                                )

object AppealInformationType {
  implicit val reads: Reads[AppealInformationType] = Json.reads[AppealInformationType]

  implicit val writes: Writes[AppealInformationType] = new Writes[AppealInformationType] {
    override def writes(appealInformation: AppealInformationType): JsValue = {
      val newAppealLevel: Option[AppealLevelEnum.Value] = parseAppealLevel(appealInformation)
      Json.obj(
        "appealStatus" -> appealInformation.appealStatus,
        "appealLevel" -> newAppealLevel,
        "appealDescription" -> appealInformation.appealDescription
      )
    }
  }

  private def parseAppealLevel(appealInformation: AppealInformationType): Option[AppealLevelEnum.Value] = {
    if (appealInformation.appealLevel.isEmpty
      && appealInformation.appealStatus.contains(AppealStatusEnum.Unappealable)) {
      Some(AppealLevelEnum.HMRC)
    } else {
      appealInformation.appealLevel
    }
  }
}