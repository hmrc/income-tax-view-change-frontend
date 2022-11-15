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

import models.financialDetails.{BalanceDetails, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito.mock
import services.WhatYouOweService
import testUtils.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future

trait MockWhatYouOweService extends UnitSpec with BeforeAndAfterEach {

  val mockWhatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])

  val emptyWhatYouOweChargesList: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.0, 0.0, 0.0, None, None, None, None))

  val oneOverdueBCDPaymentInWhatYouOweChargesList: WhatYouOweChargesList =
    emptyWhatYouOweChargesList.copy(
      outstandingChargesModel = Some(OutstandingChargesModel(List(OutstandingChargeModel("BCD", Some(LocalDate.parse("2019-01-31")), 1.67, 2345))))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockWhatYouOweService)
  }

  def setupMockGetWhatYouOweChargesListEmpty(): Unit =
    when(mockWhatYouOweService.getWhatYouOweChargesList()(any(), any()))
      .thenReturn(Future.successful(emptyWhatYouOweChargesList))

  def setupMockGetWhatYouOweChargesListEmptyFromFinancialDetails(): Unit =
    when(mockWhatYouOweService.getWhatYouOweChargesList(any())(any(), any()))
      .thenReturn(Future.successful(emptyWhatYouOweChargesList))

  def setupMockGetWhatYouOweChargesListWithOne(): Unit =
    when(mockWhatYouOweService.getWhatYouOweChargesList()(any(), any()))
      .thenReturn(Future.successful(oneOverdueBCDPaymentInWhatYouOweChargesList))
  def setupMockGetWhatYouOweChargesListWithOneFromFinancialDetails(): Unit =
    when(mockWhatYouOweService.getWhatYouOweChargesList(any())(any(), any()))
      .thenReturn(Future.successful(oneOverdueBCDPaymentInWhatYouOweChargesList))

}
