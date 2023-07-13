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
import config.featureswitch.FeatureSwitching
import connectors.CreateIncomeSourceConnector
import connectors.helpers.IncomeSourcesDataHelper
import models.createIncomeSource.{CreateIncomeSourcesErrorResponse, CreateIncomeSourcesResponse}
import models.incomeSourceDetails.viewmodels._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.concurrent.Future

class CreateBusinessDetailsServiceSpec extends TestSupport with FeatureSwitching with IncomeSourcesDataHelper {

  implicit val mtdItUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = singleBusinessIncomeWithCurrentYear,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some(Individual),
    None
  )(FakeRequest())

  val mockIncomeSourceConnector: CreateIncomeSourceConnector = mock(classOf[CreateIncomeSourceConnector])

  object UnderTestCreateBusinessDetailsService extends CreateBusinessDetailsService(mockIncomeSourceConnector)

  "CreateBusinessDetailsService call create business" when {


    "return success response with incomeSourceId" in {
      when(mockIncomeSourceConnector.createBusiness(any(), any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourcesResponse("123")))
        })

      val result = UnderTestCreateBusinessDetailsService.createBusinessDetails(createBusinessViewModel)

      result.futureValue shouldBe Right(CreateIncomeSourcesResponse("123"))
    }

    "return failure response with error" in {
      when(mockIncomeSourceConnector.createBusiness(any(), any())(any()))
        .thenReturn(Future {
          Left(CreateIncomeSourcesErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Error creating incomeSource"))
        })
      val result = UnderTestCreateBusinessDetailsService.createBusinessDetails(createBusinessViewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Expecting to fail")
      }
    }

    "return failure: wrong data" in {
      when(mockIncomeSourceConnector.createBusiness(any(), any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourcesResponse("561")))
        })

      // set view model with the wrong data
      val viewModel = createBusinessViewModel.copy(businessName = None)
      val result = UnderTestCreateBusinessDetailsService.createBusinessDetails(viewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Incorrect data in the view model")
      }
    }

  }

  "CreateBusinessDetailsService call create foreign property " when {

    "return success response with incomeSourceId" in {
      when(mockIncomeSourceConnector.createForeignProperty(any(), any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourcesResponse("561")))
        })

      val viewModel = CheckForeignPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1),
        cashOrAccrualsFlag = "CASH"
      )
      val result = UnderTestCreateBusinessDetailsService.createForeignProperty(viewModel)

      result.futureValue shouldBe Right(CreateIncomeSourcesResponse("561"))
    }

    "return failure response with error" in {
      when(mockIncomeSourceConnector.createForeignProperty(any(), any())(any()))
        .thenReturn(Future {
          Left(CreateIncomeSourcesErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Error creating incomeSource"))
        })
      val result = UnderTestCreateBusinessDetailsService.createForeignProperty(createForeignPropertyViewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Expecting to fail")
      }
    }

    "return failure: wrong data" in {
      when(mockIncomeSourceConnector.createForeignProperty(any(), any())(any()))
        .thenReturn(Future {
          Right(List(CreateIncomeSourcesResponse("561")))
        })

      // set cashOrAccruals field to lowercase to cause failure
      val viewModel = CheckForeignPropertyViewModel(
        tradingStartDate = LocalDate.of(2011, 1, 1),
        cashOrAccrualsFlag = "cash"
      )
      val result = UnderTestCreateBusinessDetailsService.createForeignProperty(viewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Incorrect data in the view model")
      }
    }

  }
}
