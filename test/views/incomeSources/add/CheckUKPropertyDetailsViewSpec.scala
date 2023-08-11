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

import implicits.ImplicitDateFormatter
import models.incomeSourceDetails.viewmodels.CheckUKPropertyViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.incomeSources.add.CheckUKPropertyDetails

import java.time.LocalDate

class CheckUKPropertyDetailsViewSpec extends TestSupport with ImplicitDateFormatter {

  val checkUKPropertyDetailsView: CheckUKPropertyDetails = app.injector.instanceOf[CheckUKPropertyDetails]

  val cashOrAccrualsFlag = "CASH"
  val cash = messages("incomeSources.add.accountingMethod.cash")
  val continue = messages("incomeSources.add.checkUKPropertyDetails.confirm")
  val tradingStartDate = LocalDate.parse("2023-05-01")
  val viewModel = CheckUKPropertyViewModel(tradingStartDate, cashOrAccrualsFlag)
  val heading = messages("incomeSources.add.checkUKPropertyDetails.heading")
  val caption = messages("incomeSources.add.checkUKPropertyDetails.caption")
  val startDateLabel = messages("incomeSources.add.checkUKPropertyDetails.startDateLabel")
  val accountingMethodLabel = messages("incomeSources.add.checkUKPropertyDetails.accountingMethodLabel")
  val change = messages("incomeSources.add.checkUKPropertyDetails.change")


  class Setup(isAgent: Boolean, error: Boolean = false) {
    val postAction: String = if (isAgent) {
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submitAgent().url
    } else {
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.submit().url
    }
    val backUrl: Call = if (isAgent) {
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent()
    } else {
      controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.show()
    }
    val changeStartDateUrl = if (isAgent) {
      controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.changeUKPropertyStartDateAgent.url
    } else {
      controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.changeUKPropertyStartDate.url
    }

    val changeAccountingMethodUrl = if (isAgent) {
      controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.changeAgent().url
    } else {
      controllers.incomeSources.add.routes.UKPropertyAccountingMethodController.change().url
    }

    lazy val view: HtmlFormat.Appendable = {
      checkUKPropertyDetailsView(
        viewModel,
        isAgent,
        postAction,
        backUrl)(messages, implicitly)
    }

    lazy val document: Document = Jsoup.parse(contentAsString(view))
  }

  "CheckUKPropertyDetails - Individual" should {
    "renders the heading" in new Setup(false) {
      document.getElementsByClass("govuk-heading-l").text() shouldBe heading
    }
    "renders the back correct back Url" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl.url
    }
    "renders the summary" in new Setup(false) {
      document.select("dl:nth-of-type(1) > div > dt.govuk-summary-list__key").text() shouldBe startDateLabel
      document.select("dl:nth-of-type(1) > div > dd.govuk-summary-list__value").text() shouldBe tradingStartDate.toLongDate
      document.getElementById("change-start-date-link").attr("href") shouldBe changeStartDateUrl
      document.getElementById("change-start-date-link").text() shouldBe s"$change $change"

      document.select("dl:nth-of-type(2) > div > dt.govuk-summary-list__key").text() shouldBe accountingMethodLabel
      document.select("dl:nth-of-type(2) > div > dd.govuk-summary-list__value").text() shouldBe cash
      document.getElementById("change-accounting-method-link").attr("href") shouldBe changeAccountingMethodUrl
      document.getElementById("change-accounting-method-link").text() shouldBe s"$change $change"

      document.getElementById("check-uk-property-details-form").getElementsByTag("button").get(0).text() shouldBe continue
    }
  }

  "CheckUKPropertyDetails - Agent" should {
    "renders the heading" in new Setup(true) {
      document.getElementsByClass("govuk-heading-l").text() shouldBe heading
    }
    "renders the back correct back Url" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl.url
    }
    "renders the summary" in new Setup(true) {
      document.select("dl:nth-of-type(1) > div > dt.govuk-summary-list__key").text()  shouldBe startDateLabel
      document.select("dl:nth-of-type(1) > div > dd.govuk-summary-list__value").text() shouldBe tradingStartDate.toLongDate
      document.getElementById("change-start-date-link").attr("href") shouldBe changeStartDateUrl
      document.getElementById("change-start-date-link").text() shouldBe s"$change $change"

      document.select("dl:nth-of-type(2) > div > dt.govuk-summary-list__key").text() shouldBe accountingMethodLabel
      document.select("dl:nth-of-type(2) > div > dd.govuk-summary-list__value").text() shouldBe cash
      document.getElementById("change-accounting-method-link").attr("href") shouldBe changeAccountingMethodUrl
      document.getElementById("change-accounting-method-link").text() shouldBe s"$change $change"

      document.getElementById("check-uk-property-details-form").getElementsByTag("button").get(0).text() shouldBe continue
    }
  }
}
