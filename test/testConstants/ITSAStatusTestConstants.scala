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

import models.itsaStatus._
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse


object ITSAStatusTestConstants {
  val statusDetail = StatusDetail("2023-06-15T15:38:33.960Z", "No Status", "Sign up - return available", Some(8000.25))
  val statusDetailMinimal = StatusDetail("2023-06-15T15:38:33.960Z", "No Status", "Sign up - return available", None)
  val successITSAStatusResponseModel = ITSAStatusResponseModel("2019-20", Some(List(statusDetail)))
  val successITSAStatusResponseModelMinimal = ITSAStatusResponseModel("2019-20", None)
  val errorITSAStatusError = ITSAStatusResponseError(BAD_REQUEST, "Dummy message")
  val badJsonErrorITSAStatusError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing ITSA Status response")
  val failedFutureITSAStatusError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, s"Unexpected failed future, error")


  val statusDetailMinimalJson = Json.parse(
    """
      |{
      | "submittedOn": "2023-06-15T15:38:33.960Z",
      | "status": "No Status",
      | "statusReason": "Sign up - return available"
      |}
      |""".stripMargin)

  val successITSAStatusResponseModelMinimalJson = Json.parse(
    """
      |{
      | "taxYear": "2019-20"
      |}
      |""".stripMargin
  )
  val successITSAStatusResponseJson = Json.parse(
    """
      |{
      |    "taxYear": "2019-20",
      |    "itsaStatusDetails": [
      |      {
      |        "submittedOn": "2023-06-15T15:38:33.960Z",
      |        "status": "No Status",
      |        "statusReason": "Sign up - return available",
      |        "businessIncomePriorTo2Years": 8000.25
      |      }
      |    ]
      |  }
      |""".stripMargin)

  val successITSAStatusListResponseJson = Json.parse(
    """
      |[{
      |    "taxYear": "2019-20",
      |    "itsaStatusDetails": [
      |      {
      |        "submittedOn": "2023-06-15T15:38:33.960Z",
      |        "status": "No Status",
      |        "statusReason": "Sign up - return available",
      |        "businessIncomePriorTo2Years": 8000.25
      |      }
      |    ]
      |  }]
      |""".stripMargin)

  val successHttpResponse = HttpResponse(Status.OK, Json.arr(successITSAStatusResponseJson), Map.empty)
  val errorHttpResponse = HttpResponse(Status.BAD_REQUEST, "Dummy message", Map.empty)
  val notFoundHttpResponse = HttpResponse(Status.NOT_FOUND, "Dummy message", Map.empty)
  val badJsonHttpResponse = HttpResponse(Status.OK, Json.obj(), Map.empty)
}
