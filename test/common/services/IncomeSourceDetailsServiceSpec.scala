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

package common.services

import businessDetails.models.incomeSourceDetails.viewmodels.{AddIncomeSourcesViewModel, BusinessDetailsViewModel, CeaseBusinessDetailsViewModel, CeaseIncomeSourcesViewModel, CeasePropertyDetailsViewModel, CeasedBusinessDetailsViewModel, PropertyDetailsViewModel, ViewIncomeSourcesViewModel, ViewPropertyDetailsViewModel}
import businessDetails.services.IncomeSourceDetailsService
import common.auth.{AuthorisedAndEnrolledRequest, MtdItUser}
import common.auth.actions.AuthActionsTestData.defaultMTDITUser
import common.enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import common.mocks.MockAuditingService
import common.mocks.services.MockAsyncCacheApi
import common.mocks.services.config.MockAppConfig
import common.models.admin.{DisplayBusinessStartDate, FeatureSwitch}
import common.models.core.IncomeSourceId.mkIncomeSourceId
import businessDetails.models.incomeSourceDetails.viewmodels.*
import obligations.mocks.services.MockNextUpdatesService
import play.api.cache.AsyncCacheApi
import common.testConstants.BaseTestConstants.*
import businessDetails.testConstants.BusinessDetailsTestConstants.*
import businessDetails.testConstants.PropertyDetailsTestConstants.{testStartDate as _, *}
import common.models.incomeSourceDetails.IncomeSourceDetailsModel
import common.testConstants.IncomeSourceDetailsTestConstants.*
import common.testUtils.TestSupport

import scala.util.Success

//scalastyle:off
class IncomeSourceDetailsServiceSpec extends TestSupport with MockNextUpdatesService
  with MockAuditingService with MockAsyncCacheApi with MockAppConfig {
  val cache = app.injector.instanceOf[AsyncCacheApi]
  val expectedAddressString1: Option[String] = Some("Line 1<br>Line 2<br>Line 3<br>Line 4<br>LN1 1NL<br>NI")
  val expectedAddressString2: Option[String] = Some("A Line 1<br>A Line 3<br>LN2 2NL<br>GB")
  implicit val authorisedAndEnrolledRequest: AuthorisedAndEnrolledRequest[_] = testAuthorisedAndEnrolled

  override implicit val individualUser: MtdItUser[_] = defaultMTDITUser(
    Some(testUserTypeIndividual),
    IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty)
  )
    .addFeatureSwitches(List(FeatureSwitch(DisplayBusinessStartDate, true)))

  object TestIncomeSourceDetailsService extends IncomeSourceDetailsService()(mockAppConfig, ec)

  override def beforeEach(): Unit = {
    super.beforeEach()
    cache.removeAll()
  }

  "The IncomeSourceDetailsService.getAddIncomeSourceViewModel method" when {
    "a user has a uk property and a sole trader business" should {
      "return an AddIncomeSourcesViewModel with a sole trader business and uk property" in {

        val result = TestIncomeSourceDetailsService.getAddIncomeSourceViewModel(ukPropertyAndSoleTraderBusinessIncome, displayBusinessStartDateFS = true)

        result shouldBe Success(AddIncomeSourcesViewModel(
          soleTraderBusinesses = List(BusinessDetailsViewModel(Some(testTradeName), Some(testStartDate))),
          ukProperty = Some(PropertyDetailsViewModel(Some(testStartDate))),
          foreignProperty = None,
          ceasedBusinesses = Nil,
          displayStartDate = true
        )
        )
      }
    }
    "a user has a foreign property and a ceased businesses" should {
      "return an AddIncomeSourcesViewModel with a foreign property, ceased business, ceased uk property and ceased foreign property" in {

        val result = TestIncomeSourceDetailsService.getAddIncomeSourceViewModel(foreignPropertyAndCeasedBusinessesIncome, displayBusinessStartDateFS = true)

        result shouldBe Success(AddIncomeSourcesViewModel(
          soleTraderBusinesses = Nil,
          ukProperty = None,
          foreignProperty = Some(PropertyDetailsViewModel(Some(testStartDate))),
          ceasedBusinesses = List(
            CeasedBusinessDetailsViewModel(testTradeNameOption2, SelfEmployment, testStartDateOption3, testCessation2.date.get),
            CeasedBusinessDetailsViewModel(None, UkProperty, testPropertyStartDateOption, testPropertyCessation3.date.get),
            CeasedBusinessDetailsViewModel(None, ForeignProperty, testPropertyStartDateOption2, testPropertyCessation2.date.get),
          ),
          displayStartDate = true
        )
        )
      }
    }
  }

  "The IncomeSourceDetailsService.getCeaseIncomeSourceViewModel method" when {
    "a user has a uk property and a sole trader business" should {
      "return a CeaseIncomeSourcesViewModel with a sole trader business and uk property" in {

        val result = TestIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(ukPropertyAndSoleTraderBusinessIncome, displayBusinessStartDateFS = true)

        result shouldBe Right(CeaseIncomeSourcesViewModel(
          soleTraderBusinesses = List(CeaseBusinessDetailsViewModel(mkIncomeSourceId(testSelfEmploymentId), Some(testTradeName), Some(testStartDate))),
          ukProperty = Some(CeasePropertyDetailsViewModel(Some(testStartDate))),
          foreignProperty = None,
          ceasedBusinesses = Nil,
          displayStartDate = true
        ))
      }
    }
    "a user has a foreign property and a ceased businesses" should {
      "return an AddIncomeSourcesViewModel with a foreign property, ceased business, ceased uk property and ceased foreign property" in {

        val result = TestIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(foreignPropertyAndCeasedBusinessesIncome, displayBusinessStartDateFS = true)

        result shouldBe Right(CeaseIncomeSourcesViewModel(
          soleTraderBusinesses = Nil,
          ukProperty = None,
          foreignProperty = Some(CeasePropertyDetailsViewModel(Some(testStartDate))),
          ceasedBusinesses = List(
            CeasedBusinessDetailsViewModel(testTradeNameOption2, SelfEmployment, testStartDateOption3, testCessation2.date.get),
            CeasedBusinessDetailsViewModel(None, UkProperty, testPropertyStartDateOption, testPropertyCessation3.date.get),
            CeasedBusinessDetailsViewModel(None, ForeignProperty, testPropertyStartDateOption2, testPropertyCessation2.date.get),
          ),
          displayStartDate = true
        ))
      }
    }

  }

  "The IncomeSourceDetailsService.getViewIncomeSourceViewModel method" when {
    "a user has a uk property and a sole trader business" should {
      "return a ViewIncomeSourcesViewModel with a sole trader business and uk property" in {

        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(ukPropertyAndSoleTraderBusinessIncome, true)

        result shouldBe Right(ViewIncomeSourcesViewModel(
          viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel2),
          viewUkProperty = Some(viewUkPropertyDetailsViewModel),
          viewForeignProperty = None,
          viewCeasedBusinesses = Nil,
          displayStartDate = true))
      }
    }
    "a user has a foreign property and a ceased businesses" should {
      "return an AddIncomeSourcesViewModel with a foreign property, ceased business, ceased uk property and ceased foreign property" in {

        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(foreignPropertyAndCeasedBusinessesIncome, true)

        result shouldBe Right(ViewIncomeSourcesViewModel(
          viewSoleTraderBusinesses = Nil,
          viewUkProperty = None,
          viewForeignProperty = Some(ViewPropertyDetailsViewModel(testStartDateOption)),
          viewCeasedBusinesses = List(
            CeasedBusinessDetailsViewModel(testTradeNameOption2, SelfEmployment, testStartDateOption3, testCessation2.date.get),
            CeasedBusinessDetailsViewModel(None, UkProperty, testPropertyStartDateOption, testPropertyCessation3.date.get),
            CeasedBusinessDetailsViewModel(None, ForeignProperty, testPropertyStartDateOption2, testPropertyCessation2.date.get),
          ),
          displayStartDate = true
        ))
      }
    }
  }

  "The IncomeSourceDetailsService.getCeasedBusinesses method" when {
    "a user has a business without a cessation date" should {
      "return the list of ceased income sources without the income source without a cessation date" in {
        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(foreignPropertyAndCeasedBusinessesIncome, true)

        result shouldBe Right(ViewIncomeSourcesViewModel(
          viewSoleTraderBusinesses = Nil,
          viewUkProperty = None,
          viewForeignProperty = Some(ViewPropertyDetailsViewModel(testStartDateOption)),
          viewCeasedBusinesses = List(
            CeasedBusinessDetailsViewModel(testTradeNameOption2, SelfEmployment, testStartDateOption3, testCessation2.date.get),
            CeasedBusinessDetailsViewModel(None, UkProperty, testPropertyStartDateOption, testPropertyCessation3.date.get),
            CeasedBusinessDetailsViewModel(None, ForeignProperty, testPropertyStartDateOption2, testPropertyCessation2.date.get),
          ),
          displayStartDate = true
        ))
      }
    }
    "a user has a property business without an income source type" should {
      "return the list of ceased income sources without the income source without an income source type" in {
        val result = TestIncomeSourceDetailsService.getViewIncomeSourceViewModel(foreignPropertyAndCeasedPropertyIncomeWithNoIncomeSourceType, true)

        result shouldBe Right(ViewIncomeSourcesViewModel(
          viewSoleTraderBusinesses = Nil,
          viewUkProperty = None,
          viewForeignProperty = Some(ViewPropertyDetailsViewModel(testStartDateOption)),
          viewCeasedBusinesses = List(
            //            CeasedBusinessDetailsViewModel(testTradeNameOption2, SelfEmployment, testStartDateOption3, testCessation2.date.get),
            CeasedBusinessDetailsViewModel(None, UkProperty, testPropertyStartDateOption, testPropertyCessation3.date.get)
          ),
          displayStartDate = true
        ))
      }
    }
  }
}