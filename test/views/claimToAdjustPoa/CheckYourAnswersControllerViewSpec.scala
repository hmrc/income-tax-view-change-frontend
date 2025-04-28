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

package views.claimToAdjustPoa

import controllers.claimToAdjustPoa.routes._
import models.claimToAdjustPoa.{Increase, MainIncomeLower, SelectYourReason}
import models.core.CheckMode
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.test.Helpers.contentAsString
import testUtils.TestSupport
import views.html.claimToAdjustPoa.CheckYourAnswers

class CheckYourAnswersControllerViewSpec extends TestSupport {

  class Setup(isAgent: Boolean, poaReason: SelectYourReason = MainIncomeLower) {

    val checkYourAnswers: CheckYourAnswers = app.injector.instanceOf[CheckYourAnswers]

    val document: Document =
      Jsoup.parse(
        contentAsString(
          checkYourAnswers(
            isAgent = isAgent,
            taxYear = TaxYear(2023, 2024),
            adjustedFirstPoaAmount = BigDecimal(3000.00),
            adjustedSecondPoaAmount = BigDecimal(3000.00),
            poaReason = poaReason,
            redirectUrl = ConfirmationForAdjustingPoaController.show(isAgent).url,
            changePoaReasonUrl = SelectYourReasonController.show(isAgent, CheckMode).url,
            changePoaAmountUrl = EnterPoaAmountController.show(isAgent, CheckMode).url
          )
        )
      )
  }

  //noinspection ScalaStyle
  def executeTest(isAgent: Boolean): Unit = {
    s"${if (isAgent) "Agent" else "Individual"}: CheckYourAnswersView" should {
      "render the heading" in new Setup(isAgent) {
        document.getElementsByClass("govuk-caption-l").first().ownText() shouldBe messages("claimToAdjustPoa.checkYourAnswers.caption", "2023", "2024")
        document.getElementsByClass("govuk-heading-l").first().text() shouldBe messages("claimToAdjustPoa.checkYourAnswers.heading")
      }
      "render the first first summary list key" in new Setup(isAgent) {
        document.getElementsByClass("govuk-summary-list__key").get(0).text() shouldBe
          messages("claimToAdjustPoa.checkYourAnswers.summary-list-1.key")
      }
      "render the second summary list key" in new Setup(isAgent) {
        document.getElementsByClass("govuk-summary-list__key").get(1).text() shouldBe
          messages("claimToAdjustPoa.checkYourAnswers.summary-list-2.key")
      }
      "render the first first summary list value" in new Setup(isAgent) {
        document.getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe
          messages("claimToAdjustPoa.selectYourReason.radios.main-lower")
      }
      "render the second summary list value" in new Setup(isAgent) {
        document.getElementsByClass("govuk-summary-list__value").get(1).text() shouldBe
          messages("claimToAdjustPoa.checkYourAnswers.summary-list-1.value", "£3,000.00") + " " +
          messages("claimToAdjustPoa.checkYourAnswers.summary-list-2.value", "£3,000.00")
      }
      "render the first change link" in new Setup(isAgent) {
        document.getElementById("change-1").text() shouldBe messages("claimToAdjustPoa.checkYourAnswers.summary-list.change")
        document.getElementById("change-1").getElementsByTag("a").attr("href") shouldBe SelectYourReasonController.show(isAgent, CheckMode).url
      }
      "render the second change link" in new Setup(isAgent) {
        document.getElementById("change-2").text() shouldBe messages("claimToAdjustPoa.checkYourAnswers.summary-list.change")
        document.getElementById("change-2").getElementsByTag("a").attr("href") shouldBe EnterPoaAmountController.show(isAgent, CheckMode).url
      }
      "render the continue button" in new Setup(isAgent) {
        document.getElementById("confirm-button").text() shouldBe messages("base.confirm-and-continue")
        document.getElementById("confirm-button").getElementsByTag("a").attr("href") shouldBe ConfirmationForAdjustingPoaController.show(isAgent).url
      }
      "render the Confirm and Save button" in new Setup(isAgent = isAgent, poaReason = Increase) {
        document.getElementById("confirm-and-save-button").text() shouldBe messages("claimToAdjustPoa.checkYourAnswers.confirm-and-save")
      }
      "render the cancel link" in new Setup(isAgent) {
        document.getElementById("cancel-link").text() shouldBe messages("claimToAdjustPoa.checkYourAnswers.cancel")
        document.getElementById("cancel-link").getElementsByTag("a").attr("href") shouldBe getCancelLinkUrl(isAgent)
      }
      "hide the Payment On Account reason row when the user has increased the Poa after a prior adjustment" in new Setup(isAgent, poaReason = Increase) {
          document.getElementsByClass("govuk-summary-list__key").size() shouldBe 1
          document.getElementsByClass("govuk-summary-list__key").get(0).text() shouldBe messages("claimToAdjustPoa.checkYourAnswers.summary-list-2.key")
          document.getElementsByClass("govuk-summary-list__value").size() shouldBe 1
          document.getElementsByClass("govuk-summary-list__value").get(0).text() shouldBe
            messages("claimToAdjustPoa.checkYourAnswers.summary-list-1.value", "£3,000.00") + " " +
              messages("claimToAdjustPoa.checkYourAnswers.summary-list-2.value", "£3,000.00")
      }
    }
  }

  def getCancelLinkUrl(isAgent: Boolean): String = {
    if (isAgent) controllers.routes.HomeController.showAgent()
    else         controllers.routes.HomeController.show()
  }.url

  executeTest(isAgent = true)
  executeTest(isAgent = false)
}
