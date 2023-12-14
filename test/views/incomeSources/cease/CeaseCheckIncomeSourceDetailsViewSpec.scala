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

package views.incomeSources.cease

import controllers.incomeSources.cease.routes
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.viewmodels.CheckCeaseIncomeSourceDetailsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testPropertyIncomeId, testSelfEmploymentId}
import testConstants.BusinessDetailsTestConstants.{testBizAddress, testEndDate, testTradeName}
import testUtils.TestSupport
import views.html.incomeSources.cease.CeaseCheckIncomeSourceDetails

class CeaseCheckIncomeSourceDetailsViewSpec extends TestSupport {

  val ceaseCheckIncomeSourceDetailsView: CeaseCheckIncomeSourceDetails = app.injector.instanceOf[CeaseCheckIncomeSourceDetails]

  val viewModelSE: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testSelfEmploymentId), Some(testTradeName), Some(testBizAddress), testEndDate, SelfEmployment)

  val viewModelUK: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testPropertyIncomeId), None, None, testEndDate, UkProperty)

  val viewModelFP: CheckCeaseIncomeSourceDetailsViewModel = CheckCeaseIncomeSourceDetailsViewModel(
    mkIncomeSourceId(testPropertyIncomeId), None, None, testEndDate, ForeignProperty)

  val testBackUrl: String = routes.CeaseIncomeSourceController.show().url
  val testCeaseDateLong: String = "1 January 2023"
  val businessAddressAsString = "64 Zoo Lane Happy Place Magical Land England ZL1 064 United Kingdom"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType) extends TestSupport {

    val messagesPrefix = incomeSourceType.ceaseCheckDetailsPrefix

    val redirectAction: Call = (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment)
      case (false, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.show(SelfEmployment)
      case (true, UkProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty)
      case (false, UkProperty) => routes.IncomeSourceCeasedObligationsController.show(UkProperty)
      case (true, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty)
      case (false, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.show(ForeignProperty)
      case _ => routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType)
    }

    val viewModel: CheckCeaseIncomeSourceDetailsViewModel = (incomeSourceType) match {
      case (SelfEmployment) => viewModelSE
      case (UkProperty) => viewModelUK
      case (ForeignProperty) => viewModelFP
    }

    lazy val view: HtmlFormat.Appendable = ceaseCheckIncomeSourceDetailsView(isAgent, testBackUrl, viewModel, messagesPrefix)
    lazy val document: Document = Jsoup.parse(contentAsString(view)
    )
  }

  "CeaseCheckIncomeSourceDetailsView - Individual" should {
    "render the heading - Self employment" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(messages("incomeSources.ceaseBusiness.checkDetails.caption"))
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.heading")

    }
    "render the heading - Foreign property" in new Setup(isAgent = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(messages("incomeSources.ceaseForeignProperty.checkDetails.caption"))
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("incomeSources.ceaseForeignProperty.checkDetails.heading")

    }
    "render the heading - Uk Property " in new Setup(isAgent = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(messages("incomeSources.ceaseUKProperty.checkDetails.caption"))
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("incomeSources.ceaseUKProperty.checkDetails.heading")
    }

    "render the back link with the correct URL" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
      document.getElementsByClass("govuk-back-link").attr("href") shouldBe testBackUrl
    }

    "render the summary list" in new Setup(false, SelfEmployment) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.dateStopped")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.change")

      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.businessName")
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe testTradeName

      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.businessAddress")
      document.getElementsByClass("govuk-summary-list__value").eq(2).first().text() shouldBe businessAddressAsString
    }

    "render the continue button" in new Setup(isAgent = false, incomeSourceType = SelfEmployment) {
      document.select("#main-content .govuk-button").text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.confirm")
    }
  }

  "CeaseCheckIncomeSourceDetailsView - Agent" should {
    "render the heading - Self employment" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(messages("incomeSources.ceaseBusiness.checkDetails.caption"))
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.heading")

    }
    "render the heading - Foreign property" in new Setup(isAgent = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("incomeSources.ceaseForeignProperty.checkDetails.heading")
      document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(messages("incomeSources.ceaseForeignProperty.checkDetails.caption"))

    }
    "render the heading - Uk Property " in new Setup(isAgent = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("incomeSources.ceaseUKProperty.checkDetails.heading")
      document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(messages("incomeSources.ceaseUKProperty.checkDetails.caption"))
    }

    "render the back link with the correct URL" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-back-link").text() shouldBe messages("base.back")
      document.getElementsByClass("govuk-back-link").attr("href") shouldBe testBackUrl
    }

    "render the summary list" in new Setup(isAgent = true, SelfEmployment) {
      document.getElementsByClass("govuk-summary-list__key").eq(0).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.dateStopped")
      document.getElementsByClass("govuk-summary-list__value").eq(0).text() shouldBe testCeaseDateLong
      document.getElementsByClass("govuk-summary-list__actions").eq(0).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.change")

      document.getElementsByClass("govuk-summary-list__key").eq(1).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.businessName")
      document.getElementsByClass("govuk-summary-list__value").eq(1).text() shouldBe testTradeName

      document.getElementsByClass("govuk-summary-list__key").eq(2).text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.businessAddress")
      document.getElementsByClass("govuk-summary-list__value").eq(2).first().text() shouldBe businessAddressAsString
    }

    "render the continue button" in new Setup(isAgent = true, incomeSourceType = SelfEmployment) {
      document.select("#main-content .govuk-button").text() shouldBe messages("incomeSources.ceaseBusiness.checkDetails.confirm")
    }
  }

}
