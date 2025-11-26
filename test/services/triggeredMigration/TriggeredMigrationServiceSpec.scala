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

package services.triggeredMigration

import models.core.{CessationModel, IncomeSourceId}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetails, ukPropertyDetails}
import testUtils.TestSupport

import java.time.LocalDate

class TriggeredMigrationServiceSpec extends TestSupport {

  val service: TriggeredMigrationService = new TriggeredMigrationService()

  val baseIncomeSources = IncomeSourceDetailsModel(
    nino = "AA123456A",
    mtdbsa = "123456789012345",
    yearOfMigration = None,
    businesses = List.empty,
    properties = List.empty
  )

  "getCheckHmrcRecordsViewModel" should {
    "return a view model with an active sole trader businesses and active uk and foreign property businesses" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1),
        properties = List(ukPropertyDetails, foreignPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List(CheckHmrcRecordsSoleTraderDetails(IncomeSourceId("XA00001234"),Some("Fruit Ltd"), Some("nextUpdates.business"))),
        hasActiveUkProperty = true,
        hasActiveForeignProperty = true
      )

      result shouldBe expectedResult
    }

    "return a view model with no active sole trader businesses and no active uk or foreign property businesses due to cessation" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1.copy(cessation = Some(CessationModel(Some(LocalDate.now()))))),
        properties = List(
          ukPropertyDetails.copy(cessation = Some(CessationModel(Some(LocalDate.now())))),
          foreignPropertyDetails.copy(cessation = Some(CessationModel(Some(LocalDate.now()))))
        )
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = false,
        hasActiveForeignProperty = false
      )

      result shouldBe expectedResult
    }

    "return a view model with an active sole trader business and no active uk or foreign property businesses due to no returned businesses" in {
      val result = service.getCheckHmrcRecordsViewModel(baseIncomeSources)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = false,
        hasActiveForeignProperty = false
      )

      result shouldBe expectedResult
    }

    "return a view model with no active sole trader businesses and an active uk property business only" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1.copy(cessation = Some(CessationModel(Some(LocalDate.now()))))),
        properties = List(ukPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = true,
        hasActiveForeignProperty = false
      )

      result shouldBe expectedResult
    }

    "return a view model with no active sole trader businesses and an active foreign property business only" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1.copy(cessation = Some(CessationModel(Some(LocalDate.now()))))),
        properties = List(foreignPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = false,
        hasActiveForeignProperty = true
      )

      result shouldBe expectedResult
    }
  }
}
