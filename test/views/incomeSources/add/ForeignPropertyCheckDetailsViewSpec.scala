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

package views.incomeSources.add

import enums.IncomeSourceJourney.ForeignProperty
import models.incomeSourceDetails.viewmodels.CheckForeignPropertyViewModel
import org.jsoup.nodes.Element
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.incomeSources.add.ForeignPropertyCheckDetails

import java.time.LocalDate

class ForeignPropertyCheckDetailsViewSpec extends ViewSpec{

  object ForeignCheckMessages{
    val heading = "Foreign property business you entered"
    val title = "Check your details"
    val cash = "Cash basis accounting"
    val accruals = "Traditional accounting"
    val startDate = "Foreign property business start date"
    val accounting = "Foreign property business accounting method"
    val change = "Change"
    val button = "Confirm and continue"
  }

  val foreignPropertyCheckDetailsView: ForeignPropertyCheckDetails = app.injector.instanceOf[ForeignPropertyCheckDetails]

  val backUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty.key).url
  val agentBackUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty.key).url

  val foreignReportingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show("123").url

  val changeDateLinkIndiv: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.handleRequest(ForeignProperty.key, isAgent = false, isUpdate = true).url
  val changeAccMethodIndiv: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.changeIncomeSourcesAccountingMethod(ForeignProperty.key).url

  val viewModel: CheckForeignPropertyViewModel = CheckForeignPropertyViewModel(LocalDate.of(2023,1,1), "cash")
  val validCallWithSessionDataCash: Html = foreignPropertyCheckDetailsView(viewModel, testCall, isAgent = false, backUrl)

  "Foreign Details Check Details Page" should {
    "Display the correct banner and heading" in new Setup(validCallWithSessionDataCash){
      val title: String = layoutContent.getElementsByClass("govuk-heading-l").text()
      title shouldBe ForeignCheckMessages.title

      val heading: String = layoutContent.getElementsByClass("govuk-caption-l").text()
      heading shouldBe ("This section is " + ForeignCheckMessages.heading)
    }

    "Display the user's start date and accounting method" in new Setup(validCallWithSessionDataCash){
      val tableHeadings: String = layoutContent.getElementsByClass("govuk-summary-list__key").text()
      tableHeadings shouldBe (ForeignCheckMessages.startDate + " " + ForeignCheckMessages.accounting)

      val startDate: String = layoutContent.getElementById("foreign-property-date-value").text()
      startDate shouldBe "1 January 2023"

      val accMethod: String = layoutContent.getElementById("business-accounting-value").text()
      accMethod shouldBe ForeignCheckMessages.cash
    }

    "Display the correct change links and correct button" in new Setup(validCallWithSessionDataCash){
      val dateChangeLink: Element = layoutContent.getElementById("start-date-change-details-link")
      dateChangeLink.hasCorrectHref(changeDateLinkIndiv)
      dateChangeLink.text() shouldBe ForeignCheckMessages.change

      val accChangeLink: Element = layoutContent.getElementById("accounting-method-change-details-link")
      accChangeLink.hasCorrectHref(changeAccMethodIndiv)
      accChangeLink.text() shouldBe ForeignCheckMessages.change

      val button: Element = layoutContent.getElementById("confirm-button")
      button.text() shouldBe ForeignCheckMessages.button
    }
  }
}
