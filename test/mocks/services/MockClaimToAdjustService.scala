/*
 * Copyright 2024 HM Revenue & Customs
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

import mocks.connectors.MockFinancialDetailsConnector
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, spy, when}
import org.scalatest.BeforeAndAfterEach
import services.ClaimToAdjustService
import testUtils.UnitSpec

import scala.concurrent.{ExecutionContext, Future}

trait MockClaimToAdjustService extends UnitSpec with BeforeAndAfterEach with MockFinancialDetailsConnector with MockFinancialDetailsService {

  implicit val mockEC: ExecutionContext = mock(classOf[ExecutionContext])
  val mockClaimToAdjustService: ClaimToAdjustService = spy(new ClaimToAdjustService(mockFinancialDetailsConnector, mockFinancialDetailsService))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClaimToAdjustService)
  }

  def setupSpyMaybePoATaxYear(response: Option[TaxYear]): Unit = {
    when(mockClaimToAdjustService.maybePoATaxYear(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

}
