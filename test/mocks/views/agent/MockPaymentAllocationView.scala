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

package mocks.views.agent

import models.paymentAllocationCharges.PaymentAllocationViewModel
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import views.html.PaymentAllocation

trait MockPaymentAllocationView extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val paymentAllocationView: PaymentAllocation = mock[PaymentAllocation]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(paymentAllocationView)
  }

  def mockPaymentAllocationView(viewModel: PaymentAllocationViewModel, backUrl: String, saUtr: Option[String], CutOverCreditsEnabled: Boolean, isAgent: Boolean)(response: Html): Unit = {
    when(paymentAllocationView.apply(
      matches(viewModel),
      matches(backUrl),
      matches(saUtr),
      matches(CutOverCreditsEnabled),
      matches(isAgent),
      any(),
      any()
    )(any(), any()))
      .thenReturn(response)
  }
}
