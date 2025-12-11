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

import enums.IncomeSourceJourney.SelfEmployment
import enums.TriggeredMigration.{TriggeredMigrationAdded, TriggeredMigrationCeased}
import models.core.{CessationModel, IncomeSourceId}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.triggeredMigration.viewModels.{CheckHmrcRecordsSoleTraderDetails, CheckHmrcRecordsViewModel}
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.PropertyDetailsTestConstants.{foreignPropertyDetails, ukPropertyDetails}
import testUtils.TestSupport

import java.time.LocalDate

class TriggeredMigrationServiceSpec extends TestSupport {

  val service: TriggeredMigrationService = new TriggeredMigrationService()

  val baseIncomeSources: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
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

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources, None)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List(CheckHmrcRecordsSoleTraderDetails(IncomeSourceId("XA00001234"),Some("Fruit Ltd"), Some("nextUpdates.business"))),
        hasActiveUkProperty = true,
        hasActiveForeignProperty = true,
        triggeredMigrationState = None
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

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources, None)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = false,
        hasActiveForeignProperty = false,
        triggeredMigrationState = None,
        numberOfCeasedBusinesses = 1
      )

      result shouldBe expectedResult
    }

    "return a view model with an active sole trader business and no active uk or foreign property businesses due to no returned businesses" in {
      val result = service.getCheckHmrcRecordsViewModel(baseIncomeSources, None)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = false,
        hasActiveForeignProperty = false,
        triggeredMigrationState = None
      )

      result shouldBe expectedResult
    }

    "return a view model with no active sole trader businesses and an active uk property business only" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1.copy(cessation = Some(CessationModel(Some(LocalDate.now()))))),
        properties = List(ukPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources, None)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = true,
        hasActiveForeignProperty = false,
        triggeredMigrationState = None,
        numberOfCeasedBusinesses = 1
      )

      result shouldBe expectedResult
    }

    "return a view model with an active sole trader businesses and active uk and foreign property businesses and a state of ceased" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1),
        properties = List(ukPropertyDetails, foreignPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources, Some(TriggeredMigrationCeased))

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List(CheckHmrcRecordsSoleTraderDetails(IncomeSourceId("XA00001234"),Some("Fruit Ltd"), Some("nextUpdates.business"))),
        hasActiveUkProperty = true,
        hasActiveForeignProperty = true,
        triggeredMigrationState = Some(TriggeredMigrationCeased)
      )

      result shouldBe expectedResult
    }

    "return a view model with an active sole trader businesses and active uk and foreign property businesses and a state of added" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1),
        properties = List(ukPropertyDetails, foreignPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources, Some(TriggeredMigrationAdded(SelfEmployment)))

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List(CheckHmrcRecordsSoleTraderDetails(IncomeSourceId("XA00001234"),Some("Fruit Ltd"), Some("nextUpdates.business"))),
        hasActiveUkProperty = true,
        hasActiveForeignProperty = true,
        triggeredMigrationState = Some(TriggeredMigrationAdded(SelfEmployment))
      )

      result shouldBe expectedResult
    }

    "return a view model with no active sole trader businesses and an active foreign property business only" in {
      val populatedIncomeSources = baseIncomeSources.copy(
        businesses = List(business1.copy(cessation = Some(CessationModel(Some(LocalDate.now()))))),
        properties = List(foreignPropertyDetails)
      )

      val result = service.getCheckHmrcRecordsViewModel(populatedIncomeSources, None)

      val expectedResult = CheckHmrcRecordsViewModel(
        soleTraderBusinesses = List.empty,
        hasActiveUkProperty = false,
        hasActiveForeignProperty = true,
        triggeredMigrationState = None,
        numberOfCeasedBusinesses = 1
      )

      result shouldBe expectedResult
    }
  }
}
