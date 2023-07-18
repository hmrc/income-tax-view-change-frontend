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

package views.incomeSources.add

import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import org.jsoup.select.Elements
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.incomeSources.add.BusinessAddedObligations

class BusinessAddedObligationsViewSpec extends ViewSpec{

  object BusinessAddedMessages {
    val h1: String = "has been added to your account"
    val h2: String = "What you must do"
  }

  val testId: String = "XAIS00000000001"
  val backUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show(testId).url
  val agentBackUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.showAgent(testId).url

  val businessAddedView: BusinessAddedObligations = app.injector.instanceOf[BusinessAddedObligations]

  val viewModel: ObligationsViewModel = ObligationsViewModel(Seq.empty, Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = true)
  val validCall: Html = businessAddedView("test name", viewModel, testCall, backUrl, isAgent = false)
  val validAgentCall: Html = businessAddedView("test name", viewModel, testCall, agentBackUrl, isAgent = true)

  /*"Business Added Obligations page" should {
    "Display the correct banner message and heading" in new Setup(validCall) {
      val banner: Elements = layoutContent.getElementsByClass("govuk-panel govuk-panel--confirmation")
    }
  }*/

}
