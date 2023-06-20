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

import models.incomeSourceDetails.BusinessAddressModel
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.HttpReads

object GetAddressLookupDetailsHttpParser {

  type GetAddressLookupDetailsResponse = Either[GetAddressLookupDetailsFailure, Option[BusinessAddressModel]]

  implicit def getAddressLookupDetailsHttpReads: HttpReads[GetAddressLookupDetailsResponse] = HttpReads { (_, _, response) =>
    response.status match {
      case OK => response.json.validate[BusinessAddressModel] match {
        case JsSuccess(value, _) => Right(Some(value))
        case _ => Left(InvalidJson)
      }
      case NOT_FOUND => Right(None)
      case status => Left(UnexpectedGetStatusFailure(status))
    }
  }

  sealed trait GetAddressLookupDetailsFailure

  case object InvalidJson extends GetAddressLookupDetailsFailure

  case class UnexpectedGetStatusFailure(status: Int) extends GetAddressLookupDetailsFailure

}
