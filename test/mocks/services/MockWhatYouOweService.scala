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

import models.financialDetails.{BalanceDetails, ChargeItem, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.{DateService, WhatYouOweService}
import testUtils.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future

trait MockWhatYouOweService extends UnitSpec with MockDateService with BeforeAndAfterEach {

  implicit val dateService: DateService = mockDateService
  lazy val mockWhatYouOweService: WhatYouOweService = mock(classOf[WhatYouOweService])

  val emptyWhatYouOweChargesList: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(0.0, 0.0, 0.0, None, None, None, None, None, None, None))

  val oneOverdueBCDPaymentInWhatYouOweChargesList: WhatYouOweChargesList =
    emptyWhatYouOweChargesList.copy(
      outstandingChargesModel = Some(OutstandingChargesModel(List(OutstandingChargeModel("BCD", Some(LocalDate.parse("2019-01-31")), 1.67, 2345))))
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockWhatYouOweService)
  }

  def setupMockGetWhatYouOweChargesListFromFinancialDetails(whatYouOweChargesList: WhatYouOweChargesList): Unit = {
    when(mockWhatYouOweService.getWhatYouOweChargesList(any(), any(), any(), any(), any())(any(), any()))
      .thenReturn(Future.successful(whatYouOweChargesList))
  }

  def setupMockGetFilteredChargesListFromFinancialDetails(chargeItems: List[ChargeItem]): Unit = {
    when(mockWhatYouOweService.getFilteredChargesList(any(), any(), any(), any()))
      .thenReturn(chargeItems)
  }

}
