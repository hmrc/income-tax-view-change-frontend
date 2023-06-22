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

package models.addIncomeSource

import play.api.libs.json.{Format, Json}

import java.time.LocalDate

sealed trait AddIncomeSourceRequest

case class AddBusinessIncomeSourcesRequest(businessDetails: Option[List[BusinessDetails]]) extends AddIncomeSourceRequest


case class BusinessDetails(accountingPeriodStartDate: String,
                           accountingPeriodEndDate: String,
                           tradingName: String,
                           addressDetails: AddressDetails,
                           typeOfBusiness: Option[String],
                           tradingStartDate: String,
                           cashOrAccrualsFlag: String,
                           cessationDate: Option[String],
                           cessationReason: Option[String]
                          )

case class AddressDetails(addressLine1: String,
                          addressLine2: Option[String],
                          addressLine3: Option[String],
                          addressLine4: Option[String],
                          countryCode: String,
                          postalCode: String
                         )

object AddBusinessIncomeSourcesRequest {
  implicit val format: Format[AddBusinessIncomeSourcesRequest] = Json.format
}

object BusinessDetails {
  implicit val format: Format[BusinessDetails] = Json.format
}

object AddressDetails {
  implicit val format: Format[AddressDetails] = Json.format
}