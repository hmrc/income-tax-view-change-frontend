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

package models.updateIncomeSource

import play.api.libs.json.{Format, Json}

sealed trait UpdateIncomeSourceResponse

case class UpdateIncomeSourceResponseModel(processingDate: String) extends UpdateIncomeSourceResponse

object UpdateIncomeSourceResponseModel {
  implicit val format: Format[UpdateIncomeSourceResponseModel] = Json.format
}

case class UpdateIncomeSourceResponseError(status: String, reason: String)

object UpdateIncomeSourceResponseError {
  implicit val format: Format[UpdateIncomeSourceResponseError] = Json.format
}

case class UpdateIncomeSourceListResponseError(failures: Seq[UpdateIncomeSourceResponseError]) extends UpdateIncomeSourceResponse

object UpdateIncomeSourceListResponseError {
  implicit val format: Format[UpdateIncomeSourceListResponseError] = Json.format
}