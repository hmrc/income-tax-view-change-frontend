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

import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants._
import testUtils.ViewSpec
import views.html.TaxYears

class TaxYearsViewSpec extends ViewSpec {

  val taxYearsView: TaxYears = app.injector.instanceOf[TaxYears]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val taxYearsViewSummary: String = messages("taxYears.viewSummary")
  val earliestYear: Int = 2023
  val previousYear: Int = earliestYear - 1
  val taxYearsOldSaLink: String = s"${messages("taxYears.oldSa.content.link")} ${messages("pagehelp.opensInNewTabText")}"
  val saNote = s"${messages("taxYears.oldSa.content.text", previousYear.toString, earliestYear.toString)} $taxYearsOldSaLink."
  val taxYearsOldSaAgentLink = s"${messages("taxYears.oldSa.agent.content.2")} ${messages("pagehelp.opensInNewTabText")}"
  val saNoteAgent: String = s"${messages("taxYears.oldSa.agent.content.1", previousYear.toString, earliestYear.toString)} $taxYearsOldSaAgentLink. ${messages("taxYears.oldSa.agent.content.3")}"
  val currentTaxYear: (String, String) => String = (year, yearPlusOne) => s"${messages("taxYears.currentTaxYear", year, yearPlusOne)}"
  val taxYear: (String, String) => String = (year, yearPlusOne) => s"${messages("taxYears.taxYears", year, yearPlusOne)}"

  class TestSetup(calcs: List[Int],
              itsaSubmissionFeatureSwitch: Boolean = false,
              isPostFinalisationAmendmentR18Enabled: Boolean = false,
              utr: Option[String] = None, isAgent: Boolean = false) {
    lazy val page: HtmlFormat.Appendable =
      taxYearsView(taxYears = calcs,
        backUrl = "testBackURL",
        utr = utr,
        itsaSubmissionIntegrationEnabled = itsaSubmissionFeatureSwitch,
        isPostFinalisationAmendmentR18Enabled = isPostFinalisationAmendmentR18Enabled,
        earliestSubmissionTaxYear = 2023,
        isAgent = isAgent)(FakeRequest(), implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "individual" when {
    "The TaxYears view with itsaSubmissionFeatureSwitch FS disabled" when {
      "the view is displayed" should {
        s"have the title '${messages("htmlTitle", messages("taxYears.heading"))}'" in new TestSetup(List(testYearPlusOne, testTaxYear)) {
          document.title() shouldBe messages("htmlTitle", messages("taxYears.heading"))
        }

        "have a header" in new TestSetup(List(testYearPlusOne, testTaxYear)) {
          layoutContent.selectHead("h1").text shouldBe messages("taxYears.heading")
        }
      }

      "the user has two tax years" should {
        "display two tax years" in new TestSetup(List(testYearPlusOne, testTaxYear)) {
          document.selectHead("dl div:nth-child(1) dt").text() shouldBe currentTaxYear(testTaxYear.toString, testYearPlusOne.toString)
          document.selectHead("dl div:nth-child(2) dt").text() shouldBe taxYear((testTaxYear - 1).toString, testTaxYear.toString)
        }

        "display two view return links for the correct tax year" in new TestSetup(List(testYearPlusOne, testTaxYear)) {
          document.getElementById("viewSummary-link-2018").text() shouldBe
            s"$taxYearsViewSummary ${taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"
          document.getElementById("viewSummary-link-2019").text() shouldBe
            s"$taxYearsViewSummary ${taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
        }

        "not display any update return link" in new TestSetup(List(testYearPlusOne, testTaxYear)) {
          Option(document.getElementById("updateReturn-link-2018")) shouldBe None
          Option(document.getElementById("updateReturn-link-2019")) shouldBe None
        }
      }

      "the user has three tax years records" should {
        "display three tax years" in new TestSetup(List(testYearPlusTwo, testYearPlusOne, testTaxYear)) {
          document.selectHead("dl div:nth-child(1) dt").text() shouldBe currentTaxYear(testYearPlusOne.toString, testYearPlusTwo.toString)
          document.selectHead("dl div:nth-child(2) dt").text() shouldBe taxYear(testTaxYear.toString, testYearPlusOne.toString)
          document.selectHead("dl div:nth-child(3) dt").text() shouldBe taxYear((testTaxYear - 1).toString, testTaxYear.toString)
        }

        "display three view return links for the correct tax year" in new TestSetup(List(testYearPlusTwo, testYearPlusOne, testTaxYear)) {
          document.getElementById("viewSummary-link-2018").text() shouldBe
            s"$taxYearsViewSummary ${taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"
          document.getElementById("viewSummary-link-2019").text() shouldBe
            s"$taxYearsViewSummary ${taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
          document.getElementById("viewSummary-link-2020").text() shouldBe
            s"$taxYearsViewSummary ${taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)}"
        }

        "not display any update return link" in new TestSetup(List(testYearPlusTwo, testYearPlusOne, testTaxYear)) {
          Option(document.getElementById("updateReturn-link-2018")) shouldBe None
          Option(document.getElementById("updateReturn-link-2019")) shouldBe None
          Option(document.getElementById("updateReturn-link-2020")) shouldBe None
        }
      }

      "the user has no taxYears" should {
        s"have the paragraph '${messages("taxYears.noEstimates")}'" in new TestSetup(List()) {
          document.getElementById("no-taxYears").text shouldBe messages("taxYears.noEstimates")
        }
      }

      "the paragraph explaining about previous Self Assessments" should {
        "appear if the user has a UTR" in new TestSetup(List(testYearPlusOne, testTaxYear), utr = Some("1234567890")) {
          layoutContent.select("#oldSa-para").text shouldBe saNote
          layoutContent.selectFirst("#oldSa-para").hasCorrectLinkWithNewTab(taxYearsOldSaLink, appConfig.saViewLandPService("1234567890"))
        }

        "not appear if the user does not have a UTR" in new TestSetup(List(testYearPlusOne, testTaxYear)) {
          Option(document.selectFirst("#content p")) shouldBe None
        }
      }
    }

    "The TaxYears view with itsaSubmissionFeatureSwitch FS enabled" when {
      "the user has two tax years" should {
        "display two tax years" in new TestSetup(List(testYearPlusOne, testTaxYear), true) {
          document.selectHead("dl div:nth-child(1) dt").text() shouldBe currentTaxYear(testTaxYear.toString, testYearPlusOne.toString)
          document.selectHead("dl div:nth-child(2) dt").text() shouldBe taxYear((testTaxYear - 1).toString, testTaxYear.toString)
        }

        "display two view return links for the correct tax year" in new TestSetup(List(testYearPlusOne, testTaxYear), true) {

          document.getElementById(s"viewSummary-link-$testTaxYear").attr("href") shouldBe
            controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear).url
          document.getElementById(s"viewSummary-link-$testTaxYear").text() shouldBe
            s"$taxYearsViewSummary ${taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"

          document.getElementById(s"viewSummary-link-$testYearPlusOne").attr("href") shouldBe
            controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearPlusOne).url
          document.getElementById(s"viewSummary-link-$testYearPlusOne").text() shouldBe
            s"$taxYearsViewSummary ${taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
        }

        "display two update return links for the correct tax year" in new TestSetup(List(testYearPlusSix, testYearPlusFive), true) {
          document.getElementById("updateReturn-link-2023").text() shouldBe
            s"${messages("taxYears.updateReturn")} ${taxYear((testYearPlusFive - 1).toString, testYearPlusFive.toString)}"
          document.getElementById("updateReturn-link-2024").text() shouldBe
            s"${messages("taxYears.updateReturn")} ${taxYear(testYearPlusFive.toString, testYearPlusSix.toString)}"
        }

        s"display the update return link for the $testYearPlusThree tax year and go to correct link" in new TestSetup(
          List(testYearPlusSix, testYearPlusFive), true) {
          document.getElementById(s"updateReturn-link-$testYearPlusFive").attr("href") shouldBe mockAppConfig.submissionFrontendTaxYearsPage(testYearPlusFive)
        }
      }
    }

    "The TaxYears view with PostFinalisationAmendmentR18 FS disabled" when {
      "the user has multiple tax years" should {
        "not display the amendment guidance text" in new TestSetup(
          calcs = List(testYearPlusOne, testTaxYear),
          isPostFinalisationAmendmentR18Enabled = false
        ) {
          Option(document.getElementById("pfa-amendment-text")) shouldBe None
        }
      }
    }

    "The TaxYears view with PostFinalisationAmendmentR18 FS enabled" when {
      "the user has multiple tax years" should {
        "display the amendment guidance text below the tax years list" in new TestSetup(
          calcs = List(testYearPlusOne, testTaxYear),
          isPostFinalisationAmendmentR18Enabled = true
        ) {
          val amendmentParagraph: Element = document.getElementById("pfa-amendment-text")
          amendmentParagraph.text() shouldBe messages("taxYears.r18.amendment.text")
        }
      }
    }

  }

  "agent" when {
    "display the agent view return link" in new TestSetup(List(testYearPlusOne), true, isAgent = true) {
      document.getElementById(s"viewSummary-link-$testYearPlusOne").attr("href") shouldBe
        controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testYearPlusOne).url
    }
    "the paragraph explaining about previous Self Assessments" in new TestSetup(List(testYearPlusOne), isAgent = true) {
      layoutContent.select("#oldSa-para-agent").text shouldBe saNoteAgent
      layoutContent.selectFirst("#oldSa-para-agent").hasCorrectLinkWithNewTab(
        s"${messages("taxYears.oldSa.agent.content.2")} ${messages("pagehelp.opensInNewTabText")}",
        "https://www.gov.uk/guidance/self-assessment-for-agents-online-service"
      )

    }
  }
}
