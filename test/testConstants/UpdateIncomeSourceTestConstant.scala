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

package testConstants

import models.updateIncomeSource.{Cessation, TaxYearSpecific, UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.libs.json.{JsValue, Json}
import play.mvc.Http.Status
import testConstants.BaseTestConstants.testNino
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate

object UpdateIncomeSourceTestConstants {
  val incomeSourceId = "11111111111"
  val cessationDate = "2023-04-01"
  val request: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = incomeSourceId,
    cessation = Some(Cessation(true, Some(LocalDate.parse(cessationDate))))
  )

  val requestTaxYearSpecific: UpdateIncomeSourceRequestModel = UpdateIncomeSourceRequestModel(
    nino = testNino,
    incomeSourceId = incomeSourceId,
    taxYearSpecific = Some(List(TaxYearSpecific("2022", true)))
  )

  val requestTaxYearSpecificJson: JsValue = Json.obj(
    "nino" -> testNino,
    "incomeSourceId" -> incomeSourceId,
    "taxYearSpecific" -> Json.arr(Json.obj("taxYear" -> "2022", "latencyIndicator" -> true))
  )

  val requestJson: JsValue = Json.obj(
    "nino" -> testNino,
    "incomeSourceId" -> incomeSourceId,
    "cessation" -> Json.obj("cessationIndicator" -> true, "cessationDate" -> cessationDate)
  )

  val badRequest = request.copy(cessation = None)

  val errorBadResponse = UpdateIncomeSourceResponseError(Status.BAD_REQUEST, "Dummy Message")
  val failureResponse = UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, """"Error message"""")
  val badJsonResponse = UpdateIncomeSourceResponseError(Status.INTERNAL_SERVER_ERROR, "Json validation error parsing response")
  val successResponse = UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z")
  val successResponseJson = Json.obj("processingDate" -> "2022-01-31T09:26:17Z")

  val successHttpResponse = HttpResponse(Status.OK, Json.toJson(successResponse), Map.empty)
  val successInvalidJsonResponse = HttpResponse(Status.OK, Json.toJson(""), Map.empty)
  val badHttpResponse = HttpResponse(Status.BAD_REQUEST, "Dummy Message", Map.empty)


}
