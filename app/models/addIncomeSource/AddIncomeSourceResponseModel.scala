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

package models.addIncomeSource

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

sealed trait AddIncomeSourceResponse


case class IncomeSource(incomeSourceId: String)

object IncomeSource {
  implicit val format: Format[IncomeSource] = Json.format
}

case class SingleError(code: String, reason: String)

case class MultipleErrors(failures: List[SingleError])

case class FailedSingleErrorResponse(
                                      _id: String,
                                      schemaId: String,
                                      method: String,
                                      status: Int,
                                      response: SingleError) extends  AddIncomeSourceResponse

case class FailedMultiErrorResponse(
                                     _id: String,
                                     schemaId: String,
                                     method: String,
                                     status: Int,
                                     response: MultipleErrors) extends  AddIncomeSourceResponse


case class AddIncomeSourceResponseError(status: Int, reason: String) extends AddIncomeSourceResponse

object AddIncomeSourceResponseError {
  implicit val format: Format[AddIncomeSourceResponseError] = Json.format
}

object SingleError {
  implicit val format: Format[SingleError] = Json.format
}

object MultipleErrors {
  implicit val format: Format[MultipleErrors] = Json.format
}

object FailedSingleErrorResponse {
  implicit val format: Format[FailedSingleErrorResponse] = Json.format
}

object FailedMultiErrorResponse {
  implicit val format: Format[FailedMultiErrorResponse] = Json.format
}