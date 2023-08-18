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
import connectors.IncomeTaxViewChangeConnector
import exceptions.MissingSessionKey
import forms.utils.SessionKeys.ceaseUKPropertyEndDate
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.ukPropertyIncome
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.UpdateIncomeSourceTestConstants.{cessationDate, failureResponse, successResponse, taxYearSpecific}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.Future

class UpdateIncomeSourceServiceSpec extends TestSupport with FeatureSwitching {
  implicit val userWithSessionKey = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = Some(testRetrievedUserName),
    incomeSources = ukPropertyIncome,
    btaNavPartial = None,
    saUtr = Some("1234567890"),
    credId = Some("credId"),
    userType = Some(Individual),
    None
  )(FakeRequest().withSession(ceaseUKPropertyEndDate -> cessationDate))

  val userWithOutSessionKey = MtdItUser(
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


  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock(classOf[IncomeTaxViewChangeConnector])

  object TestUpdateIncomeSourceService extends UpdateIncomeSourceService(mockIncomeTaxViewChangeConnector)

  "The UpdateIncomeSourceService.updateCessationDate method" should {
    "return UpdateIncomeSourceResponse " when {
      "valid response" in {
        when(mockIncomeTaxViewChangeConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.successResponse))
        TestUpdateIncomeSourceService.updateCessationDate.futureValue shouldBe Right(successResponse)
      }
      "invalid response" in {
        when(mockIncomeTaxViewChangeConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.failureResponse))
        TestUpdateIncomeSourceService.updateCessationDate.futureValue shouldBe Right(failureResponse)
      }
    }

    "return exception " when {
      "session key is missing" in {
        val exception = Left(MissingSessionKey(ceaseUKPropertyEndDate))
        TestUpdateIncomeSourceService.updateCessationDate(userWithOutSessionKey, headerCarrier, ec).futureValue shouldBe exception
      }
    }
  }

  "The UpdateIncomeSourceService.updateCessationDatev2 method" should {
    "return UpdateIncomeSourceSuccess" when {
      val testIncomeSourceId = "123"
      "valid response" in {
        when(mockIncomeTaxViewChangeConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.successResponse))
        TestUpdateIncomeSourceService.updateCessationDatev2(testNino, testIncomeSourceId, cessationDate).futureValue shouldBe Right(UpdateIncomeSourceSuccess(testIncomeSourceId))
      }
      "invalid response" in {
        when(mockIncomeTaxViewChangeConnector.updateCessationDate(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.failureResponse))
        TestUpdateIncomeSourceService.updateCessationDatev2(testNino, testIncomeSourceId, cessationDate).futureValue shouldBe Left(UpdateIncomeSourceError("Failed to update cessationDate"))
      }
    }
  }

  "The UpdateIncomeSourceService.updateTaxYearSpecific method" should {
    "return UpdateIncomeSourceSuccess" when {
      val testIncomeSourceId = "123"
      "valid response" in {
        when(mockIncomeTaxViewChangeConnector.updateIncomeSourceTaxYearSpecific(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.successResponse))
        TestUpdateIncomeSourceService.updateTaxYearSpecific(testNino, testIncomeSourceId, taxYearSpecific).futureValue shouldBe successResponse
      }
      "invalid response" in {
        when(mockIncomeTaxViewChangeConnector.updateIncomeSourceTaxYearSpecific(any(), any(), any())(any()))
          .thenReturn(Future.successful(UpdateIncomeSourceTestConstants.failureResponse))
        TestUpdateIncomeSourceService.updateTaxYearSpecific(testNino, testIncomeSourceId, taxYearSpecific).futureValue shouldBe failureResponse
      }
    }
  }
}
