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
    val latencyDetailsDropdownPara1 = "separately-choose-to-opt-out"
    val latencyDetailsDropdownBullet1 = "latency-section-1-bullet-1"
    val latencyDetailsDropdownBullet2 = "latency-section-1-bullet-2"
    val latencyDetailsDropdownPara2 = "options-available"
    val latencyDetailsDropdownBullet3 = "latency-section-2-bullet-1"
    val latencyDetailsDropdownBullet4 = "latency-section-2-bullet-2"
    val latencyDetailsDropdownBullet5 = "latency-section-2-bullet-3"
    val latencyDetailsDropdownH3 = "change-reporting-obligations-heading"
    val latencyDetailsDropdownPara3 = "your-businesses"
    val latencyDetailsDropdownPara3Link = "your-businesses-link"
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

    "optInOptOutContentUpdateR17 is true" when {

      "the user is an Agent" should {

        "return the correct content when opt in and opt out has multiple tax years" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optOutTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq("2024 to 2025" -> Some("Quarterly")),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
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
              Seq(TaxYear(2024, 2025)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
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
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optOutTaxYears = Seq(TaxYear(2023, 2024)),
              optInTaxYears = Seq(TaxYear(2023, 2024)),
              itsaStatusTable = Seq(
                "2023 to 2024" -> Some("Quarterly (mandatory)"),
                "2024 to 2025" -> Some("Quarterly"),
                "2025 to 2026" -> Some("Annual"),
              ),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
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

          pageDocument.select("#table-head-name-taxyear").text() shouldBe "Tax year"

          pageDocument.select("#table-head-name-status").text() shouldBe "Reporting frequency"

          val tableTaxYearColumnContent =
            Seq(
              "2023 to 2024",
              "2024 to 2025",
              "2025 to 2026",
            )

          tableTaxYearColumnContent
            .zipWithIndex
            .foreach { case (tableContent, i) =>
              pageDocument.select(s"#table-taxyear-$i").text() shouldBe tableContent
            }

          val tableReportingFrequencyColumnContent =
            Seq(
              "Quarterly (mandatory)",
              "Quarterly",
              "Annual",
            )

          tableReportingFrequencyColumnContent
            .zipWithIndex
            .foreach { case (tableContent, i) =>
              pageDocument.select(s"#table-status-$i").text() shouldBe tableContent
            }
        }

        "return the correct content when there are latent businesses" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optOutTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq("2024 to 2025" -> Some("Quarterly")),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = true
            )


          val latencyDetailsDropdownContent = Seq(
            Selectors.latencyDetailsDropdownPara1 -> "For tax years you are using Making Tax Digital for Income Tax, you can separately choose to opt out for any new sole trader or property income source:",
            Selectors.latencyDetailsDropdownBullet1 -> "started less than 2 years ago",
            Selectors.latencyDetailsDropdownBullet2 -> "that you start in future",
            Selectors.latencyDetailsDropdownPara2 -> "This option is available to your new businesses:",
            Selectors.latencyDetailsDropdownBullet3 -> "for up to 2 tax years",
            Selectors.latencyDetailsDropdownBullet4 -> "only when you use Making Tax Digital for Income Tax for your other businesses",
            Selectors.latencyDetailsDropdownBullet5 -> "even if your total gross income from self-employment or property, or both, exceeds the £50,000 threshold",
            Selectors.latencyDetailsDropdownH3 -> "How to change your reporting obligations for a new income source",
            Selectors.latencyDetailsDropdownPara3 -> "You can do this at any time in the your businesses section.",
            Selectors.latencyDetailsDropdownPara3Link -> "your businesses",
          )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument, latencyDetailsDropdownContent)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

          pageDocument.select(bullet(1)).text() shouldBe optOutGenericContent

          pageDocument.select(bullet(1)).attr("href") shouldBe optOutChooseTaxYearUrl(isAgentFlag)

          pageDocument.select(bullet(2)).text() shouldBe optInGenericContent

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
              Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
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

        "return the correct content when the user has a ceased business" in {
          val isAgentFlag = false

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(TaxYear(2024, 2025)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
                )
              )
            )

          pageDocument.title() shouldBe title

          testContentByIds(pageDocument)

          pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page."

          pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses"
        }

        "return the correct content when the user has a ceased business and is an agent" in {
          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(TaxYear(2024, 2025)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = true
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument)

          pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page."

          pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
        }
      }
    }

    "optInOptOutContentUpdateR17 is false" when {

      "the user is an Agent" should {

        "return the correct content when opt in and opt out has multiple tax years" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optOutTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq("2024 to 2025" -> Some("Quarterly")),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
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
              Seq(TaxYear(2024, 2025)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
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
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optOutTaxYears = Seq(TaxYear(2023, 2024)),
              optInTaxYears = Seq(TaxYear(2023, 2024)),
              itsaStatusTable = Seq(
                "2023 to 2024" -> Some("Quarterly (mandatory)"),
                "2024 to 2025" -> Some("Quarterly"),
                "2025 to 2026" -> Some("Annual"),
              ),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
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

          pageDocument.select("#table-head-name-taxyear").text() shouldBe "Tax year"

          pageDocument.select("#table-head-name-status").text() shouldBe "Reporting frequency"

          val tableTaxYearColumnContent =
            Seq(
              "2023 to 2024",
              "2024 to 2025",
              "2025 to 2026",
            )

          tableTaxYearColumnContent
            .zipWithIndex
            .foreach { case (tableContent, i) =>
              pageDocument.select(s"#table-taxyear-$i").text() shouldBe tableContent
            }

          val tableReportingFrequencyColumnContent =
            Seq(
              "Quarterly (mandatory)",
              "Quarterly",
              "Annual",
            )

          tableReportingFrequencyColumnContent
            .zipWithIndex
            .foreach { case (tableContent, i) =>
              pageDocument.select(s"#table-status-$i").text() shouldBe tableContent
            }
        }

        "return the correct content when there are latent businesses" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optOutTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq("2024 to 2025" -> Some("Quarterly")),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = true
            )


          val latencyDetailsDropdownContent = Seq(
            Selectors.latencyDetailsDropdownPara1 -> "For tax years you report quarterly, you can separately choose to report annually for any new sole trader or property income source:",
            Selectors.latencyDetailsDropdownBullet1 -> "started less than 2 years ago",
            Selectors.latencyDetailsDropdownBullet2 -> "that you start in future",
            Selectors.latencyDetailsDropdownPara2 -> "This option is available to new businesses:",
            Selectors.latencyDetailsDropdownBullet3 -> "for up to 2 tax years",
            Selectors.latencyDetailsDropdownBullet4 -> "only when you report quarterly for your other businesses",
            Selectors.latencyDetailsDropdownBullet5 -> "even if your income from self-employment or property, or both, exceeds the income threshold",
            Selectors.latencyDetailsDropdownH3 -> "How to change a new income source’s reporting frequency",
            Selectors.latencyDetailsDropdownPara3 -> "You can do this at any time in the all businesses section.",
            Selectors.latencyDetailsDropdownPara3Link -> "all businesses",
          )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument, latencyDetailsDropdownContent)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

          pageDocument.select(bullet(1)).text() shouldBe optOutGenericContent

          pageDocument.select(bullet(1)).attr("href") shouldBe optOutChooseTaxYearUrl(isAgentFlag)

          pageDocument.select(bullet(2)).text() shouldBe optInGenericContent

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
              Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
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

        "return the correct content when the user has a ceased business" in {
          val isAgentFlag = false

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(TaxYear(2024, 2025)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
                )
              )
            )

          pageDocument.title() shouldBe title

          testContentByIds(pageDocument)

          pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page."

          pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses"
        }

        "return the correct content when the user has a ceased business and is an agent" in {
          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(TaxYear(2024, 2025)),
              Seq("2024 to 2025" -> Some("Quarterly")),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17 = false
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument)

          pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the all businesses page."

          pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
        }
      }
    }
  }
}
