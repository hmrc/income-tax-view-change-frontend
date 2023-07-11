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

sealed trait CreateIncomeSourcesRequest

case class CreateBusinessIncomeSourceRequest(businessDetails: List[BusinessDetails]) extends CreateIncomeSourcesRequest

case class CreateForeignPropertyIncomeSourceRequest(foreignPropertyDetails: PropertyDetails) extends CreateIncomeSourcesRequest

case class CreateUKPropertyIncomeSourceRequest(ukPropertyDetails: PropertyDetails) extends CreateIncomeSourcesRequest

case class BusinessDetails(accountingPeriodStartDate: String,
                           accountingPeriodEndDate: String,
                           tradingName: String,
                           addressDetails: AddressDetails,
                           typeOfBusiness: Option[String],
                           tradingStartDate: String,
                           cashOrAccrualsFlag: String,
                           cessationDate: Option[String] = None,
                           cessationReason: Option[String] = None)

case class AddressDetails(addressLine1: String,
                          addressLine2: Option[String],
                          addressLine3: Option[String],
                          addressLine4: Option[String],
                          countryCode: Option[String],
                          postalCode: Option[String])

case class PropertyDetails(tradingStartDate: String,
                           cashOrAccrualsFlag: String,
                           startDate: String,
                           cessationDate: Option[String] = None,
                           cessationReason: Option[String] = None)

object CreateBusinessIncomeSourceRequest {
  implicit val format: Format[CreateBusinessIncomeSourceRequest] = Json.format
}

object BusinessDetails {
  implicit val format: Format[BusinessDetails] = Json.format
}

object CreateForeignPropertyIncomeSourceRequest {
  implicit val format: Format[CreateForeignPropertyIncomeSourceRequest] = Json.format
}

object CreateUKPropertyIncomeSourceRequest {
  implicit val format: Format[CreateUKPropertyIncomeSourceRequest] = Json.format

}

object PropertyDetails {
  implicit val format: Format[PropertyDetails] = Json.format
}

object AddressDetails {
  implicit val format: Format[AddressDetails] = Json.format
}