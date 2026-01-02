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

package views.manageBusinesses.cease

import controllers.manageBusinesses.cease.routes
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.CheckCeaseIncomeSourceDetailsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testPropertyIncomeId, testSelfEmploymentId}
import testConstants.BusinessDetailsTestConstants.{testBizAddress, testEndDate, testIncomeSource, testTradeName}
import testUtils.TestSupport
import views.html.manageBusinesses.cease.CeaseCheckIncomeSourceDetailsView

class CeaseCheckIncomeSourceDetailsViewSpec extends TestSupport {

  val ceaseCheckIncomeSourceDetailsView: CeaseCheckIncomeSourceDetailsView = app.injector.instanceOf[CeaseCheckIncomeSourceDetailsView]

  val viewModelSE: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testSelfEmploymentId), Some(testTradeName), Some(testIncomeSource), Some(testBizAddress), testEndDate, SelfEmployment)

  val viewModelSEUnknown: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testSelfEmploymentId), None, None, Some(testBizAddress), testEndDate, SelfEmployment)

  val viewModelUK: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testPropertyIncomeId), None, None, None, testEndDate, UkProperty)

  val viewModelFP: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testPropertyIncomeId), None, None, None, testEndDate, ForeignProperty)

  val testBackUrl: String = routes.CeaseIncomeSourceController.show().url
  val testCeaseDateLong: String = "1 January 2023"
  val businessAddressAsString = "64 Zoo Lane Happy Place Magical Land England ZL1 064 United Kingdom"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, viewModel: CheckCeaseIncomeSourceDetailsViewModel) {

    val messagesPrefix: String = incomeSourceType.ceaseCheckAnswersPrefix

    val redirectAction: Call = (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment)
      case (false, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.show(SelfEmployment)
      case (true, UkProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty)
      case (false, UkProperty) => routes.IncomeSourceCeasedObligationsController.show(UkProperty)
      case (true, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty)
      case (false, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.show(ForeignProperty)
      case _ => routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType)
    }

    lazy val view: HtmlFormat.Appendable = ceaseCheckIncomeSourceDetailsView(isAgent, testBackUrl, viewModel, messagesPrefix)
    lazy val document: Document = Jsoup.parse(contentAsString(view)
    )
  }

  "CeaseCheckIncomeSourceDetailsView - Individual" should {
    "render the heading - Self employment" in new Setup(isAgent = false, incomeSourceType = SelfEmployment, viewModelSE) {
      document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(messages(s"cease-check-answers.caption"))
      document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("cease-check-answers.title")

    }
    "render the heading - Foreign property" in new Setup(isAgent = false, incomeSourceType = ForeignProperty, viewModelFP) {
      document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(messages("cease-check-answers-fp.caption"))
      document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("cease-check-answers.title")

    }
    "render the heading - Uk Property " in new Setup(isAgent = false, incomeSourceType = UkProperty, viewModelUK) {
      document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(messages("cease-check-answers-uk.caption"))
      document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("cease-check-answers.title")
    }

    "render the back link with the correct URL" in new Setup(isAgent = false, incomeSourceType = SelfEmployment, viewModelSE) {
      document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
      document.getElementsByClass("govuk-back-link").attr("href") shouldBe testBackUrl
    }

    "render the summary list for SE" in new Setup(false, SelfEmployment, viewModelSE) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")

      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("cease-check-answers.business-name")
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe testTradeName

      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("cease-check-answers.trade")
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe testIncomeSource

      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("cease-check-answers.address")
      document.getElementsByClass("govuk-summary-list__value").eq(3).first().text() shouldBe businessAddressAsString
    }

    "render the summary list for SE with Unknown trade" in new Setup(false, SelfEmployment, viewModelSEUnknown) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")

      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("cease-check-answers.business-name")
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe messages("cease-check-answers.unknown")

      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("cease-check-answers.trade")
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe messages("cease-check-answers.unknown")

      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("cease-check-answers.address")
      document.getElementsByClass("govuk-summary-list__value").eq(3).first().text() shouldBe businessAddressAsString
    }

    "render the summary list for UK" in new Setup(false, UkProperty, viewModelUK) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")
    }

    "render the summary list for FP" in new Setup(false, ForeignProperty, viewModelFP) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")
    }

    "render the continue button" in new Setup(isAgent = false, incomeSourceType = SelfEmployment, viewModelSE) {
      document.select("#main-content .govuk-button").text() shouldBe messages("base.confirm-and-continue")
    }
  }

  "CeaseCheckIncomeSourceDetailsView - Agent" should {
    "render the heading - Self employment" in new Setup(isAgent = true, incomeSourceType = SelfEmployment, viewModelSE) {
      document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(messages(s"cease-check-answers.heading"))
      document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("cease-check-answers.title")

    }
    "render the heading - Foreign property" in new Setup(isAgent = false, incomeSourceType = ForeignProperty, viewModelFP) {
      document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(messages("cease-check-answers-fp.caption"))
      document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("cease-check-answers.title")

    }
    "render the heading - Uk Property " in new Setup(isAgent = false, incomeSourceType = UkProperty, viewModelUK) {
      document.getElementsByClass("hmrc-caption govuk-caption-xl").text().contains(messages("cease-check-answers-uk.caption"))
      document.getElementsByClass("govuk-heading-xl").first().text() shouldBe messages("cease-check-answers.title")
    }

    "render the back link with the correct URL" in new Setup(isAgent = true, incomeSourceType = SelfEmployment, viewModelSE) {
      document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
      document.getElementsByClass("govuk-back-link").attr("href") shouldBe testBackUrl
    }

    "render the summary list" in new Setup(true, SelfEmployment, viewModelSE) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")

      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("cease-check-answers.business-name")
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe testTradeName

      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("cease-check-answers.trade")
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe testIncomeSource

      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("cease-check-answers.address")
      document.getElementsByClass("govuk-summary-list__value").eq(3).first().text() shouldBe businessAddressAsString
    }

    "render the summary list for SE with Unknown trade" in new Setup(true, SelfEmployment, viewModelSEUnknown) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")

      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("cease-check-answers.business-name")
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe messages("cease-check-answers.unknown")

      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("cease-check-answers.trade")
      document.getElementsByClass("govuk-summary-list__value").eq(2).text() shouldBe messages("cease-check-answers.unknown")

      document.getElementsByClass("govuk-summary-list__key").eq(3).text() shouldBe messages("cease-check-answers.address")
      document.getElementsByClass("govuk-summary-list__value").eq(3).first().text() shouldBe businessAddressAsString
    }

    "render the summary list for UK" in new Setup(true, UkProperty, viewModelUK) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")
    }

    "render the summary list for FP" in new Setup(true, ForeignProperty, viewModelFP) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("cease-check-answers.cease-date")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("cease-check-answers.change")
    }

    "render the continue button" in new Setup(isAgent = true, incomeSourceType = SelfEmployment, viewModelSE) {
      document.select("#main-content .govuk-button").text() shouldBe messages("base.confirm-and-continue")
    }
  }

}
