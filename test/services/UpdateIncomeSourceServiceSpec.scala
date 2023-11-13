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
import connectors.UpdateIncomeSourceConnector
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.BusinessDetailsTestConstants.testMtdItId
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.UpdateIncomeSourceTestConstants.{failureResponse, successResponse, taxYearSpecific}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.ukPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.Future

class UpdateIncomeSourceServiceSpec extends TestSupport with FeatureSwitching {
  implicit val userWithSessionKey: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = ukPropertyIncome,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some(Individual),
    None
  )(FakeRequest())

  val cessationDate = "2022-07-01"

  val mockUpdateIncomeSourceConnector: UpdateIncomeSourceConnector = mock(classOf[UpdateIncomeSourceConnector])

  object TestUpdateIncomeSourceService extends UpdateIncomeSourceService(mockUpdateIncomeSourceConnector)

  "The UpdateIncomeSourceService.updateCessationDate method" should {
    "return UpdateIncomeSourceResponse " when {
      "valid response" in {
        when(mockUpdateIncomeSourceConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.successResponse))
        TestUpdateIncomeSourceService.updateCessationDate(testNino, testMtdItId, cessationDate).futureValue shouldBe Right(UpdateIncomeSourceSuccess(testMtdItId))
      }
      "invalid response" in {
        when(mockUpdateIncomeSourceConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.failureResponse))
        TestUpdateIncomeSourceService.updateCessationDate(testNino, testMtdItId, cessationDate).futureValue shouldBe Left(failureResponse)
      }
    }
  }

  "The UpdateIncomeSourceService.updateCessationDatev2 method" should {
    "return UpdateIncomeSourceSuccess" when {
      val testIncomeSourceId = "123"
      "valid response" in {
        when(mockUpdateIncomeSourceConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.successResponse))
        TestUpdateIncomeSourceService.updateCessationDate(testNino, testIncomeSourceId, cessationDate).futureValue shouldBe Right(UpdateIncomeSourceSuccess(testIncomeSourceId))
      }
      "invalid response" in {
        when(mockUpdateIncomeSourceConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.failureResponse))
        TestUpdateIncomeSourceService.updateCessationDate(testNino, testIncomeSourceId, cessationDate).futureValue shouldBe Left(failureResponse)
      }
    }
  }

  "The UpdateIncomeSourceService.updateTaxYearSpecific method" should {
    "return UpdateIncomeSourceSuccess" when {
      val testIncomeSourceId = "123"
      "valid response" in {
        when(mockUpdateIncomeSourceConnector.updateIncomeSourceTaxYearSpecific(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.successResponse))
        TestUpdateIncomeSourceService.updateTaxYearSpecific(testNino, testIncomeSourceId, taxYearSpecific).futureValue shouldBe successResponse
      }
      "invalid response" in {
        when(mockUpdateIncomeSourceConnector.updateIncomeSourceTaxYearSpecific(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.failureResponse))
        TestUpdateIncomeSourceService.updateTaxYearSpecific(testNino, testIncomeSourceId, taxYearSpecific).futureValue shouldBe failureResponse
      }
    }
  }
}
