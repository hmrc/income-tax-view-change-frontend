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

package views.incomeSources.manage

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.incomeSources.manage.ReportingMethodChangeError

class ReportingMethodChangeErrorControllerViewSpec extends TestSupport {

  private lazy val manageIncomeSourceDetailsController = controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController

  val reportingMethodChangeErrorView: ReportingMethodChangeError = app.injector.instanceOf[ReportingMethodChangeError]

  val testBusinessId = "000000"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {
    lazy val document: Document = {
      Jsoup.parse(
        contentAsString(
          reportingMethodChangeErrorView(
            messagesPrefix = incomeSourceType.reportingMethodChangeErrorPrefix,
            continueUrl = ((isAgent, incomeSourceType) match {
              case (false, SelfEmployment) => manageIncomeSourceDetailsController.showSoleTraderBusiness(testBusinessId)
              case (true, SelfEmployment) => manageIncomeSourceDetailsController.showSoleTraderBusinessAgent(testBusinessId)
              case (false, UkProperty) => manageIncomeSourceDetailsController.showUkProperty()
              case (true, UkProperty) => manageIncomeSourceDetailsController.showUkPropertyAgent()
              case (false, ForeignProperty) => manageIncomeSourceDetailsController.showForeignProperty()
              case (true, ForeignProperty) => manageIncomeSourceDetailsController.showForeignPropertyAgent()
            }).url,
            isAgent = isAgent
          )
        )
      )
    }
  }

  "Individual: ReportingMethodChangeErrorView - UK Property" should {
    "render the heading" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
    }
    "render the paragraph text" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById(s"${UkProperty.reportingMethodChangeErrorPrefix}.text").text() shouldBe
        messages(s"${UkProperty.reportingMethodChangeErrorPrefix}.text")
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementById("continue-button").attr("href") shouldBe
        controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showUkProperty().url
    }
    "not render the back button" in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
  "Individual: ReportingMethodChangeErrorView - Foreign Property" should {
    "render the heading" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
    }
    "render the paragraph text" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById(s"${ForeignProperty.reportingMethodChangeErrorPrefix}.text").text() shouldBe
        messages(s"${ForeignProperty.reportingMethodChangeErrorPrefix}.text")
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").attr("href") shouldBe
        controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showForeignProperty().url
    }
    "not render the back button" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
  "Individual: ReportingMethodChangeErrorView - Sole Trader Business" should {
    "render the heading" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
    }
    "render the paragraph text" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById(s"${SelfEmployment.reportingMethodChangeErrorPrefix}.text").text() shouldBe
        messages(s"${SelfEmployment.reportingMethodChangeErrorPrefix}.text")
    }
    "render the continue button" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").attr("href") shouldBe
        controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(testBusinessId).url
    }
    "not render the back button" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }

  "Agent: ReportingMethodChangeErrorView - UK Property" should {
    "render the heading" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
    }
    "render the paragraph text" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById(s"${UkProperty.reportingMethodChangeErrorPrefix}.text").text() shouldBe
        messages(s"${UkProperty.reportingMethodChangeErrorPrefix}.text")
    }
    "render the continue button" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementById("continue-button").attr("href") shouldBe
        controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showUkPropertyAgent().url
    }
    "not render the back button" in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
  "Agent: ReportingMethodChangeErrorView - Foreign Property" should {
    "render the heading" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
    }
    "render the paragraph text" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById(s"${ForeignProperty.reportingMethodChangeErrorPrefix}.text").text() shouldBe
        messages(s"${ForeignProperty.reportingMethodChangeErrorPrefix}.text")
    }
    "render the continue button" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementById("continue-button").attr("href") shouldBe
        controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent().url
    }
    "not render the back button" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
  "Agent: ReportingMethodChangeErrorView - Sole Trader Business" should {
    "render the heading" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
    }
    "render the paragraph text" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById(s"${SelfEmployment.reportingMethodChangeErrorPrefix}.text").text() shouldBe
        messages(s"${SelfEmployment.reportingMethodChangeErrorPrefix}.text")
    }
    "render the continue button" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementById("continue-button").attr("href") shouldBe
        controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(testBusinessId).url
    }
    "not render the back button" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      Option(document.getElementById("back")).isDefined shouldBe false
    }
  }
}
