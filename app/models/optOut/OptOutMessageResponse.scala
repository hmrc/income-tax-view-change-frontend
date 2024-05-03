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

package models.optOut

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.StatusDetail
import play.api.libs.json.{Format, Json}

case class YearStatusDetail(taxYear: TaxYear, statusDetail: StatusDetail)
case class OptOutMessageResponse(oneYearOptOut: Boolean = false, taxYears: Array[TaxYear] = Array()) {
  def oneYearOptOutTaxYear: TaxYear = taxYears(0)
}


case class OptOutUpdateReason(code: Int) {
  private val validCodes = List(10)
  assert(validCodes.contains(code))
}
case class OptOutApiCallRequest(taxYear: String, updateReason: Int = 10)
object OptOutApiCallRequest {
  implicit val format: Format[OptOutApiCallRequest] = Json.format[OptOutApiCallRequest]
}

sealed trait OptOutApiCallResponse
case class OptOutApiCallSuccessfulResponse() extends OptOutApiCallResponse
case class OptOutApiCallFailureResponse(message: String, code: Int = 500) extends OptOutApiCallResponse

object OptOutApiCallResponse {
  implicit val formatSuccess: Format[OptOutApiCallSuccessfulResponse] = Json.format[OptOutApiCallSuccessfulResponse]
  implicit val formatFailure: Format[OptOutApiCallFailureResponse] = Json.format[OptOutApiCallFailureResponse]
}