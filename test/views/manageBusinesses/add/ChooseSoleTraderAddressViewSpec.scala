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

import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import forms.manageBusinesses.add.ChooseSoleTraderAddressForm
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.ChooseSoleTraderAddressView

class ChooseSoleTraderAddressViewSpec extends TestSupport {

  val view: ChooseSoleTraderAddressView = app.injector.instanceOf[ChooseSoleTraderAddressView]

  class Setup(isAgent: Boolean, form: Form[ChooseSoleTraderAddressForm], isInternational: Boolean = false) {

    val postAction: Call = controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.submit(isAgent)
    val backUrl: String = controllers.manageBusinesses.add.routes.AddBusinessTradeController.show(NormalMode).url

    val businessAddress: Seq[(String, Int)] = Seq(("some address line 1, TTT6AB", 0), ("some other address line 1, RRR6AB", 1))
    val businessAddressInternational: Seq[(String, Int)] = Seq(("some international address", 0), ("no postcode address", 1))

    val pageDocument: Document = Jsoup.parse(
      contentAsString(
        view(
          postAction = postAction,
          isAgent = isAgent,
          form = form,
          chooseSoleTraderAddressRadioOptionsWithIndex =
            if (isInternational) businessAddressInternational else businessAddress,
          backUrl = backUrl
        )
      )
    )

  }

  val users: Seq[MTDUserRole] = Seq(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent)

  users.foreach { user => {
    val isAgent: Boolean = if (user != MTDIndividual) true else false
    s"for $user | OverseasBusinessAddress FS is enabled" should {
      s"$user | render the correct title" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.title() shouldBe "What is the address of your sole trader business? - Manage your Self Assessment - GOV.UK"
      }
      s"$user | render the correct heading" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.getElementById("choose-sole-trader-address-heading").text() shouldBe "What is the address of your sole trader business?"
      }
      s"$user | render the correct sub-heading" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.getElementById("choose-sole-trader-address-subheading").text() shouldBe "Sole trader"
      }
      s"$user | render the correct existing UK business addresses in the radio buttons" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(0).text() shouldBe "some address line 1, TTT6AB"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(1).text() shouldBe "some other address line 1, RRR6AB"
        pageDocument.getElementsByClass("govuk-radios__divider").text() shouldBe "or"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(2).text() shouldBe "None of these, I want to add a new address"
      }
      s"$user | render the correct existing international business addresses in the radio buttons" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0")), isInternational = true) {
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(0).text() shouldBe "some international address"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(1).text() shouldBe "no postcode address"
        pageDocument.getElementsByClass("govuk-radios__divider").text() shouldBe "or"
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(2).text() shouldBe "None of these, I want to add a new address"
      }
      s"$user | render the correct correct radio buttons when there are no results from getAllUniqueBusinessAddresses" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.getElementsByClass("govuk-label govuk-radios__label").get(0).text()
      }
      s"$user | render the continue button" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.getElementById("choose-sole-trader-address-continue-button").text() shouldBe "Continue"
      }
      s"$user | render the correct back link with the correct Add Business Trade URL" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq("0"))) {
        pageDocument.getElementById("back-fallback").text() shouldBe "Back"
        pageDocument.getElementById("back-fallback").attr("href") shouldBe backUrl
      }
      s"$user | render the the input error summary with correct link" in new Setup(isAgent, ChooseSoleTraderAddressForm.form(Seq.empty).withError(FormError("value", "manageBusinesses.add.chooseSoleTraderAddress.radio.option.error"))) {
        pageDocument.getElementsByClass("govuk-error-summary__title").text() shouldBe messages("base.error_summary.heading")
        pageDocument.getElementsByClass("govuk-list govuk-error-summary__list").select("a").text() shouldBe
          messages("manageBusinesses.add.chooseSoleTraderAddress.radio.option.error")
        pageDocument.getElementsByClass("govuk-list govuk-error-summary__list").select("a").attr("href") shouldBe "#choose-sole-trader-address-radios-new-address"
      }
    }
  }
  }

}
