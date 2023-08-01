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

package services

import audit.mocks.MockAuditingService
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.services.{MockAsyncCacheApi, MockNextUpdatesService}
import models.incomeSourceDetails.viewmodels.{AddIncomeSourcesViewModel, BusinessDetailsViewModel, CeaseBusinessDetailsViewModel, CeaseCeasedBusinessDetailsViewModel, CeaseIncomeSourcesViewModel, CeasePropertyDetailsViewModel, CeasedBusinessDetailsViewModel, PropertyDetailsViewModel, ViewCeasedBusinessDetailsViewModel, ViewIncomeSourcesViewModel, ViewPropertyDetailsViewModel}
import org.scalacheck.Gen
import play.api.cache.AsyncCacheApi
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants._
import testConstants.IncomeSourceDetailsTestConstants._
import testConstants.PropertyDetailsTestConstants.viewUkPropertyDetailsViewModel
import testUtils.TestSupport

import scala.util.Success

//scalastyle:off
class IncomeSourceDetailsServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector with MockNextUpdatesService
  with MockAuditingService with MockAsyncCacheApi {
  val cache = app.injector.instanceOf[AsyncCacheApi]

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService(mockIncomeTaxViewChangeConnector, cache)

  override def beforeEach(): Unit = {
    super.beforeEach()
    cache.removeAll()
  }

  "The IncomeSourceDetailsService.getIncomeSourceDetails method" when {

    "a result with both business and property details is returned" should {

      "return an IncomeSourceDetailsModel with business and property options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(businessesAndPropertyIncome)
        TestIncomeSourceDetailsService.getIncomeSourceDetails().futureValue shouldBe businessesAndPropertyIncome
      }
    }

    "a result with just business details is returned" should {
      "return an IncomeSourceDetailsModel with just a business option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(singleBusinessIncome)
        TestIncomeSourceDetailsService.getIncomeSourceDetails().futureValue shouldBe singleBusinessIncome
      }
    }

    "a result with just property details is returned" should {
      "return an IncomeSourceDetailsModel with just a property option" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(propertyIncomeOnly)
        TestIncomeSourceDetailsService.getIncomeSourceDetails().futureValue shouldBe propertyIncomeOnly
      }
    }

    "a result with no income source details is returned" should {
      "return an IncomeSourceDetailsModel with no options" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(noIncomeDetails)
        TestIncomeSourceDetailsService.getIncomeSourceDetails().futureValue shouldBe noIncomeDetails
      }
    }

    "a result where the Income Source Details are error" should {
      "return an IncomeSourceError" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(errorResponse)
        TestIncomeSourceDetailsService.getIncomeSourceDetails().futureValue shouldBe errorResponse
      }
    }
    "caching" when {
      "should cache" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(noIncomeDetails)
        TestIncomeSourceDetailsService.getIncomeSourceDetails(Some("key")).futureValue shouldBe noIncomeDetails
        TestIncomeSourceDetailsService.getIncomeSourceDetails(Some("key")).futureValue shouldBe noIncomeDetails
        verifyMockIncomeSourceDetailsResponse(1)
      }

      "should NOT cache" in {
        setupMockIncomeSourceDetailsResponse(testMtditid, testNino, Some(testSaUtr), Some(testCredId), Some(testUserType))(noIncomeDetails)
        TestIncomeSourceDetailsService.getIncomeSourceDetails(Some("key2")).futureValue shouldBe noIncomeDetails
        TestIncomeSourceDetailsService.getIncomeSourceDetails(Some("someotherkey")).futureValue shouldBe noIncomeDetails
        verifyMockIncomeSourceDetailsResponse(2)
      }
    }
  }

  "The IncomeSourceDetailsService.getAddIncomeSourceViewModel method" when {
    "a user has a uk property and a sole trader business" should {
      "return an AddIncomeSourcesViewModel with a sole trader business and uk property" in {

        val result = TestIncomeSourceDetailsService.getAddIncomeSourceViewModel(ukPropertyAndSoleTraderBusinessIncome)

        result shouldBe Success(AddIncomeSourcesViewModel(
          soleTraderBusinesses = List(BusinessDetailsViewModel(Some(testTradeName), Some(testStartDate))),
          ukProperty = Some(PropertyDetailsViewModel(Some(testStartDate))),
          foreignProperty = None,
          ceasedBusinesses = Nil
        )
        )
      }
    }
    "a user has a foreign property and a ceased businesses" should {
      "return an AddIncomeSourcesViewModel with a foreign property and ceased businesses" in {

        val result = TestIncomeSourceDetailsService.getAddIncomeSourceViewModel(foreignPropertyAndCeasedBusinessIncome)

        result shouldBe Success(AddIncomeSourcesViewModel(
          soleTraderBusinesses = Nil,
          ukProperty = None,
          foreignProperty = Some(PropertyDetailsViewModel(Some(testStartDate))),
          ceasedBusinesses = List(
            CeasedBusinessDetailsViewModel(Some(testTradeName), Some(testStartDate), testCessation.date.get),
            CeasedBusinessDetailsViewModel(Some(testTradeName2), Some(testStartDate2), testCessation2.date.get)
          )
        )
        )
      }
    }
  }

  "The IncomeSourceDetailsService.getCeaseIncomeSourceViewModel method" when {
    "a user has a uk property and a sole trader business" should {
      "return a CeaseIncomeSourcesViewModel with a sole trader business and uk property" in {

        val result = TestIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(ukPropertyAndSoleTraderBusinessIncome)

        result shouldBe Right(CeaseIncomeSourcesViewModel(
          soleTraderBusinesses = List(CeaseBusinessDetailsViewModel(testSelfEmploymentId, Some(testTradeName), testStartDate, Some(address))),
          ukProperty = Some(CeasePropertyDetailsViewModel(testStartDate)),
          foreignProperty = None,
          ceasedBusinesses = Nil
        ))
      }
    }
    "a user has a foreign property and a ceased businesses" should {
      "return a CeaseIncomeSourcesViewModel with a foreign property and ceased businesses" in {

        val result = TestIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(foreignPropertyAndCeasedBusinessIncome)

        result shouldBe Right(CeaseIncomeSourcesViewModel(
          soleTraderBusinesses = Nil,
          ukProperty = None,
          foreignProperty = Some(CeasePropertyDetailsViewModel(testStartDate)),
          ceasedBusinesses = List(
            CeaseCeasedBusinessDetailsViewModel(testTradeName, testStartDate, testCessation.date.get),
            CeaseCeasedBusinessDetailsViewModel(testTradeName2, testStartDate2, testCessation2.date.get)
          )
        ))
      }
    }

    "invalid data provided" should {
      "return failure" in {
        // Simulate dynamic data generation from one of the invalid data
        // TODO: eventually need to be move under tests data generation section
        val generatedFailedData = Gen.oneOf(Seq(ukPropertyAndSoleTraderBusinessIncomeNoTradingName, ukPropertyAndSoleTraderBusinessIncomeNoTradingStartDate, foreignPropertyAndCeasedBusinessIncomeNoStartDate)).sample.get
        val result = TestIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(generatedFailedData)
        result.isLeft should be(true)
      }
    }
  }

  "The IncomeSourceDetailsService.getViewIncomeSourceViewModel method" when {
    "a user has a uk property and a sole trader business" should {
      "return a ViewIncomeSourcesViewModel with a sole trader business and uk property" in {

        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(ukPropertyAndSoleTraderBusinessIncome)

        result shouldBe Right(ViewIncomeSourcesViewModel(
          viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
          viewUkProperty = Some(viewUkPropertyDetailsViewModel),
          viewForeignProperty = None,
          viewCeasedBusinesses = Nil))
      }
    }
    "a user has a foreign property and a ceased businesses" should {
      "return a ViewIncomeSourcesViewModel with a foreign property and ceased businesses" in {

        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(foreignPropertyAndCeasedBusinessIncome)

        result shouldBe Right(ViewIncomeSourcesViewModel(
          viewSoleTraderBusinesses = Nil,
          viewUkProperty = None,
          viewForeignProperty = Some(ViewPropertyDetailsViewModel(testStartDateOption)),
          viewCeasedBusinesses = List(
            ViewCeasedBusinessDetailsViewModel(testTradeNameOption, testStartDateOption, testCessation.date.get),
            ViewCeasedBusinessDetailsViewModel(testTradeNameOption2, testStartDateOption2, testCessation2.date.get)
          )
        ))
      }
    }

    "invalid data provided" should {
      "still return page as unknown is used rather than error" in {
        // Simulate dynamic data generation from one of the invalid data
        // TODO: eventually need to be move under tests data generation section
        val generatedFailedData = Gen.oneOf(Seq(ukPropertyAndSoleTraderBusinessIncomeNoTradingName, ukPropertyAndSoleTraderBusinessIncomeNoTradingStartDate, foreignPropertyAndCeasedBusinessIncomeNoStartDate)).sample.get
        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(generatedFailedData)
        result.isLeft should be(false)
      }
    }
  }
}