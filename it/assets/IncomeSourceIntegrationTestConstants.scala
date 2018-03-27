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

package assets

import models.core.{AccountingPeriodModel, AddressModel, CessationModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsError, IncomeSourceDetailsModel, IncomeSourceDetailsResponse}
import assets.BaseIntegrationTestConstants._
import assets.BusinessDetailsIntegrationTestConstants._
import assets.PropertyDetailsIntegrationTestConstants._

object IncomeSourceIntegrationTestConstants {

  val singleBusinessResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    businesses = List(business1),
    property = None
  )

  val misalignedBusinessWithPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    businesses = List(business2),
    property = Some(property)
  )

  val multipleBusinessesResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    businesses = List(
      business1,
      business2
    ),
    property = None
  )

  val businessAndPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    businesses = List(business1),
    property = Some(property)
  )

  val multipleBusinessesAndPropertyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    businesses = List(
      business1,
      business2
    ),
    property = Some(property)
  )

  val propertyOnlyResponse: IncomeSourceDetailsResponse = IncomeSourceDetailsModel(
    businesses = List(),
    property = Some(property)
  )

  val errorResponse: IncomeSourceDetailsError = IncomeSourceDetailsError(500,"ISE")

}