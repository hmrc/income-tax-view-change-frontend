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

package models.obligations

import play.api.libs.json.{Format, Json}

case class GroupedObligationsModel(identification: String, obligations: List[SingleObligationModel]) {

  val currentCrystDeadlines: List[SingleObligationModel] = obligations.filter(_.obligationType == "Crystallisation")
    .sortBy(_.start.toEpochDay)
}

object GroupedObligationsModel {
  implicit val format: Format[GroupedObligationsModel] = Json.format[GroupedObligationsModel]
}