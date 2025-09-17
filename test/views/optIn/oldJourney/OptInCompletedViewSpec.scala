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

package views.optIn.oldJourney

import models.incomeSourceDetails.TaxYear
import models.optin.OptInCompletedViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.optIn.oldJourney.OptInCompletedView
import views.messages.OptInCompletedViewMessages._


class OptInCompletedViewSpec extends TestSupport {

  val taxYear22_23: TaxYear = TaxYear(2022, 2023)

  val view: OptInCompletedView = app.injector.instanceOf[OptInCompletedView]

  def bullet(i: Int) = s"#optin-completed-view > ul > li:nth-child($i)"

  def nextUpdatesLink(origin: Option[String] = None) = controllers.routes.NextUpdatesController.show(origin).url

  def reportingFrequencyLinkUrl(isAgent: Boolean) = controllers.optIn.oldJourney.routes.OptInCompletedController.show(isAgent).url //TODO: Needs fixing/updating

  class SetupForCurrentYear(
                             isAgent: Boolean = true,
                             taxYear: TaxYear,
                             followingYearVoluntary: Boolean,
                             annualWithFollowingYearMandated: Boolean = false
                           ) {
    val model: OptInCompletedViewModel =
      OptInCompletedViewModel(
        isAgent = isAgent,
        optInTaxYear = taxYear,
        isCurrentYear = true,
        showAnnualReportingAdvice = false,
        optInIncludedNextYear = followingYearVoluntary,
        annualWithFollowingYearMandated = annualWithFollowingYearMandated
      )

    val pageDocument: Document = Jsoup.parse(contentAsString(view(model = model)))
  }

  object Selectors {

    val pageTitleClass = "govuk-panel__title"
    val pagePanelBodyClass = "govuk-panel__body"
    val warningInsetId = "warning-inset"
    val warningInsetAnnualFollowingId = "warning-inset-annual-following"

    def paragraphId(i: Int) = s"optin-completed-view-p$i"

    val overdueUpdatesInset = "overdue-updates-inset"

    val yourRevisedDeadlineH2 = "your-revised-deadline-heading"
    val yourRevisedDeadlineInset = "your-revised-deadline-inset"
    val yourRevisedDeadlineP1 = "your-account-has-been-updated"
    val yourRevisedDeadlineP1Link = "#your-account-has-been-updated > a"
    val yourRevisedDeadlineP2 = "opt-out-reporting-quarterly"
    val yourRevisedDeadlineP2Link = "#opt-out-reporting-quarterly > a"
  }

  s"has the correct content for year $taxYear22_23 which is the current year" in new SetupForCurrentYear(false, taxYear22_23, false) {

    pageDocument.title() shouldBe titleContent

    pageDocument.getElementsByClass(Selectors.pageTitleClass).text() shouldBe panelTitleContent
    pageDocument.getElementsByClass(Selectors.pagePanelBodyClass).text() shouldBe "You are now reporting quarterly from 2022 to 2023 tax year onwards"

    pageDocument.getElementById(Selectors.overdueUpdatesInset).text() shouldBe overdueUpdatesGenericInset(taxYear22_23.startYear.toString, taxYear22_23.endYear.toString)

    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2022 to 2023 tax year, you would have to report quarterly from 6 April 2024."

    pageDocument.getElementById(Selectors.warningInsetId).text() shouldBe expectedText

    pageDocument.getElementById(Selectors.paragraphId(5)).text() shouldBe "You have just chosen to voluntarily report quarterly from the 2022 to 2023 tax year onwards, but in the future it could be mandatory for you if:"
    pageDocument.getElementById(Selectors.paragraphId(6)).text() shouldBe optinCompletedViewP6

  }

  val anotherForYearEnd = 2022
  val taxYear21_22: TaxYear = TaxYear.forYearEnd(anotherForYearEnd)

  s"has the correct content for year $taxYear21_22 which is the current year" in new SetupForCurrentYear(false, taxYear21_22, false) {

    pageDocument.title() shouldBe titleContent

    pageDocument.getElementsByClass(Selectors.pageTitleClass).text() shouldBe panelTitleContent

    pageDocument.getElementById(Selectors.overdueUpdatesInset).text() shouldBe overdueUpdatesGenericInset(taxYear21_22.startYear.toString, taxYear21_22.endYear.toString)

    pageDocument.getElementsByClass(Selectors.pagePanelBodyClass).text() shouldBe "You are now reporting quarterly from 2021 to 2022 tax year onwards"
    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2021 to 2022 tax year, you would have to report quarterly from 6 April 2023."

    pageDocument.getElementById(Selectors.warningInsetId).text() shouldBe expectedText

    pageDocument.getElementById(Selectors.paragraphId(5)).text() shouldBe
      "You have just chosen to voluntarily report quarterly from the 2021 to 2022 tax year onwards, but in the future it could be mandatory for you if:"

    pageDocument.getElementById(Selectors.paragraphId(6)).text() shouldBe optinCompletedViewP6

  }

  s"has the correct heading for year $taxYear22_23 which is the current year and next year is Voluntary" in new SetupForCurrentYear(false, taxYear22_23, true) {

    pageDocument.title() shouldBe titleContent

    pageDocument.getElementById(Selectors.overdueUpdatesInset).text() shouldBe overdueUpdatesGenericInset(taxYear22_23.startYear.toString, taxYear22_23.endYear.toString)

    pageDocument.getElementsByClass(Selectors.pageTitleClass).text() shouldBe panelTitleContent
    pageDocument.getElementsByClass(Selectors.pagePanelBodyClass).text() shouldBe messages("optin.completedOptIn.followingVoluntary.heading.desc", "2022", "2023")

    val expectedText: String = "For example, if your income from self-employment or property, or both, exceeds the threshold " +
      "in the 2022 to 2023 tax year, you would have to report quarterly from 6 April 2024."

    pageDocument.getElementById(Selectors.warningInsetId).text() shouldBe expectedText

    pageDocument.getElementById(Selectors.paragraphId(5)).text() shouldBe "You have just chosen to voluntarily report quarterly from the 2022 to 2023 tax year onwards, but in the future it could be mandatory for you if:"
    pageDocument.getElementById(Selectors.paragraphId(6)).text() shouldBe optinCompletedViewP6
  }

  s"has the correct heading for year $taxYear22_23 which is the current year (Annual) and next year is Mandatory" in
    new SetupForCurrentYear(false, taxYear22_23, true, true) {

      pageDocument.title() shouldBe titleContent

      pageDocument.getElementById(Selectors.overdueUpdatesInset).text() shouldBe overdueUpdatesGenericInset(taxYear22_23.startYear.toString, taxYear22_23.endYear.toString)

      pageDocument.getElementsByClass(Selectors.pageTitleClass).text() shouldBe panelTitleContent
      pageDocument.getElementsByClass(Selectors.pagePanelBodyClass).text() shouldBe messages("optin.completedOptIn.followingVoluntary.heading.desc", "2022", "2023")

      val expectedText: String = "From 6 April 2023, you’ll be required to send quarterly updates through compatible software."
      pageDocument.getElementById(Selectors.warningInsetAnnualFollowingId).text() shouldBe expectedText

      Option(pageDocument.getElementById(Selectors.warningInsetId)) shouldBe None

      pageDocument.getElementById(Selectors.paragraphId(5)).text() shouldBe "You have just chosen to voluntarily report quarterly from the 2022 to 2023 tax year."

      pageDocument.select(bullet(1)).text() shouldBe bullet1Content
      pageDocument.select(bullet(2)).text() shouldBe bullet2Content

      pageDocument.getElementById(Selectors.paragraphId(6)).text() shouldBe optinCompletedViewP6
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
        annualWithFollowingYearMandated = false
      )

    val pageDocument: Document = Jsoup.parse(contentAsString(view(model = model)))
  }

  "has the correct content for tax year 2022-2023 which is the next tax year" in
    new SetupNextYear(isAgent = false, taxYear = taxYear22_23) {

      def testContentByIds(idsAndContent: Seq[(String, String)]): Unit =
        idsAndContent.foreach {
          case (selectors, content) =>
            pageDocument.getElementById(selectors).text() shouldBe content
        }

      val expectedContent: Seq[(String, String)] =
        Seq(
          Selectors.yourRevisedDeadlineH2 -> yourRevisedDeadlineH2,
          Selectors.yourRevisedDeadlineInset -> yourRevisedDeadlineInset,
          Selectors.yourRevisedDeadlineP1 -> yourRevisedDeadlineContentP1,
          Selectors.yourRevisedDeadlineP2 -> yourRevisedDeadlineContentP2,
          Selectors.paragraphId(3) -> optinCompletedViewP3,
          Selectors.paragraphId(4) -> optinCompletedViewP4,
          Selectors.paragraphId(5) -> optinCompletedViewP5,
          Selectors.paragraphId(6) -> optinCompletedViewP6,
        )

      pageDocument.title() shouldBe titleContent

//      pageDocument.getElementById(Selectors.overdueUpdatesInset).text() shouldBe overdueUpdatesGenericInset

      pageDocument.getElementsByClass(Selectors.pageTitleClass).text() shouldBe panelTitleContent

      pageDocument.getElementsByClass(Selectors.pagePanelBodyClass).text() shouldBe panelBodyContent

      testContentByIds(expectedContent)

      pageDocument.select(Selectors.yourRevisedDeadlineP1Link).attr("href") shouldBe nextUpdatesLink()

      pageDocument.select(Selectors.yourRevisedDeadlineP2Link).attr("href") shouldBe reportingFrequencyLinkUrl(false)
    }

}
