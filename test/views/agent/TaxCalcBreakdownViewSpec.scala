/*
 * Copyright 2021 HM Revenue & Customs
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

package views.agent

import assets.MessagesLookUp.TaxCalcBreakdown
import models.calculation.CalcDisplayModel
import play.twirl.api.Html
import views.TaxCalcBreakdownViewBehaviour
import views.html.agent.TaxCalcBreakdown

class TaxCalcBreakdownViewSpec extends TaxCalcBreakdownViewBehaviour {

  override val backUrl = "/report-quarterly/income-and-expenses/view/agent/calculation/2021"

  override def taxCalcBreakdown(calcModel: CalcDisplayModel, taxYear: Int, backUrl: String): Html =
    app.injector.instanceOf[TaxCalcBreakdown].apply(calcModel, taxYear, backUrl)

  override val expectedPageTitle: String = TaxCalcBreakdown.agentTitle

  override val pageContentSelector = "#content"

  override val messageContentSelector = "div.panel-border-wide"

  override val headingSelector = ".heading-secondary"
}
