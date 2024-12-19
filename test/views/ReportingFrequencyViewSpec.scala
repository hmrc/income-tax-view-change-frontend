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

package views

import models.ReportingFrequencyViewModel
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import testUtils.TestSupport
import views.html.ReportingFrequencyView
import views.messages.ReportingFrequencyViewMessages._


class ReportingFrequencyViewSpec extends TestSupport {

  val view: ReportingFrequencyView = app.injector.instanceOf[ReportingFrequencyView]

  def beforeYouStartUrl(isAgent: Boolean): String = controllers.optIn.routes.BeforeYouStartController.show(isAgent).url

  def optOutChooseTaxYearUrl(isAgent: Boolean): String = controllers.optOut.routes.OptOutChooseTaxYearController.show(isAgent).url

  def confirmOptOutUrl(isAgent: Boolean): String = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url

  val govGuidanceUrl = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  def bullet(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"

  object Selectors {
    val h1 = "reporting-frequency-heading"
    val h2 = "manage-reporting-frequency-heading"
    val p1 = "change-reporting-frequency"
    val p2 = "what-you-can-do"
    val mandatoryReportingH2 = "mandatory-reporting-heading"
    val mandatoryReportingInset = "mandatory-reporting-inset"
    val mandatoryReportingLink = "mandatory-reporting-link"
    val mandatoryReportingText = "mandatory-reporting-text"
    val compatibleSoftwareH2 = "compatible-software-heading"
    val compatibleSoftwareP1 = "compatible-software-text"
    val compatibleSoftwareP2 = "compatible-software-text-2"
    val govGuidance = "#compatible-software-link"
  }

  def testContentByIds(pageDocument: Document, additionalIdsAndContent: Seq[(String, String)] = Seq()): Unit = {
    val expectedContent: Seq[(String, String)] =
      Seq(
        Selectors.h1 -> h1Content,
        Selectors.h2 -> h2Content,
        Selectors.p1 -> p1Content,
        Selectors.p2 -> p2Content,
        Selectors.mandatoryReportingH2 -> mandatoryReportingHeading,
        Selectors.mandatoryReportingInset -> mandatoryReportingInset,
        Selectors.mandatoryReportingLink -> mandatoryReportingLink,
        Selectors.mandatoryReportingText -> mandatoryReportingText,
        Selectors.compatibleSoftwareH2 -> compatibleSoftwareH2,
        Selectors.compatibleSoftwareP1 -> compatibleSoftwareP1,
        Selectors.compatibleSoftwareP2 -> compatibleSoftwareP2
      ) ++ additionalIdsAndContent

    expectedContent.foreach {
      case (selectors, content) =>
        pageDocument.getElementById(selectors).text() shouldBe content
    }
  }

  "ReportingFrequencyView" when {

    "the user is an Agent" should {

      "return the correct content when opt in and opt out has multiple tax years" in {

        val isAgentFlag = true

        val reportingFrequencyViewModel: ReportingFrequencyViewModel =
          ReportingFrequencyViewModel(
            isAgent = isAgentFlag,
            Some(optOutChooseTaxYearUrl(isAgentFlag)),
            Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
            Seq(TaxYear(2024, 2025), TaxYear(2025, 2026))
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                viewModel = reportingFrequencyViewModel
              )
            )
          )

        pageDocument.title() shouldBe agentTitle

        testContentByIds(pageDocument)

        pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

        pageDocument.select(bullet(1)).text() shouldBe optOutGenericContent

        pageDocument.select(bullet(1)).attr("href") shouldBe optOutChooseTaxYearUrl(isAgentFlag)

        pageDocument.select(bullet(2)).text() shouldBe optInGenericContent

        pageDocument.select(bullet(2)).attr("href") shouldBe beforeYouStartUrl(isAgentFlag)
      }

      "return the correct content when opt in and opt out has single tax year and it is next tax year(2024)" in {

        val isAgentFlag = true

        val reportingFrequencyViewModel: ReportingFrequencyViewModel =
          ReportingFrequencyViewModel(
            isAgent = isAgentFlag,
            Some(optOutChooseTaxYearUrl(isAgentFlag)),
            Seq(TaxYear(2024, 2025)),
            Seq(TaxYear(2024, 2025))
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                viewModel = reportingFrequencyViewModel
              )
            )
          )

        pageDocument.title() shouldBe agentTitle

        testContentByIds(pageDocument)

        pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

        pageDocument.select(bullet(1)).text() shouldBe optOutContentWithTaxYearOnwards

        pageDocument.select(bullet(1)).attr("href") shouldBe optOutChooseTaxYearUrl(isAgentFlag)

        pageDocument.select(bullet(2)).text() shouldBe optInContentWithTaxYearOnwards

        pageDocument.select(bullet(2)).attr("href") shouldBe beforeYouStartUrl(isAgentFlag)
      }

      "return the correct content when opt in and opt out has single tax year and it is not next tax year(2024)" in {

        val isAgentFlag = true

        val reportingFrequencyViewModel: ReportingFrequencyViewModel =
          ReportingFrequencyViewModel(
            isAgent = isAgentFlag,
            Some(optOutChooseTaxYearUrl(isAgentFlag)),
            Seq(TaxYear(2023, 2024)),
            Seq(TaxYear(2023, 2024))
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                viewModel = reportingFrequencyViewModel
              )
            )
          )

        pageDocument.title() shouldBe agentTitle

        testContentByIds(pageDocument)

        pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

        pageDocument.select(bullet(1)).text() shouldBe optOutContentWithTaxYear

        pageDocument.select(bullet(1)).attr("href") shouldBe optOutChooseTaxYearUrl(isAgentFlag)

        pageDocument.select(bullet(2)).text() shouldBe optInContentWithTaxYear

        pageDocument.select(bullet(2)).attr("href") shouldBe beforeYouStartUrl(isAgentFlag)
      }
    }

    "the user is Non-Agent" should {

      "return the correct content when opt in and opt out has multiple tax years" in {

        val isAgentFlag = false

        val reportingFrequencyViewModel: ReportingFrequencyViewModel =
          ReportingFrequencyViewModel(
            isAgent = isAgentFlag,
            Some(confirmOptOutUrl(isAgentFlag)),
            Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
            Seq(TaxYear(2024, 2025), TaxYear(2025, 2026))
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                viewModel = reportingFrequencyViewModel
              )
            )
          )

        pageDocument.title() shouldBe title

        testContentByIds(pageDocument)

        pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

        pageDocument.select(bullet(1)).text() shouldBe optOutGenericContent

        pageDocument.select(bullet(1)).attr("href") shouldBe confirmOptOutUrl(isAgentFlag)

        pageDocument.select(bullet(2)).text() shouldBe optInGenericContent

        pageDocument.select(bullet(2)).attr("href") shouldBe beforeYouStartUrl(isAgentFlag)
      }
    }
  }
}
