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

package common.models.itsaStatus

import play.api.libs.json.{Format, Json}

sealed trait ITSAStatusResponse

case class ITSAStatusResponseModel(taxYear: String,
                                   itsaStatusDetails: Option[List[StatusDetail]] = None) extends ITSAStatusResponse

case class ITSAStatusResponseError(status: Int, reason: String) extends ITSAStatusResponse

case class ITSAStatusYearOfMigrationModel(yearOfMigrationEndYear: Option[String]) extends ITSAStatusResponse

object ITSAStatusResponseModel {
  given format: Format[ITSAStatusResponseModel] = Json.format[ITSAStatusResponseModel]
}

object ITSAStatusResponseError {
  given format: Format[ITSAStatusResponseError] = Json.format[ITSAStatusResponseError]
}

object ITSAStatusYearOfMigrationModel {
  given format: Format[ITSAStatusYearOfMigrationModel] = Json.format[ITSAStatusYearOfMigrationModel]
}