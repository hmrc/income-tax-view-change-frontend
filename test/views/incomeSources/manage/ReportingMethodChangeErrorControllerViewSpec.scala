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

  private lazy val manageIncomeSourceController = controllers.incomeSources.manage.routes.ManageIncomeSourceController

  val reportingMethodChangeErrorView: ReportingMethodChangeError = app.injector.instanceOf[ReportingMethodChangeError]

  val testBusinessId = "000000"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) {
    lazy val document: Document = {
      Jsoup.parse(
        contentAsString(
          reportingMethodChangeErrorView(
            isAgent = isAgent,
            messagesPrefix = incomeSourceType.reportingMethodChangeErrorPrefix,
            manageIncomeSourceDetailsUrl = getManageIncomeSourceDetailsUrl(isAgent, incomeSourceType),
            manageIncomeSourcesUrl = getManageIncomeSourcesUrl(isAgent)
          )
        )
      )
    }
  }

  def executeTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
    s"${if(isAgent) "Agent" else "Individual"}: ReportingMethodChangeErrorView - $incomeSourceType" should {
      "render the heading" in new Setup(isAgent, incomeSourceType) {
        document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("standardError.heading")
      }
      "render p1 text and link" in new Setup(isAgent, incomeSourceType) {
        document.getElementById("reportingMethodError.p1").text() shouldBe
          messages(s"${incomeSourceType.reportingMethodChangeErrorPrefix}.p1")
      }
      "render p2 text and link" in new Setup(isAgent, incomeSourceType) {
        document.getElementById("reportingMethodError.p2").text().contains(
          messages(s"${incomeSourceType.reportingMethodChangeErrorPrefix}.p2")
        ) shouldBe true
        document.getElementById("reportingMethodError.p2").text().contains(
          messages("incomeSources.manage.reportingMethodError.hyperlink1")
        ) shouldBe true
        document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
          getManageIncomeSourceDetailsUrl(isAgent, incomeSourceType)
      }
      "render p3 text and link" in new Setup(isAgent, incomeSourceType) {
        document.getElementById("reportingMethodError.p3").text().contains(
          messages("incomeSources.manage.reportingMethodError.p3")
        ) shouldBe true
        document.getElementById("reportingMethodError.p3").text().contains(
          messages("incomeSources.manage.reportingMethodError.hyperlink2")
        ) shouldBe true
        document.getElementById("reportingMethodError.p3-link").attr("href") shouldBe
          getManageIncomeSourcesUrl(isAgent)
      }
      "not render the back button" in new Setup(isAgent, incomeSourceType) {
        Option(document.getElementById("back")).isDefined shouldBe false
      }
    }
  }

  def getManageIncomeSourceDetailsUrl(isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, SelfEmployment)  => manageIncomeSourceDetailsController.showSoleTraderBusiness(testBusinessId)
      case (true,  SelfEmployment)  => manageIncomeSourceDetailsController.showSoleTraderBusinessAgent(testBusinessId)
      case (false, UkProperty)      => manageIncomeSourceDetailsController.showUkProperty()
      case (true,  UkProperty)      => manageIncomeSourceDetailsController.showUkPropertyAgent()
      case (false, ForeignProperty) => manageIncomeSourceDetailsController.showForeignProperty()
      case (true,  ForeignProperty) => manageIncomeSourceDetailsController.showForeignPropertyAgent()
    }).url
  }

  def getManageIncomeSourcesUrl(isAgent: Boolean): String = {
    (if(isAgent)manageIncomeSourceController.showAgent() else manageIncomeSourceController.show()).url
  }

  for {
    isAgent <- Seq(false, true)
    incomeSourceType <- Seq(UkProperty, ForeignProperty, SelfEmployment)
  } yield {
    executeTest(isAgent, incomeSourceType)
  }
}
