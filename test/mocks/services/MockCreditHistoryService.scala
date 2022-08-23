/*
 * Copyright 2022 HM Revenue & Customs
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

import models.{CreditDetailModel, MfaCreditType}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.CreditHistoryService
import services.CreditHistoryService.CreditHistoryError
import services.agent.ClientDetailsService
import services.agent.ClientDetailsService.{ClientDetails, ClientDetailsFailure}
import testConstants.BaseTestConstants.testNino
import testConstants.FinancialDetailsTestConstants.{creditAndRefundCreditDetailMFA, creditAndRefundDocumentDetailListMFA}
import testUtils.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait MockCreditHistoryService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockCreditHistoryService: CreditHistoryService = mock[CreditHistoryService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCreditHistoryService)
  }

  def mockCreditHistoryService(creditDetailsModel: List[CreditDetailModel]): Unit = {
    when(mockCreditHistoryService.getCreditsHistory(any(), any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(creditDetailsModel)))
  }



  def mockCreditHistoryServiceError(): Unit = {
    when(mockCreditHistoryService.getCreditsHistory(any(), any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Left(CreditHistoryError)))
  }

}
