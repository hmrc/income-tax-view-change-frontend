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

package views.optIn

import models.incomeSourceDetails.TaxYear
import models.optin.OptInCompletedViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.OptInCompletedView

class OptInCompletedNextYearViewSpec extends TestSupport {

  val view: OptInCompletedView = app.injector.instanceOf[OptInCompletedView]

  class Setup(isAgent: Boolean = true, taxYear: TaxYear) {
    val model: OptInCompletedViewModel = OptInCompletedViewModel(isAgent = isAgent, optInTaxYear = taxYear, isCurrentYear = false)
    val pageDocument: Document = Jsoup.parse(contentAsString(view(model = model)))
  }

  val forYearEnd = 2023
  val taxYear22_23: TaxYear = TaxYear.forYearEnd(forYearEnd)
  s"have the correct content for year $taxYear22_23" in new Setup(false, taxYear22_23) {
    pageDocument.title() shouldBe "Opt In completed - Manage your Income Tax updates - GOV.UK"

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe "Opt In completed"
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You opted in to quarterly reporting from 2022 to 2023 tax year onwards"

    pageDocument.getElementById("optin-completed-view-p1").text() shouldBe "Check the updates and deadlines page for " +
      "the current tax year’s deadlines. Deadlines for future years will not be visible until they become the current " +
      "year."
    pageDocument.getElementById("optin-completed-view-p2").text() shouldBe "You can decide at any time to opt out of " +
      "reporting quarterly for all your businesses on your reporting frequency page."

    pageDocument.getElementById("optin-completed-view-p3").text() shouldBe "For any tax year you are reporting " +
      "quarterly, you will need software compatible with Making Tax Digital for Income Tax (opens in new tab)."
    pageDocument.getElementById("optin-completed-view-p4").text() shouldBe "When reporting annually, you can submit " +
      "your tax return directly through your HMRC online account or compatible software."

    pageDocument.getElementById("optin-completed-view-p5").text() shouldBe "You are voluntarily opted in to reporting " +
      "quarterly from the next tax year onwards, but in the future it could be mandatory for you if:"
    pageDocument.getElementById("optin-completed-view-p6").text() shouldBe "You can check the threshold for qualifying " +
      "income in the criteria for people who will need to sign up for Making Tax Digital for Income Tax " +
      "(opens in new tab)."

  }

}
