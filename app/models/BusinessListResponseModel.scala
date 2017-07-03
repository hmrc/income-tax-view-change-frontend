/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import java.time.LocalDate

import play.api.libs.json.Json


sealed trait BusinessListResponseModel
case class BusinessDetailsModel(business: List[BusinessModel]) extends BusinessListResponseModel
case class BusinessDetailsErrorModel(code: Int, message: String) extends BusinessListResponseModel

case class BusinessModel(
                          id: String,
                          accountingPeriod: AccountingPeriodModel,
                          accountingType: String,
                          commencementDate: Option[LocalDate],
                          cessationDate: Option[LocalDate],
                          tradingName: String,
                          businessDescription: Option[String],
                          businessAddressLineOne: Option[String],
                          businessAddressLineTwo: Option[String],
                          businessAddressLineThree: Option[String],
                          businessAddressLineFour: Option[String],
                          businessPostcode: Option[String]
                          )


object BusinessModel {
  implicit val format = Json.format[BusinessModel]
}

object BusinessDetailsErrorModel {
  implicit val format = Json.format[BusinessDetailsErrorModel]
}

object BusinessDetailsModel {
  implicit val format = Json.format[BusinessDetailsModel]
}
