/*
 * Copyright 2026 HM Revenue & Customs
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

package views.manageBusinesses.add

import auth.MtdItUser
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import forms.manageBusinesses.add.ChooseSoleTraderAddressForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.ChooseSoleTraderAddressView

class ChooseSoleTraderAddressViewSpec extends TestSupport {

  val view: ChooseSoleTraderAddressView = app.injector.instanceOf[ChooseSoleTraderAddressView]

  class Setup(isAgent: Boolean, form: Form[ChooseSoleTraderAddressForm], hasBusinesses: Boolean = true) {

    val postAction: Call = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.submit(isAgent)

    val pageDocument: Document = Jsoup.parse(
      contentAsString(
        view(
          postAction = postAction,
          isAgent = isAgent,
          form = form,
          businessAddresses = if(hasBusinesses) Seq(("some address line 1, TTT6AB", 0), ("some other address line 1, RRR6AB", 0)) else Nil,
          backUrl = controllers.routes.HomeController.show().url
        )
      )
    )

  }

  val users: Seq[MTDUserRole] = Seq(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  def pageContent() = {
    val radioNewAddress = "None of these, I want to add a new address"
    val formError = "Select an option to continue"
  }

  users.foreach { user => {
    val isAgent: Boolean = if(user != MTDIndividual) true else false
    s"for $user OverseasBusinessAddress FS is enabled" should {
      s"for $user have the correct title" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        pageDocument.title() shouldBe "What is the address of your sole trader business? - Manage your Self Assessment - GOV.UK"
      }
      s"for $user have heading" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        pageDocument.getElementById("choose-sole-trader-address-heading").text() shouldBe "What is the address of your sole trader business?"
      }
      s"for $user have sub-heading" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        pageDocument.getElementById("choose-sole-trader-address-subheading").text() shouldBe "Sole trader"
      }
      s"for $user have the existing business addresses in the radio buttons" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(0).text() shouldBe "some address line 1, TTT6AB"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(1).text() shouldBe "some other address line 1, RRR6AB"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(2).text() shouldBe "or"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(3).text() shouldBe "None of these, I want to add a new address"
      }
      s"for $user have the correct radio buttons when there are no results from getAllUniqueBusinessAddresses" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(0).text()
      }
      s"for $user have a continue button" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        pageDocument.getElementById("choose-sole-trader-address-continue-button").text() shouldBe "Continue"
      }
      s"for $user have a back link with the correct URL" in new Setup(isAgent, ChooseSoleTraderAddressForm()) {
        //TODO this will need to be changed with the nav ticket
        pageDocument.getElementById("back-fallback").text() shouldBe "Back"
        pageDocument.getElementById("back-fallback").attr("href") shouldBe controllers.routes.HomeController.show().url
      }
    }
  }}

}
