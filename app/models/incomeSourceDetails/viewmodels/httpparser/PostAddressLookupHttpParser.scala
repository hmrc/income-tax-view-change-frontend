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

package models.incomeSourceDetails.viewmodels.httpparser

import play.api.http.Status.ACCEPTED
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object PostAddressLookupHttpParser {

  type PostAddressLookupResponse = Either[PostAddressLookupFailure, PostAddressLookupSuccess]

  implicit def postAddressLookupHttpReads: HttpReads[PostAddressLookupResponse] =
    new HttpReads[PostAddressLookupResponse] {
      override def read(method: String, url: String, response: HttpResponse): PostAddressLookupResponse = {
        response.status match {
          case ACCEPTED => Right(
            if (response.header(key = "location").isEmpty) PostAddressLookupSuccessResponse(response.header(key = "Location"))
            else PostAddressLookupSuccessResponse(response.header(key = "location"))
          )
          case status => Left(UnexpectedPostStatusFailure(status))
        }
      }
    }

  sealed trait PostAddressLookupSuccess

  case class PostAddressLookupSuccessResponse(location: Option[String]) extends PostAddressLookupSuccess

  sealed trait PostAddressLookupFailure

  case class UnexpectedPostStatusFailure(status: Int) extends PostAddressLookupFailure

}
