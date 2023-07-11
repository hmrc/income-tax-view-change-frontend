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

import forms.incomeSources.add.CheckUKPropertyStartDateForm
import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testConstants.BaseTestConstants.testNavHtml
import testUtils.TestSupport
import views.html.incomeSources.add.CheckBusinessDetails

import java.time.LocalDate

class CheckBusinessDetailsViewSpec extends TestSupport {

  val checkBusinessDetailsView: CheckBusinessDetails = app.injector.instanceOf[CheckBusinessDetails]

  val viewModelMax: CheckBusinessDetailsViewModel = CheckBusinessDetailsViewModel(
    businessName = Some("Test Business"),
    businessStartDate = Some(LocalDate.of(2022, 1, 1)),
    businessTrade = "Test Trade",
    businessAddressLine1 = "Test Business Address Line 1",
    businessPostalCode = Some("Test Business Postal Code"),
    businessAccountingMethod = Some("Test Accounting Method"),
    accountingPeriodEndDate = LocalDate.of(2022, 1, 1),
    businessAddressLine2 = None,
    businessAddressLine3 = None,
    businessAddressLine4 = None,
    businessCountryCode = Some("UK"),
    cashOrAccrualsFlag = "Cash",
    skippedAccountingMethod = true
  )

  val postAction: Call = {
      controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit()
  }
  class Setup(isAgent: Boolean, error: Boolean = false) {

    val businessName = "Test Business"
    val businessStartDate = "1 January 2022"
    val businessTrade = "Test Trade"
    val businessAddressLine1 = "Test Business Address Line 1"
    val businessPostalCode = "Test Business Postal Code"
    val businessAccountingMethod = "Traditional accounting"

    val backUrl: String = if (isAgent) controllers.routes.HomeController.showAgent.url else
      controllers.routes.HomeController.show().url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submitAgent() else
      controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.submit()


    lazy val view: HtmlFormat.Appendable = {
      checkBusinessDetailsView(
        viewModelMax,
        isAgent = isAgent,
        postAction = postAction,
        backUrl = backUrl)(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "CheckBusinessDetails - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-heading-l").text() shouldBe messages("check-business-details.heading")
    }
    "render the business name from session storage" in new Setup(false) {
      document.getElementById("business-name-value").text shouldBe businessName
    }
    "render the summary list" in new Setup(false) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("check-business-details.business-name")
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("check-business-details.business-start-date")
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("check-business-details.business-description")
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("check-business-details.business-address")
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe messages("check-business-details.accounting-method")

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe businessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe businessTrade
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe businessAddressLine1 + " " + businessPostalCode
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe businessAccountingMethod

    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-and-continue")
    }
    "render the back url" in new Setup(false, true) {
      document.getElementById("back").attr("href") shouldBe backUrl
    }
  }

  "CheckUKPropertyStartDate - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-heading-l").text() shouldBe messages("check-business-details.heading")
    }
    "render the business name from session storage" in new Setup(true) {
      document.getElementById("business-name-value").text shouldBe businessName
    }
    "render the summary list" in new Setup(false) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("check-business-details.business-name")
      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("check-business-details.business-start-date")
      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("check-business-details.business-description")
      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("check-business-details.business-address")
      document.getElementsByClass("govuk-summary-list__key").eq(4).text() shouldBe messages("check-business-details.accounting-method")

      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe businessName
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe businessStartDate
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe businessTrade
      document.getElementsByClass("govuk-summary-list__value").eq(3).text() shouldBe businessAddressLine1 + " " + businessPostalCode
      document.getElementsByClass("govuk-summary-list__value").eq(4).text() shouldBe businessAccountingMethod


      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("check-business-details.change-details-link")
      document.getElementsByClass("govuk-summary-list__actions").eq(1).text() shouldBe messages("check-business-details.change-details-link")
      document.getElementsByClass("govuk-summary-list__actions").eq(2).text() shouldBe messages("check-business-details.change-details-link")
      document.getElementsByClass("govuk-summary-list__actions").eq(3).text() shouldBe messages("check-business-details.change-details-link")
      document.getElementsByClass("govuk-summary-list__actions").eq(4).text() shouldBe messages("check-business-details.change-details-link")

    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(true) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-and-continue")
    }
    "render the back url" in new Setup(true, true) {
      document.getElementById("back").attr("href") shouldBe backUrl
    }
  }
}
