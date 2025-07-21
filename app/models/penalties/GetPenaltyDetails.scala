/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties

import models.penalties.breathingSpace.BreathingSpace
import models.penalties.latePayment.LatePaymentPenalty
import models.penalties.lateSubmission.LateSubmissionPenalty
import play.api.http.Status._
import play.api.libs.Files.logger
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

case class GetPenaltyDetails(totalisations: Option[Totalisations],
                             lateSubmissionPenalty: Option[LateSubmissionPenalty],
                             latePaymentPenalty: Option[LatePaymentPenalty],
                             breathingSpace: Option[BreathingSpace])

object GetPenaltyDetails {
  implicit val format: Format[GetPenaltyDetails] = Json.format[GetPenaltyDetails]
}

object GetPenaltyDetailsParser {

  sealed trait GetPenaltyDetailsFailure

  sealed trait GetPenaltyDetailsSuccess

  case class GetPenaltyDetailsSuccessResponse(penaltyDetails: GetPenaltyDetails) extends GetPenaltyDetailsSuccess

  case class GetPenaltyDetailsFailureResponse(status: Int) extends GetPenaltyDetailsFailure

  case object GetPenaltyDetailsMalformed extends GetPenaltyDetailsFailure

  type GetPenaltyDetailsResponse = Either[GetPenaltyDetailsFailure, GetPenaltyDetailsSuccessResponse]

  implicit object GetPenaltyDetailsReads extends HttpReads[GetPenaltyDetailsResponse] {
    override def read(method: String, url: String, response: HttpResponse): GetPenaltyDetailsResponse = {
      response.status match {
        case OK =>
          logger.debug(s"[GetPenaltyDetailsReads][read] Json response: ${response.json}")
          response.json.validate[GetPenaltyDetails] match {
            case JsSuccess(getPenaltyDetails, _) =>
              logger.debug(s"[GetPenaltyDetailsReads][read] Model: $getPenaltyDetails")
              Right(GetPenaltyDetailsSuccessResponse(getPenaltyDetails))
            case JsError(errors) =>
              logger.debug(s"[GetPenaltyDetailsReads][read] Json validation errors: $errors")
              Left(GetPenaltyDetailsMalformed)
          }
        case status@(BAD_REQUEST | CONFLICT | INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE) => {
          logger.error(s"[GetPenaltyDetailsReads][read] Received $status when trying to call GetPenaltyDetails - with body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
        }
        //Status of NOT FOUND is currently the default return value for penalties,
        //but code to implement a NO_CONTENT response is also being implemented so both included until migration to HIP is completed and a single return finalised
        case status@(NO_CONTENT | NOT_FOUND) => {
          logger.debug(s"[GetPenaltyDetailsReads][read] Received $status when calling penalties, returning empty data response")
          Right(GetPenaltyDetailsSuccessResponse(GetPenaltyDetails(None, None, None, None)))
        }
        case status@UNPROCESSABLE_ENTITY => {
          logger.error(s"[GetPenaltyDetailsReads][read] Received 422 when trying to call GetPenaltyDetails - with body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
        }
        case _@status =>
          logger.error(s"[GetPenaltyDetailsReads][read] Received unexpected response from GetPenaltyDetails, status code: $status and body: ${response.body}")
          Left(GetPenaltyDetailsFailureResponse(status))
      }
    }
  }
}
