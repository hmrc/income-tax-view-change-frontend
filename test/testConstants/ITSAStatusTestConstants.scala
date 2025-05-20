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

import models.incomeSourceDetails.TaxYear
import models.itsaStatus._
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse


object ITSAStatusTestConstants {
  val statusDetail = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.NoStatus, StatusReason.SignupReturnAvailable, Some(8000.25))
  val statusDetailMTDMandated = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Mandated, StatusReason.SignupReturnAvailable, Some(8000.25))
  val statusDetailMinimal = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.NoStatus, StatusReason.SignupReturnAvailable, None)
  val successITSAStatusResponseModel = ITSAStatusResponseModel("2019-20", Some(List(statusDetail)))
  val successITSAStatusResponseMTDMandatedModel = ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandated)))
  val successMultipleYearITSAStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetail))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMTDMandated)))
    )
  }
  val successMultipleYearITSAStatusWithUnknownResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetail))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetail))),
      ITSAStatusResponseModel("2021-22", Some(List(statusDetail)))
    )
  }
  val successITSAStatusResponseModelMinimal = ITSAStatusResponseModel("2019-20", None)
  val errorITSAStatusError = ITSAStatusResponseError(BAD_REQUEST, "Dummy message")
  val badJsonErrorITSAStatusError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing itsa-status response")
  val failedFutureITSAStatusError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, s"Unexpected failed future, error")

  val successMultipleYearMandatedITSAStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandated))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMTDMandated)))
    )
  }

  val currentYearMandatedPreviousYearNoStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailMinimal))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMTDMandated)))
    )
  }

  val previousYearMandatedCurrentYearNoStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandated))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMinimal)))
    )
  }

  val bothYearsNoStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetail))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetail)))
    )
  }

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


  val yearToStatus: Map[TaxYear, StatusDetail] = Map(TaxYear.forYearEnd(2020) -> statusDetail, TaxYear.forYearEnd(2021) -> statusDetailMTDMandated)
  val yearToUnknownStatus: Map[TaxYear, StatusDetail] = Map(TaxYear.forYearEnd(2020) -> statusDetail, TaxYear.forYearEnd(2021) -> statusDetail, TaxYear.forYearEnd(2022) -> statusDetail)

}