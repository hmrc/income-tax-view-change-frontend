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

package models.citizenDetails

import models.readNullable
import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OWrites, Reads, _}

sealed trait CitizenDetailsResponseModel

case class CitizenDetailsModel(firstName: Option[String],
                               lastName: Option[String],
                               nino: Option[String]) extends CitizenDetailsResponseModel


case class CitizenDetailsErrorModel(code: Int, message: String) extends CitizenDetailsResponseModel

object CitizenDetailsErrorModel {
  implicit val format: Format[CitizenDetailsErrorModel] = Json.format[CitizenDetailsErrorModel]
}

object CitizenDetailsModel {
  implicit val reads: Reads[CitizenDetailsModel] = (
    readNullable[String](__ \ "name" \ "current" \ "firstName") and
      readNullable[String](__ \ "name" \ "current" \ "lastName") and
      readNullable[String](__ \ "ids" \ "nino")
    ) (CitizenDetailsModel.apply _)
  implicit val writes: OWrites[CitizenDetailsModel] = Json.writes[CitizenDetailsModel]
}
