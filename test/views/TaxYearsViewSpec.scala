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

package views

import config.FrontendAppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants._
import testConstants.MessagesLookUp.{TaxYears => taxYears}
import testUtils.ViewSpec
import views.html.TaxYears

class TaxYearsViewSpec extends ViewSpec {

  val taxYearsView: TaxYears = app.injector.instanceOf[TaxYears]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  class Setup(calcs: List[Int],
                        itsaSubmissionFeatureSwitch: Boolean = false,
                        utr: Option[String] = None, isAgent: Boolean = false)  {
    lazy val page: HtmlFormat.Appendable =
      taxYearsView(calcs, "testBackURL", utr, itsaSubmissionFeatureSwitch, isAgent = isAgent)(FakeRequest(),implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "individual" when {
    "The TaxYears view with itsaSubmissionFeatureSwitch FS disabled" when {
      "the view is displayed" should {
        s"have the title '${taxYears.title}'" in new Setup(List(testYearPlusOne, testTaxYear)) {
          document.title() shouldBe taxYears.title
        }

        "have a header" in new Setup(List(testYearPlusOne, testTaxYear)) {
          layoutContent.selectHead("h1").text shouldBe taxYears.heading
        }
      }

      "the user has two tax years" should {
        "display two tax years" in new Setup(List(testYearPlusOne, testTaxYear)) {
          document.selectHead("dl div:nth-child(1) dt").text() shouldBe taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)
          document.selectHead("dl div:nth-child(2) dt").text() shouldBe taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)
        }

        "display two view return links for the correct tax year" in new Setup(List(testYearPlusOne, testTaxYear)) {
          document.getElementById("viewSummary-link-2018").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"
          document.getElementById("viewSummary-link-2019").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
        }

        "not display any update return link" in new Setup(List(testYearPlusOne, testTaxYear)) {
          Option(document.getElementById("updateReturn-link-2018")) shouldBe None
          Option(document.getElementById("updateReturn-link-2019")) shouldBe None
        }
      }

      "the user has three tax years records" should {
        "display three tax years" in new Setup(List(testYearPlusTwo, testYearPlusOne, testTaxYear)) {
          document.selectHead("dl div:nth-child(1) dt").text() shouldBe taxYears.taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)
          document.selectHead("dl div:nth-child(2) dt").text() shouldBe taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)
          document.selectHead("dl div:nth-child(3) dt").text() shouldBe taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)
        }

        "display three view return links for the correct tax year" in new Setup(List(testYearPlusTwo, testYearPlusOne, testTaxYear)) {
          document.getElementById("viewSummary-link-2018").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"
          document.getElementById("viewSummary-link-2019").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
          document.getElementById("viewSummary-link-2020").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear(testYearPlusOne.toString, testYearPlusTwo.toString)}"
        }

        "not display any update return link" in new Setup(List(testYearPlusTwo, testYearPlusOne, testTaxYear)) {
          Option(document.getElementById("updateReturn-link-2018")) shouldBe None
          Option(document.getElementById("updateReturn-link-2019")) shouldBe None
          Option(document.getElementById("updateReturn-link-2020")) shouldBe None
        }
      }

      "the user has no taxYears" should {
        s"have the paragraph '${taxYears.noEstimates}'" in new Setup(List()) {
          document.getElementById("no-taxYears").text shouldBe taxYears.noEstimates
        }
      }

      "the paragraph explaining about previous Self Assessments" should {
        "appear if the user has a UTR" in new Setup(List(testYearPlusOne, testTaxYear), utr = Some("1234567890")) {
          layoutContent.select("#oldSa-para").text shouldBe taxYears.saNote
          layoutContent.selectFirst("#oldSa-para").hasCorrectLinkWithNewTab(taxYears.saLink, appConfig.saViewLandPService("1234567890"))
        }

        "not appear if the user does not have a UTR" in new Setup(List(testYearPlusOne, testTaxYear)) {
          Option(document.selectFirst("#content p")) shouldBe None
        }
      }
    }

    "The TaxYears view with itsaSubmissionFeatureSwitch FS enabled" when {
      "the user has two tax years" should {
        "display two tax years" in new Setup(List(testYearPlusOne, testTaxYear), true) {
          document.selectHead("dl div:nth-child(1) dt").text() shouldBe taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)
          document.selectHead("dl div:nth-child(2) dt").text() shouldBe taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)
        }

        "display two view return links for the correct tax year" in new Setup(List(testYearPlusOne, testTaxYear), true) {

          document.getElementById(s"viewSummary-link-$testTaxYear").attr("href") shouldBe
            controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testTaxYear).url
          document.getElementById(s"viewSummary-link-$testTaxYear").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"

          document.getElementById(s"viewSummary-link-$testYearPlusOne").attr("href") shouldBe
            controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearPlusOne).url
          document.getElementById(s"viewSummary-link-$testYearPlusOne").text() shouldBe
            s"${taxYears.viewSummary} ${taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
        }

        "display two update return links for the correct tax year" in new Setup(List(testYearPlusOne, testTaxYear), true) {
          document.getElementById("updateReturn-link-2018").text() shouldBe
            s"${taxYears.updateReturn} ${taxYears.taxYear((testTaxYear - 1).toString, testTaxYear.toString)}"
          document.getElementById("updateReturn-link-2019").text() shouldBe
            s"${taxYears.updateReturn} ${taxYears.taxYear(testTaxYear.toString, testYearPlusOne.toString)}"
        }

        s"display the update return link for the $testYearPlusThree tax year and go to correct link" in new Setup(
          List(testYearPlusFour, testYearPlusThree), true) {
          document.getElementById(s"updateReturn-link-$testYearPlusThree").attr("href") shouldBe mockAppConfig.submissionFrontendTaxYearsPage(testYearPlusThree)
        }

        "display the update return link for the 2020 tax year and go to correct link" in new Setup(List(testYearPlusThree, testYearPlusTwo), true) {
          document.getElementById(s"updateReturn-link-$testYearPlusTwo").attr("href") shouldBe mockAppConfig.submissionFrontendTaxYearsPage(testYearPlusTwo)
        }

        "display the update return link for the 2019 tax year and go to correct link" in new Setup(List(testYearPlusOne, testTaxYear), true) {
          document.getElementById(s"updateReturn-link-$testYearPlusOne").attr("href") shouldBe mockAppConfig.submissionFrontendTaxYearsPage(testYearPlusOne)
        }
      }
    }
  }

  "agent" when {
    "display the agent view return link" in new Setup(List(testYearPlusOne), true, isAgent = true) {
      document.getElementById(s"viewSummary-link-$testYearPlusOne").attr("href") shouldBe
        controllers.agent.routes.TaxYearOverviewController.show(testYearPlusOne).url
    }
    "the paragraph explaining about previous Self Assessments" in new Setup(List(testYearPlusOne), isAgent = true) {
      layoutContent.select("#oldSa-para-agent").text shouldBe taxYears.saNoteAgent
      layoutContent.selectFirst("#oldSa-para-agent").hasCorrectLinkWithNewTab(taxYears.saLinkAgent, appConfig.saForAgents)
    }
  }
}
