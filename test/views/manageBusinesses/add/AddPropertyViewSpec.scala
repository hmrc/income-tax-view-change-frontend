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

package views.manageBusinesses.add

import forms.manageBusinesses.add.AddProprertyForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.FormError
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.manageBusinesses.add.AddPropertyView

class AddPropertyViewSpec extends TestSupport{

  val pageView: AddPropertyView = app.injector.instanceOf[AddPropertyView]

  class Setup(isAgent: Boolean, hasError: Boolean, isTrigMig: Boolean = false) {
    lazy val postCall: Call = controllers.manageBusinesses.add.routes.AddPropertyController.submit(isAgent, isTrigMig)
    lazy val backUrl: String = if(isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
    }
    lazy val document: Document = {
      Jsoup.parse(
        contentAsString(
          pageView(form = {
            if (hasError) AddProprertyForm.apply
              .withError(FormError("type-of-property", "manageBusinesses.type-of-property.error"))
            else AddProprertyForm.apply
          }, isAgent, Some(backUrl), postCall)
        )
      )
    }
  }

  for (isAgent <- Seq(true, false)) yield {
    s"AddPropertyView: isAgent = $isAgent" should {
      "render the heading" in new Setup(isAgent, hasError = false) {
        document.getElementById("heading").text() shouldBe messages("manageBusinesses.type-of-property.heading")
      }
      "render the text" in  new Setup(isAgent, hasError = false) {
        document.getElementById("text1").text() shouldBe messages("manageBusinesses.type-of-property.text1")
        document.getElementById("text2").text() shouldBe messages("manageBusinesses.type-of-property.text2")
        document.getElementById("text3").text() shouldBe messages("manageBusinesses.type-of-property.text3")
      }
      "render the radio form" in new Setup(isAgent, hasError = false) {
        document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--m").text() shouldBe messages("manageBusinesses.type-of-property.h2")
        document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages("manageBusinesses.type-of-property.uk")
        document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages("manageBusinesses.type-of-property.foreign")
        document.getElementsByClass("govuk-radios").size() shouldBe 1
      }
      "render the back link with the correct URL" in new Setup(isAgent, hasError = false) {
        val manageBusinessesUrl: String = if(isAgent) {
          controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
        } else {
          controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url
        }
        document.getElementById("back-fallback").text() shouldBe messages("base.back")
        document.getElementById("back-fallback").attr("href") shouldBe manageBusinessesUrl
      }
      "render the continue button" in new Setup(isAgent, hasError = false) {
        document.getElementById("continue-button").text() shouldBe messages("base.continue")
      }
      "render the input error" in new Setup(isAgent, hasError = true) {
        document.getElementById("type-of-property-error").text() shouldBe messages("base.error-prefix") + " " +
          messages("manageBusinesses.type-of-property.error")
      }
      "render the error summary" in new Setup(isAgent, hasError = true) {
        document.getElementsByClass("govuk-error-summary__title").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe
          messages("manageBusinesses.type-of-property.error")
      }
    }
  }

}
