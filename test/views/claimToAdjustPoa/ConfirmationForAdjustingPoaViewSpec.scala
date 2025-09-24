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

import models.claimToAdjustPoa.ConfirmationForAdjustingPoaViewModel
import models.incomeSourceDetails.TaxYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.claimToAdjustPoa.ConfirmationForAdjustingPoa

class ConfirmationForAdjustingPoaViewSpec extends TestSupport{

  val confirmationForAdjustingPoaView: ConfirmationForAdjustingPoa = app.injector.instanceOf[ConfirmationForAdjustingPoa]
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

  val testCancelUrl: String = "/report-quarterly/income-and-expenses/view"
  val testCancelUrlAgent: String = "/report-quarterly/income-and-expenses/view/agents"

  class Setup(isAgent: Boolean, isAmountZero: Boolean) {
    val viewModel = ConfirmationForAdjustingPoaViewModel(TaxYear(fixedDate.getYear, fixedDate.getYear + 1), isAmountZero)
    val view: Html = confirmationForAdjustingPoaView(isAgent = isAgent, viewModel)
    val document: Document = Jsoup.parse(view.toString())
    val groupButton: Elements = document.select("div.govuk-button-group")
    val elements = groupButton.first().children()
  }

  "The ConfirmationForAdjustingPoa page" when {
    "a user loads the page and newPoaAmount is zero" should {
      "render the page heading" in new Setup(isAgent = false, isAmountZero = true) {
        document.title shouldBe msgs("htmlTitle", msgs("claimToAdjustPoa.confirmation.heading"))
      }

      "render the caption" in new Setup(isAgent = false, isAmountZero = true) {
        document.getElementById("caption").text shouldBe msgs("claimToAdjustPoa.confirmation.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)
      }

      "render the  heading" in new Setup(isAgent = false, isAmountZero = true) {
        document.getElementById("h1").text shouldBe msgs("claimToAdjustPoa.confirmation.heading")
      }

      "have a 'Accept and submit' button" in new Setup(isAgent = false, isAmountZero = true) {
        elements.get(0).text shouldBe msgs("claimToAdjustPoa.confirmation.continue")
      }

      "have a 'Cancel' button with backUrl" in new Setup(isAgent = false, isAmountZero = true) {
        elements.get(1).text shouldBe msgs("claimToAdjustPoa.confirmation.cancel")
        document.getElementById("cancel").attr("href") shouldBe testCancelUrl
      }

    }

    "a user loads the page and newPoaAmount is greater than zero" should {
      "render the page heading" in new Setup(isAgent = false, isAmountZero = false) {
        document.title shouldBe msgs("htmlTitle", msgs("claimToAdjustPoa.confirmation.heading"))
      }

      "render the caption" in new Setup(isAgent = false, isAmountZero = false) {
        document.getElementById("caption").text shouldBe msgs("claimToAdjustPoa.confirmation.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)
      }

      "render the  heading" in new Setup(isAgent = false, isAmountZero = false) {
        document.getElementById("h1").text shouldBe msgs("claimToAdjustPoa.confirmation.heading")
      }

      "have a 'Accept and submit' button" in new Setup(isAgent = false, isAmountZero = false) {
        elements.get(0).text shouldBe msgs("claimToAdjustPoa.confirmation.continue")
      }

      "have a 'Cancel' button with backUrl" in new Setup(isAgent = false, isAmountZero = false) {
        elements.get(1).text shouldBe msgs("claimToAdjustPoa.confirmation.cancel")
        document.getElementById("cancel").attr("href") shouldBe testCancelUrl
      }

    }

    "an agent loads the page and newPoaAmount is greater than zero" should {
      "render the page heading" in new Setup(isAgent = true, isAmountZero = false) {
        document.title shouldBe msgs("htmlTitle.agent", msgs("claimToAdjustPoa.confirmation.heading"))
      }

      "render the caption" in new Setup(isAgent = true, isAmountZero = false) {
        document.getElementById("caption").text shouldBe msgs("claimToAdjustPoa.confirmation.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)
      }

      "render the  heading" in new Setup(isAgent = true, isAmountZero = false) {
        document.getElementById("h1").text shouldBe msgs("claimToAdjustPoa.confirmation.heading")
      }

      "have a 'Accept and submit' button" in new Setup(isAgent = true, isAmountZero = false) {
        elements.get(0).text shouldBe msgs("claimToAdjustPoa.confirmation.continue")
      }

      "have a 'Cancel' button with backUrl" in new Setup(isAgent = true, isAmountZero = false) {
        elements.get(1).text shouldBe msgs("claimToAdjustPoa.confirmation.cancel")
        document.getElementById("cancel").attr("href") shouldBe testCancelUrlAgent
      }

    }

    "an agent loads the page and newPoaAmount is zero" should {
      "render the page heading" in new Setup(isAgent = true, isAmountZero = true) {
        document.title shouldBe msgs("htmlTitle.agent", msgs("claimToAdjustPoa.confirmation.heading"))
      }

      "render the caption" in new Setup(isAgent = true, isAmountZero = true) {
        document.getElementById("caption").text shouldBe msgs("claimToAdjustPoa.confirmation.caption", fixedDate.getYear.toString, (fixedDate.getYear + 1).toString)
      }

      "render the  heading" in new Setup(isAgent = true, isAmountZero = true) {
        document.getElementById("h1").text shouldBe msgs("claimToAdjustPoa.confirmation.heading")
      }

      "have a 'Accept and submit' button" in new Setup(isAgent = true, isAmountZero = true) {
        elements.get(0).text shouldBe msgs("claimToAdjustPoa.confirmation.continue")
      }

      "have a 'Cancel' button with backUrl" in new Setup(isAgent = true, isAmountZero = true) {
        elements.get(1).text shouldBe msgs("claimToAdjustPoa.confirmation.cancel")
        document.getElementById("cancel").attr("href") shouldBe testCancelUrlAgent
      }

    }

  }
}
