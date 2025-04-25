/*
 * Copyright 2024 HM Revenue & Customs
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

package views.claimToAdjustPoa

import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.test.Helpers.contentAsString
import testUtils.TestSupport
import views.html.claimToAdjustPoa.YouCannotGoBackView

class YouCannotGoBackViewSpec extends TestSupport {

  class Setup(isAgent: Boolean, yourSelfAssessmentChargesFS: Boolean = false) {

    val view: YouCannotGoBackView = app.injector.instanceOf[YouCannotGoBackView]

    val document: Document =
      Jsoup.parse(
        contentAsString(
          view(
            isAgent = isAgent,
            poaTaxYear = TaxYear(2023, 2024),
            yourSelfAssessmentChargesFS
          )
        )
      )
  }

  def getHomeControllerLink(isAgent: Boolean): String = {
    if (isAgent) controllers.routes.HomeController.showAgent().url
    else controllers.routes.HomeController.show().url
  }

  def getWhatYouOweControllerLink(isAgent: Boolean): String = {
    if (isAgent) controllers.routes.WhatYouOweController.showAgent().url
    else controllers.routes.WhatYouOweController.show().url
  }

  def getTaxYearSummaryControllerLink(isAgent: Boolean): String = {
    if (isAgent) controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(2024).url
    else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(2024).url
  }

  def getSaChargesUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.routes.YourSelfAssessmentChargesController.showAgent().url
    else controllers.routes.YourSelfAssessmentChargesController.show().url
  }
  def executeTest(isAgent: Boolean): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: CheckYourAnswersView" should {
      "render the heading" in new Setup(isAgent) {
        document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("claimToAdjustPoa.youCannotGoBack.heading")
      }
      "render the first paragraph" in new Setup(isAgent) {
        document.getElementById("paragraph-text-1").text() shouldBe
          messages("claimToAdjustPoa.youCannotGoBack.para1")
      }
      "render the second paragraph" in new Setup(isAgent) {
        document.getElementById("paragraph-text-2").text() shouldBe
          messages("claimToAdjustPoa.youCannotGoBack.para2")
      }
      "render the first bullet point with the correct link" in new Setup(isAgent) {
        document.getElementsByClass("govuk-!-margin-bottom-4").get(0).text() shouldBe
          messages("claimToAdjustPoa.youCannotGoBack.bullet1Text") + " " + messages("claimToAdjustPoa.youCannotGoBack.bullet1Link")
        document.getElementById("link-1").attr("href") shouldBe getTaxYearSummaryControllerLink(isAgent)
      }
      "render the second bullet point with the correct link" in new Setup(isAgent) {
        document.getElementsByClass("govuk-!-margin-bottom-4").get(1).text() shouldBe
          messages("claimToAdjustPoa.youCannotGoBack.bullet2Text") + " " + messages("claimToAdjustPoa.youCannotGoBack.bullet2Link")
        document.getElementById("link-2").attr("href") shouldBe
          getWhatYouOweControllerLink(isAgent)
      }
      "render the second bullet point with the correct link when yourSelfAssessmentChargesFS is true" in new Setup(isAgent, yourSelfAssessmentChargesFS = true) {
        document.getElementsByClass("govuk-!-margin-bottom-4").get(1).text() shouldBe
          messages("claimToAdjustPoa.youCannotGoBack.bullet2Text") + " " + messages("claimToAdjustPoa.youCannotGoBack.sa.bullet2Link")
        document.getElementById("link-2").attr("href") shouldBe
          getSaChargesUrl(isAgent)
      }
      "render the third bullet point with the correct link" in new Setup(isAgent) {
        document.getElementsByClass("govuk-!-margin-bottom-4").get(2).text() shouldBe
          messages("claimToAdjustPoa.youCannotGoBack.bullet3Text") + " " + messages("claimToAdjustPoa.youCannotGoBack.bullet3Link")
        document.getElementById("link-3").attr("href") shouldBe
          getHomeControllerLink(isAgent)
      }
    }
  }

  executeTest(isAgent = false)
  executeTest(isAgent = true)

}
