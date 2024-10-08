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

package connectors.itsastatus

import models.core.ResponseModel.{AResponseReads, SuccessModel}
import play.api.libs.json.{Format, Json, OFormat}
import play.mvc.Http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}

object ITSAStatusUpdateConnectorModelHttpV2 {

  case class ITSAStatusBody(taxYear: String, updateReason: String)
  object ITSAStatusBody {
    implicit val format: OFormat[ITSAStatusBody] = Json.format[ITSAStatusBody]
  }

  trait ITSAStatusResponse {
    val statusCode: Int
  }
  case class ITSAStatusResponseSuccess(statusCode: Int = NO_CONTENT) extends SuccessModel with ITSAStatusResponse

  case class ErrorItem(code: Int, reason: String)
  case class ITSAStatusResponseFailure(override val statusCode: Int = INTERNAL_SERVER_ERROR,
                                       errorItems: List[ErrorItem] = List()) extends ITSAStatusResponse

  object ITSAStatusResponse {

    def withError(code: Int, reason: String): ITSAStatusResponseFailure = {
      ITSAStatusResponseFailure(statusCode = code, List(ErrorItem(code, reason)))
    }

    implicit val formatITSAStatusResponse: OFormat[ITSAStatusResponseSuccess] = Json.format[ITSAStatusResponseSuccess]
    implicit val readsITSAStatusResponse: ITSAStatusResponseReads = new ITSAStatusResponseReads
    class ITSAStatusResponseReads extends AResponseReads[ITSAStatusResponseSuccess] {
      implicit val format: Format[ITSAStatusResponseSuccess] = ITSAStatusResponse.formatITSAStatusResponse
    }

//    implicit val formatErrorItem: OFormat[ErrorItem] = Json.format[ErrorItem]
//    implicit val readsErrorItem: ErrorItemReads = new ErrorItemReads
//    class ErrorItemReads extends AResponseReads[ErrorItem] {
//      implicit val format: Format[ErrorItem] = ITSAStatusResponse.formatErrorItem
//    }
  }


}


//case class ErrorItem(code: String, reason: String)
//case class ITSAStatusResponseFailure(failures: List[ErrorItem]) extends ITSAStatusResponse()
//object ITSAStatusResponseFailure {
//  def defaultFailure(message: String = "unknown reason"): ITSAStatusResponseFailure =
//    ITSAStatusResponseFailure(
//      List(ErrorItem("INTERNAL_SERVER_ERROR", s"Request failed due to: $message"))
//    )
//  def defaultFailure(code: Int, reason: String): ITSAStatusResponseFailure =
//    ITSAStatusResponseFailure(
//      List(ErrorItem(code.toString, s"Request failed due to: $reason"))
//    )
//}

