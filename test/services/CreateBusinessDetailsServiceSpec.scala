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

import auth.MtdItUser
import authV2.AuthActionsTestData._
import config.featureswitch.FeatureSwitching
import connectors.CreateIncomeSourceConnector
import connectors.helpers.IncomeSourcesDataHelper
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import models.createIncomeSource._
import models.incomeSourceDetails.viewmodels._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import testConstants.BaseTestConstants.testMtditid
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.concurrent.Future

class CreateBusinessDetailsServiceSpec extends TestSupport with FeatureSwitching with IncomeSourcesDataHelper {

  implicit val mtdItUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), singleBusinessIncomeWithCurrentYear)

  val mockIncomeSourceConnector: CreateIncomeSourceConnector = mock(classOf[CreateIncomeSourceConnector])

  object UnderTestCreateBusinessDetailsService extends CreateBusinessDetailsService(mockIncomeSourceConnector)

  "CreateBusinessDetailsService call create business" when {


    "return success response with incomeSourceId" in {
      when(mockIncomeSourceConnector.createBusiness(any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourceResponse("XAIT00000000000")))
        })

      val result = UnderTestCreateBusinessDetailsService.createBusiness(createBusinessViewModel)

      result.futureValue shouldBe Right(CreateIncomeSourceResponse("XAIT00000000000"))
    }

    "return failure response with error" in {
      when(mockIncomeSourceConnector.createBusiness(any())(any()))
        .thenReturn(Future {
          Left(CreateIncomeSourceErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Error creating incomeSource"))
        })
      val result = UnderTestCreateBusinessDetailsService.createBusiness(createBusinessViewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Expecting to fail")
      }
    }

    "return failure: wrong data" in {
      when(mockIncomeSourceConnector.createBusiness(any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourceResponse("561")))
        })

      // set view model with the wrong data
      val viewModel = createBusinessViewModel.copy(businessName = None)
      val result = UnderTestCreateBusinessDetailsService.createBusiness(viewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Incorrect data in the view model")
      }
    }
    "convertToCreateBusinessIncomeSourceRequest" should {
      "convert to correct CreateBusinessIncomeSourceRequest model " in {
        val viewModel = createBusinessViewModel
        val result = UnderTestCreateBusinessDetailsService.convertToCreateBusinessIncomeSourceRequest(viewModel)
        result shouldBe Right(CreateBusinessIncomeSourceRequest(testMtditid,
          List(BusinessDetails("2022-11-11", "2022-11-11", "someBusinessName",
            AddressDetails("businessAddressLine1", None, None, None, Some("GB"), Some("SE15 4ER")),
            Some("someBusinessTrade"), "2022-11-11", None, None))))
      }
    }

  }

  "CreateBusinessDetailsService call create foreign property " when {

    "return success response with incomeSourceId" in {
      when(mockIncomeSourceConnector.createForeignProperty(any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourceResponse("XIAT00000000000")))
        })

      val viewModel = CheckPropertyViewModel(
        tradingStartDate = LocalDate.of(2011, 1, 1),
        incomeSourceType = ForeignProperty
      )
      val result = UnderTestCreateBusinessDetailsService.createForeignProperty(viewModel)

      result.futureValue shouldBe Right(CreateIncomeSourceResponse("XIAT00000000000"))
    }

    "return failure response with error" in {
      when(mockIncomeSourceConnector.createForeignProperty(any())(any()))
        .thenReturn(Future {
          Left(CreateIncomeSourceErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Error creating incomeSource"))
        })
      val result = UnderTestCreateBusinessDetailsService.createForeignProperty(createForeignPropertyViewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Expecting to fail")
      }
    }
  }

  "CreateBusinessDetailsService call create UK property " when {

    "return success response with incomeSourceId" in {
      when(mockIncomeSourceConnector.createUKProperty(any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourceResponse("XIAT00000000000")))
        })

      val viewModel = CheckPropertyViewModel(
        tradingStartDate = LocalDate.of(2011, 1, 1),
        incomeSourceType = UkProperty
      )
      val result = UnderTestCreateBusinessDetailsService.createUKProperty(viewModel)

      result.futureValue shouldBe Right(CreateIncomeSourceResponse("XIAT00000000000"))
    }

    "return failure response with error" in {
      when(mockIncomeSourceConnector.createUKProperty(any())(any()))
        .thenReturn(Future {
          Left(CreateIncomeSourceErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Error creating incomeSource"))
        })
      val viewModel = CheckPropertyViewModel(
        tradingStartDate = LocalDate.of(2011, 1, 1),
        incomeSourceType = UkProperty
      )
      val result = UnderTestCreateBusinessDetailsService.createUKProperty(viewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Expecting to fail")
      }
    }
  }
}
