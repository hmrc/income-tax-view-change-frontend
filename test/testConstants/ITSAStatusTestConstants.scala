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
  val statusDetailNotRollover = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.NoStatus, StatusReason.SignupReturnAvailable, Some(8000.25))
  val statusDetailRollover = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.NoStatus, StatusReason.Rollover, Some(8000.25))
  val statusDetailMTDMandatedAndReasonNotRollover = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Mandated, StatusReason.SignupReturnAvailable, Some(8000.25))
  val statusDetailMTDMandatedAndReasonRollover = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Mandated, StatusReason.Rollover, Some(8000.25))
  val statusDetailAnnualAndReasonRollover = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Annual, StatusReason.Rollover, Some(8000.25))
  val statusDetailAnnualAndReasonNotRollover = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.Annual, StatusReason.SignupReturnAvailable, Some(8000.25))
  val statusDetailMinimal = StatusDetail("2023-06-15T15:38:33.960Z", ITSAStatus.NoStatus, StatusReason.SignupReturnAvailable, None)
  val successITSAStatusResponseModel = ITSAStatusResponseModel("2019-20", Some(List(statusDetailNotRollover)))
  val successITSAStatusResponseModelWithRollover = ITSAStatusResponseModel("2019-20", Some(List(statusDetailRollover)))
  val successITSAStatusResponseMTDMandatedModel = ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandatedAndReasonNotRollover)))
  val successITSAStatusResponseMTDMandatedModelRollover = ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandatedAndReasonRollover)))
  val successITSAStatusResponseAnnualModelRollover = ITSAStatusResponseModel("2019-20", Some(List(statusDetailAnnualAndReasonRollover)))
  val successITSAStatusResponseAnnualModelNotRollover = ITSAStatusResponseModel("2019-20", Some(List(statusDetailAnnualAndReasonNotRollover)))
  val successMultipleYearITSAStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailNotRollover))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMTDMandatedAndReasonNotRollover)))
    )
  }
  val successMultipleYearITSAStatusWithUnknownResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailNotRollover))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailNotRollover))),
      ITSAStatusResponseModel("2021-22", Some(List(statusDetailNotRollover)))
    )
  }
  val successITSAStatusResponseModelMinimal = ITSAStatusResponseModel("2019-20", None)
  val errorITSAStatusError = ITSAStatusResponseError(BAD_REQUEST, "Dummy message")
  val badJsonErrorITSAStatusError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, "Json validation error parsing itsa-status response")
  val failedFutureITSAStatusError = ITSAStatusResponseError(INTERNAL_SERVER_ERROR, s"Unexpected failed future, error")

  val successMultipleYearMandatedITSAStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandatedAndReasonNotRollover))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMTDMandatedAndReasonNotRollover)))
    )
  }

  val currentYearMandatedPreviousYearNoStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailMinimal))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMTDMandatedAndReasonNotRollover)))
    )
  }

  val previousYearMandatedCurrentYearNoStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailMTDMandatedAndReasonNotRollover))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailMinimal)))
    )
  }

  val bothYearsNoStatusResponse = {
    List(
      ITSAStatusResponseModel("2019-20", Some(List(statusDetailNotRollover))),
      ITSAStatusResponseModel("2020-21", Some(List(statusDetailNotRollover)))
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


  val yearToStatus: Map[TaxYear, StatusDetail] = Map(TaxYear.forYearEnd(2020) -> statusDetailNotRollover, TaxYear.forYearEnd(2021) -> statusDetailMTDMandatedAndReasonNotRollover)
  val yearToUnknownStatus: Map[TaxYear, StatusDetail] = Map(TaxYear.forYearEnd(2020) -> statusDetailNotRollover, TaxYear.forYearEnd(2021) -> statusDetailNotRollover, TaxYear.forYearEnd(2022) -> statusDetailNotRollover)

}