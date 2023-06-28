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
import connectors.IncomeSourceConnector
import models.addIncomeSource.IncomeSourceResponse
import models.incomeSourceDetails.viewmodels._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncomeWithCurrentYear
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate
import scala.concurrent.Future

class CreateBusinessDetailsServiceSpec extends  TestSupport with FeatureSwitching {

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

  val mockIncomeSourceConnector: IncomeSourceConnector = mock(classOf[IncomeSourceConnector])

  object UnderTestCreateBusinessDetailsService extends CreateBusinessDetailsService(mockIncomeSourceConnector)


  "CreateBusinessDetailsService call create" when {
    "" should {
      "return expected incomeSourceId" in {
        when(mockIncomeSourceConnector.create(any(), any())(any()))
          .thenReturn(Future{ Right(List(IncomeSourceResponse("123"))) })

        val viewModel = CheckBusinessDetailsViewModel(
          businessName = Some("someBusinessName"),
          businessStartDate = Some(LocalDate.of(2022, 11, 11)),
          businessTrade = Some("someBusinessTrade"),
          businessAddressLine1 = "businessAddressLine1",
          businessPostalCode = Some("SE15 4ER"),
          businessAccountingMethod = None
        )
        val result = UnderTestCreateBusinessDetailsService.createBusinessDetails("someMtditId", viewModel)

        result.futureValue shouldBe Right(IncomeSourceResponse("123"))
      }
    }
  }
}
