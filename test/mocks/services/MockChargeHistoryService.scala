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

package mocks.services

import models.chargeHistory.{AdjustmentHistoryModel, ChargeHistoryModel, ChargesHistoryErrorModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.ChargeHistoryService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockChargeHistoryService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockChargeHistoryService: ChargeHistoryService = mock(classOf[ChargeHistoryService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockChargeHistoryService)
  }

  def setupMockChargeHistorySuccessResp(chargeHistoryResponse: List[ChargeHistoryModel]) = {
    when(mockChargeHistoryService.chargeHistoryResponse(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Right(chargeHistoryResponse)))
  }

  def setupMockChargeHistoryFailureResp(chargeHistoryResponse: ChargesHistoryErrorModel) = {
    when(mockChargeHistoryService.chargeHistoryResponse(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Left(chargeHistoryResponse)))
  }

  def setupMockGetAdjustmentHistory(adjustmentHistoryModel: AdjustmentHistoryModel) = {
    when(mockChargeHistoryService.getAdjustmentHistory(any(), any()))
      .thenReturn(adjustmentHistoryModel)
  }

}
