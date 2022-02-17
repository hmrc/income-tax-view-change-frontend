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

import models.liabilitycalculation.viewmodels.IncomeBreakdownViewModel
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.twirl.api.Html
import views.html.IncomeBreakdown

trait MockIncomeSummary extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val incomeBreakdown: IncomeBreakdown = mock[IncomeBreakdown]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(incomeBreakdown)
  }

  def mockIncomeBreakdown(taxYear: Int, calcModel: IncomeBreakdownViewModel, backUrl: String, isAgent:Boolean)
                            (response: Html): Unit = {
    when(incomeBreakdown.apply(
      matches(calcModel),
      matches(taxYear),
      matches(backUrl),
      matches(isAgent),
      any()
    )(any(), any()))
      .thenReturn(response)
  }

}
