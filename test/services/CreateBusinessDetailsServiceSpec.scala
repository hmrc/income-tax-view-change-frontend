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
import models.addIncomeSource.{CreateBusinessErrorResponse, AddIncomeSourceResponse}
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
          Right(List(AddIncomeSourceResponse("123")))
        })

      val viewModel = CheckBusinessDetailsViewModel(
        businessName = Some("someBusinessName"),
        businessStartDate = Some(LocalDate.of(2022, 11, 11)),
        businessTrade = "someBusinessTrade",
        businessAddressLine1 = "businessAddressLine1",
        businessAddressLine2 = None,
        businessAddressLine3 = None,
        businessAddressLine4 = None,
        businessPostalCode = Some("SE15 4ER"),
        businessAccountingMethod = None,
        accountingPeriodEndDate = LocalDate.of(2022, 11, 11),
        businessCountryCode = Some("UK"),
        cashOrAccrualsFlag = "Cash"
      )
      val result = UnderTestCreateBusinessDetailsService.createBusinessDetails(viewModel)

      result.futureValue shouldBe Right(AddIncomeSourceResponse("123"))
    }

    "return failure response with error" in {
      when(mockIncomeSourceConnector.createBusiness(any(), any())(any()))
        .thenReturn(Future {
          Left(CreateBusinessErrorResponse(Status.INTERNAL_SERVER_ERROR, s"Error creating incomeSource"))
        })
      val result = UnderTestCreateBusinessDetailsService.createBusinessDetails(viewModel)
      result.futureValue match {
        case Left(_) => succeed
        case Right(_) => fail("Expecting to return left")
      }
    }

  }

  "CreateBusinessDetailsService call create foreign property " when {


    "return success response with incomeSourceId" in {
      when(mockIncomeSourceConnector.createForeignProperty(any(), any())(any()))
        .thenReturn(Future {
          Right(List(AddIncomeSourceResponse("561")))
        })

      val viewModel = CheckForeignPropertyViewModel(tradingStartDate = LocalDate.of(2011, 1, 1),
        cashOrAccrualsFlag = "Cash"
      )
      val result = UnderTestCreateBusinessDetailsService.createForeignProperty(viewModel)

      result.futureValue shouldBe Right(AddIncomeSourceResponse("561"))
    }

  }
}
