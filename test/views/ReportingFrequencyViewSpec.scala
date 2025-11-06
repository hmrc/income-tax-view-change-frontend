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
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers._
import services.optout.{OptOutProposition, OptOutTestSupport}
import testUtils.TestSupport
import views.html.ReportingFrequencyView
import views.messages.ReportingFrequencyViewMessages._

import scala.util.Try

class ReportingFrequencyViewSpec extends TestSupport {

  val view: ReportingFrequencyView = app.injector.instanceOf[ReportingFrequencyView]

  def beforeYouStartUrl(isAgent: Boolean): String = controllers.optIn.oldJourney.routes.BeforeYouStartController.show(isAgent).url

  def optOutChooseTaxYearUrl(isAgent: Boolean): String = controllers.optOut.oldJourney.routes.OptOutChooseTaxYearController.show(isAgent).url

  def confirmOptOutUrl(isAgent: Boolean): String = controllers.optOut.oldJourney.routes.ConfirmOptOutController.show(isAgent).url

  val govGuidanceUrl = "https://www.gov.uk/guidance/find-software-thats-compatible-with-making-tax-digital-for-income-tax"

  def nextUpdatesUrl(isAgent: Boolean): String =
    if(isAgent) controllers.routes.NextUpdatesController.showAgent().url
    else controllers.routes.NextUpdatesController.show().url

  val optOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()

  def bullet(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"


  object Selectors {
    val h1 = "page-heading"
    val h2 = "manage-reporting-frequency-heading"
    val p1 = "change-reporting-frequency"
    val p2 = "what-you-can-do"

    val manageReportingObligationsHeading = "manage-reporting-obligations-heading"
    val manageReportingObligationsCardHeading = "manage-reporting-obligations-card-heading-1"
    val manageReportingObligationsCardLink = "manage-reporting-obligations-card-link-1"
    val manageReportingObligationsCardDesc = "manage-reporting-obligations-card-desc-1"
    val manageReportingObligationsCardText = "manage-reporting-obligations-card-text-1"

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

    val differentObligationsH2 = "different-obligations-heading"
    val differentObligationsP1 = "different-obligations-p1"
    val differentObligationsUl = "different-obligations-ul"
    val differentObligationsLi1 = "different-obligations-li1"
    val differentObligationsLi2 = "different-obligations-li2"
    val differentObligationsP2 = "different-obligations-p2"
    val differentObligationsP3 = "different-obligations-updates-and-deadlines"

    val yourObligationsH2 = "your-obligations-heading"
    val yourObligationsP1 = "your-obligations-p1"
    val yourObligationsP2 = "your-obligations-updates-and-deadlines"

    val mandatoryReportingH2 = "mandatory-reporting-heading"
    val mandatoryReportingInset = "mandatory-reporting-inset"
    val mandatoryReportingText = "mandatory-reporting-text"
    val mandatoryReportingText2 = "mandatory-reporting-text-2"
    val mandatoryReportingText3 = "mandatory-reporting-text-3"

    val compatibleSoftwareH2 = "compatible-software-heading"
    val compatibleSoftwareText = "compatible-software-text"
    val compatibleSoftwareText2 = "compatible-software-text-2"

    val govGuidance = "#compatible-software-link"
  }

  def testContentByIds(pageDocument: Document, R17ContentEnabled: Boolean, additionalIdsAndContent: Seq[(String, String)] = Seq(), ceasedBusinesses: Boolean = false, exemptStatus: Boolean = false, noNonExemptStatus: Boolean = false): Unit = {
    val expectedContent: Seq[(String, String)] = {
      if(R17ContentEnabled) {

        val differentObligationsContent = if(ceasedBusinesses || noNonExemptStatus) Seq.empty else
          Seq(
            Selectors.differentObligationsH2 -> differentObligationsHeading,
            Selectors.differentObligationsP1 -> differentObligationsText,
            Selectors.differentObligationsLi1 -> differentObligationsLiOne,
            Selectors.differentObligationsLi2 -> differentObligationsLiTwo,
            Selectors.differentObligationsP2 -> differentObligationsTextTwo,
            Selectors.differentObligationsP3 -> differentObligationsTextThree
          )

        val yourObligationsContent = if (noNonExemptStatus) Seq(
          Selectors.yourObligationsH2 -> yourObligationsHeading,
          Selectors.yourObligationsP1 -> yourObligationsText,
          Selectors.yourObligationsP2 -> yourObligationsTextTwo
        ) else Seq.empty

        val cardContent: Seq[(String, String)] = if (!exemptStatus && !noNonExemptStatus) Seq(
          Selectors.manageReportingObligationsHeading -> manageReportingObligationsHeading,
          Selectors.manageReportingObligationsCardHeading -> manageReportingObligationsCardHeading,
          Selectors.manageReportingObligationsCardLink -> manageReportingObligationsCardLink,
          Selectors.manageReportingObligationsCardDesc -> manageReportingObligationsCardDesc,
          Selectors.manageReportingObligationsCardText -> manageReportingObligationsCardText
        ) else Seq.empty

        val compatSoftware: Seq[(String, String)] = if (!noNonExemptStatus) Seq(
          Selectors.compatibleSoftwareH2 -> compatibleSoftwareHeadingR17,
          Selectors.compatibleSoftwareText -> compatibleSoftwareTextR17,
          Selectors.compatibleSoftwareText2 -> compatibleSoftwareTextThreeR17
        ) else Seq.empty

        val mandatoryReportingContent: Seq[(String, String)] = if (noNonExemptStatus) Seq (
          Selectors.mandatoryReportingText -> mandatoryReportingTextR17OnlyExempt,
          Selectors.mandatoryReportingText2 -> mandatoryReportingTextThreeR17Exempt
        ) else if (exemptStatus) Seq(
          Selectors.mandatoryReportingText -> mandatoryReportingTextR17HasExempt,
          Selectors.mandatoryReportingInset -> mandatoryReportingInsetR17,
          Selectors.mandatoryReportingText2 -> mandatoryReportingTextTwoR17HasExempt,
          Selectors.mandatoryReportingText3 -> mandatoryReportingTextThreeR17Exempt
        ) else Seq(
          Selectors.mandatoryReportingInset -> mandatoryReportingInsetR17,
          Selectors.mandatoryReportingText -> mandatoryReportingTextR17,
          Selectors.mandatoryReportingText2 -> mandatoryReportingTextTwoR17
        )

        Seq(
          Selectors.h1 -> pageHeadingContentNew,
          Selectors.mandatoryReportingH2 -> mandatoryReportingHeadingR17
        ) ++ cardContent ++ mandatoryReportingContent ++ additionalIdsAndContent ++ differentObligationsContent ++ yourObligationsContent ++ compatSoftware


      } else {
        Seq(
          Selectors.h1 -> pageHeadingContent,
          Selectors.h2 -> manageRFHeadingContent,
          Selectors.p1 -> p1Content,
          Selectors.p2 -> p2Content,
          Selectors.mandatoryReportingH2 -> mandatoryReportingHeading,
          Selectors.mandatoryReportingInset -> mandatoryReportingInset,
          Selectors.mandatoryReportingText -> mandatoryReportingText,
          Selectors.compatibleSoftwareH2 -> compatibleSoftwareHeading,
          Selectors.compatibleSoftwareText -> compatibleSoftwareText,
          Selectors.compatibleSoftwareText2 -> compatibleSoftwareTextTwo,
        ) ++ additionalIdsAndContent
      }

    }

    expectedContent.foreach {
      case (selectors, content) =>
        pageDocument.getElementById(selectors).text() shouldBe content
    }
  }

  "ReportingFrequencyView" when {

    "optInOptOutContentUpdateR17 is true" when {

      "the user is an Agent" should {

        "return the correct content when opt in and opt out has single tax year and it is next tax year(2024)" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl
        }

        "return the correct content when opt in and opt out has single tax year and it is not next tax year(2024)" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optInTaxYears = Seq(TaxYear(2023, 2024)),
              itsaStatusTable = Seq(
                ("2023 to 2024", Some("Yes"), Some("Quarterly (mandatory)")),
                ("2024 to 2025", Some("Yes"), Some("Quarterly")),
                ("2025 to 2026", Some("No"), Some("Annual")),
              ),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

          pageDocument.select("#table-head-name-taxyear").text() shouldBe "Tax year"

          pageDocument.select("#table-head-name-using-mtd").text() shouldBe "Using Making Tax Digital for Income Tax"

          pageDocument.select("#table-head-name-status").text() shouldBe "Your status"

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
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = true,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.title() shouldBe titleNew

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl
        }

        "return the correct content when opt in and opt out has multiple tax years" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

        }

        "return the correct content when there is an exempt tax year" in {

          val isAgentFlag = true

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = true,
            previousYearItsaStatus = Voluntary,
            currentYearItsaStatus = Exempt,
            nextYearItsaStatus = Voluntary
          )

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true, exemptStatus = true)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl
        }

        "return the correct content when there is only exempt tax years" in {

          val isAgentFlag = true

          val optOutProposition = OptOutProposition.createOptOutProposition(
            currentYear = TaxYear(2025, 2026),
            previousYearCrystallised = true,
            previousYearItsaStatus = Exempt,
            currentYearItsaStatus = Exempt,
            nextYearItsaStatus = NoStatus
          )

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true, noNonExemptStatus = true)
        }
      }


      "the user is Non-Agent" should {

        "return the correct content when there are latent businesses" in {

          val isAgentFlag = false

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = true,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.title() shouldBe titleNew

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl
        }

        "return the correct content when opt in and opt out has multiple tax years" in {
          val isAgentFlag: Boolean = false

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl
        }

        "return the correct content when opt in and opt out has single tax year and it is next tax year(2024)" in {

          val isAgentFlag = false

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(("2024 to 2025", Some("Yes"), Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag),
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl
        }

        "return the correct content when the user has a ceased business" in {
          val isAgentFlag = false

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
              optInTaxYears = Seq(TaxYear(2023, 2024)),
              itsaStatusTable = Seq(
                ("2023 to 2024", Some("Yes"), Some("Quarterly (mandatory)")),
                ("2024 to 2025", Some("Yes"), Some("Quarterly")),
                ("2025 to 2026", Some("No"), Some("Annual")),
              ),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = true,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = true,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe titleNew

          testContentByIds(pageDocument, R17ContentEnabled = true, ceasedBusinesses = true)

          pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."

          pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses"
          Option(pageDocument.getElementById("different-obligations-heading")) shouldBe None
        }

      }
      "return the correct content when opt in and opt out has single tax year and it is not next tax year(2024)" in {

        val isAgentFlag = false

        val reportingFrequencyViewModel: ReportingFrequencyViewModel =
          ReportingFrequencyViewModel(
            isAgent = isAgentFlag,
            optOutJourneyUrl = Some(optOutChooseTaxYearUrl(isAgentFlag)),
            optInTaxYears = Seq(TaxYear(2023, 2024)),
            itsaStatusTable = Seq(
              ("2023 to 2024", Some("Required"), Some("Quarterly (mandatory)")),
              ("2024 to 2025", Some("Voluntarily signed up"), Some("Quarterly")),
              ("2025 to 2026", Some("Opted out"), Some("Annual")),
            ),
            isAnyOfBusinessLatent = false,
            displayCeasedBusinessWarning = false,
            mtdThreshold = "£50,000",
            proposition = optOutProposition,
            isSignUpEnabled = true,
            isOptOutEnabled = true
          )

        val pageDocument: Document =
          Jsoup.parse(
            contentAsString(
              view.apply(
                viewModel = reportingFrequencyViewModel,
                optInOptOutContentUpdateR17IsEnabled = true,
                nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
              )
            )
          )

        pageDocument.title() shouldBe titleNew

        testContentByIds(pageDocument, R17ContentEnabled = true)

        pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

        pageDocument.select("#table-head-name-taxyear").text() shouldBe "Tax year"

        pageDocument.select("#table-head-name-using-mtd").text() shouldBe "Using Making Tax Digital for Income Tax"

        pageDocument.select("#table-head-name-status").text() shouldBe "Your status"


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
    }

    "optInOptOutContentUpdateR17 is false" when {

      "the user is an Agent" should {

        "return the correct content when opt in and opt out has single tax year and it is next tax year(2024)" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(("2024 to 2025", None, Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = OptOutProposition.createOptOutProposition(TaxYear(2024, 2025), true, Annual, Voluntary, Annual),
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = false,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument, R17ContentEnabled = false)

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
              optInTaxYears = Seq(TaxYear(2023, 2024)),
              itsaStatusTable = Seq(
                ("2023 to 2024", None, Some("Quarterly (mandatory)")),
                ("2024 to 2025", None, Some("Quarterly")),
                ("2025 to 2026", None, Some("Annual")),
              ),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = OptOutProposition.createOptOutProposition(TaxYear(2023, 2024), true, Annual, Voluntary, Annual),
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = false,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument, R17ContentEnabled = false)

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
              optInTaxYears = Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              itsaStatusTable = Seq(("2024 to 2025", None, Some("Quarterly"))),
              displayCeasedBusinessWarning = false,
              isAnyOfBusinessLatent = true,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
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
                  optInOptOutContentUpdateR17IsEnabled = false,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          testContentByIds(pageDocument, R17ContentEnabled = false, latencyDetailsDropdownContent)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

          pageDocument.select(bullet(1)).text() shouldBe optOutGenericContent

          pageDocument.select(bullet(1)).attr("href") shouldBe optOutChooseTaxYearUrl(isAgentFlag)

          pageDocument.select(bullet(2)).text() shouldBe optInGenericContent

          pageDocument.select(bullet(2)).attr("href") shouldBe beforeYouStartUrl(isAgentFlag)
        }

        "return the correct content when the user has a ceased business" in {
          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(optOutChooseTaxYearUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025)),
              Seq(("2024 to 2025", None, Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = true,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = false,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument, R17ContentEnabled = false)

          pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."

          pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/agents/manage-your-businesses"
          Option(pageDocument.getElementById("different-obligations-heading")) shouldBe None
        }

        "return the correct content when opt in and opt out has multiple tax years" in {

          val isAgentFlag = true

          val reportingFrequencyViewModel: ReportingFrequencyViewModel =
            ReportingFrequencyViewModel(
              isAgent = isAgentFlag,
              Some(confirmOptOutUrl(isAgentFlag)),
              Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
              Seq(("2024 to 2025", None, Some("Quarterly"))),
              isAnyOfBusinessLatent = false,
              displayCeasedBusinessWarning = false,
              mtdThreshold = "£50,000",
              proposition = optOutProposition,
              isSignUpEnabled = true,
              isOptOutEnabled = true
            )

          val pageDocument: Document =
            Jsoup.parse(
              contentAsString(
                view.apply(
                  viewModel = reportingFrequencyViewModel,
                  optInOptOutContentUpdateR17IsEnabled = false,
                  nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                )
              )
            )

          pageDocument.title() shouldBe agentTitle

          testContentByIds(pageDocument, R17ContentEnabled = false)

          pageDocument.select(Selectors.govGuidance).attr("href") shouldBe govGuidanceUrl

          pageDocument.select(bullet(1)).text() shouldBe optOutGenericContent

          pageDocument.select(bullet(1)).attr("href") shouldBe confirmOptOutUrl(isAgentFlag)

          pageDocument.select(bullet(2)).text() shouldBe optInGenericContent

          pageDocument.select(bullet(2)).attr("href") shouldBe beforeYouStartUrl(isAgentFlag)


        }

        "the user is Non-Agent" should {

          "return the correct content when opt in and opt out has multiple tax years" in {

            val isAgentFlag = false

            val reportingFrequencyViewModel: ReportingFrequencyViewModel =
              ReportingFrequencyViewModel(
                isAgent = isAgentFlag,
                Some(confirmOptOutUrl(isAgentFlag)),
                Seq(TaxYear(2024, 2025), TaxYear(2025, 2026)),
                Seq(("2024 to 2025", None, Some("Quarterly"))),
                isAnyOfBusinessLatent = false,
                displayCeasedBusinessWarning = false,
                mtdThreshold = "£50,000",
                proposition = optOutProposition,
                isSignUpEnabled = true,
                isOptOutEnabled = true
              )

            val pageDocument: Document =
              Jsoup.parse(
                contentAsString(
                  view.apply(
                    viewModel = reportingFrequencyViewModel,
                    optInOptOutContentUpdateR17IsEnabled = false,
                    nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                  )
                )
              )

            pageDocument.title() shouldBe title

            testContentByIds(pageDocument, R17ContentEnabled = false)

          }

          "return the correct content when the user has a ceased business" in {
            val isAgentFlag = false

            val reportingFrequencyViewModel: ReportingFrequencyViewModel =
              ReportingFrequencyViewModel(
                isAgent = isAgentFlag,
                Some(optOutChooseTaxYearUrl(isAgentFlag)),
                Seq(TaxYear(2024, 2025)),
                Seq(("2024 to 2025", None, Some("Quarterly"))),
                isAnyOfBusinessLatent = false,
                displayCeasedBusinessWarning = true,
                mtdThreshold = "£50,000",
                proposition = optOutProposition,
                isSignUpEnabled = true,
                isOptOutEnabled = true
              )

            val pageDocument: Document =
              Jsoup.parse(
                contentAsString(
                  view.apply(
                    viewModel = reportingFrequencyViewModel,
                    optInOptOutContentUpdateR17IsEnabled = false,
                    nextUpdatesLink = nextUpdatesUrl(isAgentFlag)
                  )
                )
              )

            pageDocument.title() shouldBe title

            testContentByIds(pageDocument, R17ContentEnabled = false)

            pageDocument.getElementById("ceased-business-warning").text() shouldBe "Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."

            pageDocument.getElementById("ceased-business-link").attr("href") shouldBe "/report-quarterly/income-and-expenses/view/manage-your-businesses"
            Option(pageDocument.getElementById("different-obligations-heading")) shouldBe None

          }
        }
      }
    }
  }
}
