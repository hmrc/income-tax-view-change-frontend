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
import views.messages.OptInCompletedViewMessages._


class OptInCompletedViewSpec extends TestSupport {

  val forYearEnd = 2023
  val taxYear22_23: TaxYear = TaxYear.forYearEnd(forYearEnd)

  val view: OptInCompletedView = app.injector.instanceOf[OptInCompletedView]

  def bullet(i: Int) = s"#optin-completed-view > ul > li:nth-child($i)"

  def nextUpdatesLink(origin: Option[String] = None) = controllers.routes.NextUpdatesController.show(origin).url

  def reportingFrequencyLinkUrl(isAgent: Boolean) = controllers.optIn.routes.OptInCompletedController.show(isAgent).url //TODO: Needs fixing/updating

  class SetupForCurrentYear(isAgent: Boolean = true, taxYear: TaxYear,
                            followingYearVoluntary: Boolean,
                            annualWithFollowingYearMandated: Boolean = false) {
    val model: OptInCompletedViewModel =
      OptInCompletedViewModel(
        isAgent = isAgent,
        optInTaxYear = taxYear,
        isCurrentYear = true,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = followingYearVoluntary,
        annualWithFollowingYearMandated = annualWithFollowingYearMandated,
        nextUpdatesLink = controllers.routes.NextUpdatesController.show().url,
        reportingFrequencyLink = reportingFrequencyLinkUrl(isAgent)
      )
    val pageDocument: Document = Jsoup.parse(contentAsString(view(model = model)))
  }

  s"has the correct content for year $taxYear22_23 which is the current year" in new SetupForCurrentYear(false, taxYear22_23, false) {
    pageDocument.title() shouldBe "Opt in completed - Manage your Income Tax updates - GOV.UK"

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe panelTitleContent
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You are now reporting quarterly from 2022 to 2023 tax year onwards"
    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2022 to 2023 tax year, you would have to report quarterly from 6 April 2024."
    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

    pageDocument.getElementById("optin-completed-view-p5").text() shouldBe "You have just chosen to voluntarily report quarterly from the 2022 to 2023 tax year onwards, but in the future it could be mandatory for you if:"
    pageDocument.getElementById("optin-completed-view-p6").text() shouldBe optinCompletedViewP6

  }

  val anotherForYearEnd = 2022
  val taxYear21_22: TaxYear = TaxYear.forYearEnd(anotherForYearEnd)

  s"has the correct content for year $taxYear21_22 which is the current year" in new SetupForCurrentYear(false, taxYear21_22, false) {
    pageDocument.title() shouldBe "Opt in completed - Manage your Income Tax updates - GOV.UK"

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe panelTitleContent

    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe "You are now reporting quarterly from 2021 to 2022 tax year onwards"
    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2021 to 2022 tax year, you would have to report quarterly from 6 April 2023."

    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

    pageDocument.getElementById("optin-completed-view-p5").text() shouldBe
      "You have just chosen to voluntarily report quarterly from the 2021 to 2022 tax year onwards, but in the future it could be mandatory for you if:"

    pageDocument.getElementById("optin-completed-view-p6").text() shouldBe optinCompletedViewP6

  }

  s"has the correct heading for year $taxYear22_23 which is the current year and next year is Voluntary" in new SetupForCurrentYear(false, taxYear22_23, true) {
    pageDocument.title() shouldBe "Opt in completed - Manage your Income Tax updates - GOV.UK"

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe panelTitleContent
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe messages("optin.completedOptIn.followingVoluntary.heading.desc", "2022", "2023")

    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2022 to 2023 tax year, you would have to report quarterly from 6 April 2024."

    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

    pageDocument.getElementById("optin-completed-view-p5").text() shouldBe "You have just chosen to voluntarily report quarterly from the 2022 to 2023 tax year onwards, but in the future it could be mandatory for you if:"
    pageDocument.getElementById("optin-completed-view-p6").text() shouldBe optinCompletedViewP6
  }

  s"has the correct heading for year $taxYear22_23 which is the current year (Annual) and next year is Mandatory" in new SetupForCurrentYear(false, taxYear22_23, true, true) {
    pageDocument.title() shouldBe "Opt in completed - Manage your Income Tax updates - GOV.UK"

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe panelTitleContent
    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe messages("optin.completedOptIn.followingVoluntary.heading.desc", "2022", "2023")

    val expectedText: String = "From 6 April 2023, you’ll be required to send quarterly updates through compatible software."
    pageDocument.getElementById("warning-inset").text() shouldBe expectedText

    pageDocument.getElementById("optin-completed-view-p5").text() shouldBe "You have just chosen to voluntarily report quarterly from the 2022 to 2023 tax year."

    pageDocument.select(bullet(1)).text() shouldBe bullet1Content
    pageDocument.select(bullet(2)).text() shouldBe bullet2Content

    pageDocument.getElementById("optin-completed-view-p6").text() shouldBe optinCompletedViewP6
  }

  class SetupNextYear(
                       isAgent: Boolean = true,
                       taxYear: TaxYear
                     ) {
    val model: OptInCompletedViewModel =
      OptInCompletedViewModel(
        isAgent = isAgent,
        optInTaxYear = taxYear,
        isCurrentYear = false,
        showAnnualReportingAdvice = true,
        optInIncludedNextYear = false,
        annualWithFollowingYearMandated = false,
        nextUpdatesLink = controllers.routes.NextUpdatesController.show().url,
        reportingFrequencyLink = reportingFrequencyLinkUrl(isAgent)
      )

    val pageDocument: Document = Jsoup.parse(contentAsString(view(model = model)))
  }

  s"has the correct content for year $taxYear22_23 which is the next year" in new SetupNextYear(false, taxYear22_23) {

    pageDocument.title() shouldBe titleContent

    pageDocument.getElementsByClass("govuk-panel__title").text() shouldBe panelTitleContent

    pageDocument.getElementsByClass("govuk-panel__body").text() shouldBe panelBodyContent

    pageDocument.getElementById("your-revised-deadline-heading").text() shouldBe yourRevisedDeadlineH2

    pageDocument.getElementById("your-revised-deadline-inset").text() shouldBe yourRevisedDeadlineInset

    pageDocument.getElementById("your-account-has-been-updated").text() shouldBe yourRevisedDeadlineContentP1

    pageDocument.select("#your-account-has-been-updated > a").attr("href") shouldBe nextUpdatesLink()

    pageDocument.getElementById("opt-out-reporting-quarterly").text() shouldBe yourRevisedDeadlineContentP2

    pageDocument.select("#opt-out-reporting-quarterly > a").attr("href") shouldBe reportingFrequencyLinkUrl(false)

    pageDocument.getElementById("optin-completed-view-p3").text() shouldBe optinCompletedViewP3

    pageDocument.getElementById("optin-completed-view-p4").text() shouldBe optinCompletedViewP4

    pageDocument.getElementById("optin-completed-view-p5").text() shouldBe optinCompletedViewP5

    pageDocument.getElementById("optin-completed-view-p6").text() shouldBe optinCompletedViewP6

  }

}
