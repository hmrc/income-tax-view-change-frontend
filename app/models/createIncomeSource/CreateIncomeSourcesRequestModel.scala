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

package models.createIncomeSource

import play.api.libs.json.{Format, Json}

sealed trait CreateIncomeSourceRequest


// *********************************************************************************************************************
// *                                                   Self-employment                                                 *
// *********************************************************************************************************************

final case class CreateBusinessIncomeSourceRequest(businessDetails: List[BusinessDetails]) extends CreateIncomeSourceRequest {
  require(businessDetails.length == 1, "Only single business can be created at a time")
}

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
                          countryCode: Option[String],
                          postalCode: Option[String]
                         )

object CreateBusinessIncomeSourceRequest {
  implicit val format: Format[CreateBusinessIncomeSourceRequest] = Json.format[CreateBusinessIncomeSourceRequest]
}

object BusinessDetails {
  implicit val format: Format[BusinessDetails] = Json.format[BusinessDetails]
}

object AddressDetails {
  implicit val format: Format[AddressDetails] = Json.format[AddressDetails]
}



// *********************************************************************************************************************
// *                                                   Property                                                        *
// *********************************************************************************************************************

final case class PropertyDetails(tradingStartDate: String, cashOrAccrualsFlag: String, startDate: String) extends CreateIncomeSourceRequest {
  require(cashOrAccrualsFlag.nonEmpty, "Accounting method must be provided")
  require(tradingStartDate.nonEmpty, "Trading start date must be provided")
  require(tradingStartDate == startDate, "Trading start date and start date must be the same")
}

final case class CreateForeignPropertyIncomeSource(foreignPropertyDetails: PropertyDetails)

final case class CreateUKPropertyIncomeSource(ukPropertyDetails: PropertyDetails)

object PropertyDetails {
  implicit val format: Format[PropertyDetails] = Json.format[PropertyDetails]
}

object CreateForeignPropertyIncomeSource {
  implicit val format: Format[CreateForeignPropertyIncomeSource] = Json.format[CreateForeignPropertyIncomeSource]
}

object CreateUKPropertyIncomeSource {
  implicit val format: Format[CreateUKPropertyIncomeSource] = Json.format[CreateUKPropertyIncomeSource]
}

