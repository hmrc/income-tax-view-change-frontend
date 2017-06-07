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

case class BusinessListModel(
                              business: List[BusinessModel]
                              ) extends BusinessListResponseModel

case class BusinessModel(
                          id: String,
                          accountingPeriod: AccountingPeriod,
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

case class AccountingPeriod(
                            start: LocalDate,
                            end: LocalDate
                             )

case class BusinessListError(code: Int, message: String) extends BusinessListResponseModel

object AccountingPeriod {
  implicit val format = Json.format[AccountingPeriod]
}

object BusinessModel {
  implicit val format = Json.format[BusinessModel]
  implicit val apFormat = AccountingPeriod.format
}

object BusinessListModel {
  implicit val format = Json.format[BusinessListModel]
}

object BusinessListError {
  implicit val format = Json.format[BusinessListError]
}
