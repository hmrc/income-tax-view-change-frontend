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

package mocks.views.agent

import financials.models.paymentAllocationCharges.PaymentAllocationViewModel
import financials.views.html.PaymentAllocationView
import org.mockito.ArgumentMatchers.{any, eq as matches}
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import play.twirl.api.Html

trait MockPaymentAllocationView extends BeforeAndAfterEach {
  self: Suite =>

  val paymentAllocationView: PaymentAllocationView = mock(classOf[PaymentAllocationView])


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(paymentAllocationView)
  }

  def mockPaymentAllocationView(viewModel: PaymentAllocationViewModel, backUrl: String, saUtr: Option[String])(response: Html): Unit = {
    when(paymentAllocationView.apply(
      matches(viewModel),
      matches(backUrl),
      matches(saUtr),
      any(),
      any(),
      any(),
      any()
    )(any(), any(), any()))
      .thenReturn(response)
  }
}
