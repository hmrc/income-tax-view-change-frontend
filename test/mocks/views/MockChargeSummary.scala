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

package mocks.views

import models.chargeSummary.ChargeSummaryViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import play.twirl.api.Html
import views.html.ChargeSummary

trait MockChargeSummary extends BeforeAndAfterEach {
  self: Suite =>

  val chargeSummary: ChargeSummary = mock(classOf[ChargeSummary])
  val viewModel: ChargeSummaryViewModel
  val whatYouOweLink: String
  val saChargesUrl: String
  val yourSelfAssessmentChargesFS: Boolean

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(chargeSummary)
  }

  def mockChargeSummary()(response: Html): Unit = {
    when(chargeSummary.apply(viewModel, whatYouOweLink, saChargesUrl, yourSelfAssessmentChargesFS )(any(), any(), any(), any()))
      .thenReturn(response)
  }

}
