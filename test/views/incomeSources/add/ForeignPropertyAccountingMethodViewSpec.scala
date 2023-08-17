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

import auth.MtdItUser
import forms.incomeSources.add.ForeignPropertyAccountingMethodForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.add.ForeignPropertyAccountingMethod

class ForeignPropertyAccountingMethodViewSpec extends TestSupport {
  val foreignPropertyAccountingMethodView: ForeignPropertyAccountingMethod = app.injector.instanceOf[ForeignPropertyAccountingMethod]

  val testUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None,
    incomeSources = noIncomeDetails
  )(fakeRequestCeaseUKPropertyDeclarationComplete)


  class Setup(isAgent: Boolean, error: Boolean = false) extends TestSupport {

    val form: Form[_] = ForeignPropertyAccountingMethodForm.form

    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = true, isUpdate = false).url else
      controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = false).url
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.submitAgent() else
      controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.submit()

    val prefix: String = "incomeSources.add.foreignPropertyAccountingMethod"

    lazy val view: HtmlFormat.Appendable = if (isAgent) {
      foreignPropertyAccountingMethodView(
        form,
        postAction = postAction,
        isAgent = true,
        backUrl = backUrl)
    } else {
      foreignPropertyAccountingMethodView(
        form = form,
        postAction = postAction,
        isAgent = false,
        backUrl = backUrl)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = if (isAgent) {
      foreignPropertyAccountingMethodView(
        form = form.withError(s"$prefix", s"$prefix.no-selection"),
        postAction = postAction,
        isAgent = true,
        backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = true, isUpdate = false).url)

    } else {
      foreignPropertyAccountingMethodView(
        form = form.withError(s"$prefix", s"$prefix.no-selection"),
        postAction = postAction,
        isAgent = false,
        backUrl = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty(isAgent = false, isUpdate = false).url)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "ForeignPropertyAccountingMethod - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__legend").text() shouldBe messages(s"$prefix.heading")
    }
    "render the dropdown" in new Setup(false) {
      document.getElementsByClass("govuk-details__summary").text() shouldBe messages(s"$prefix.example")
      document.getElementsByClass("govuk-body").eq(0).text() shouldBe messages(s"$prefix.drop-down-text")
    }


    "render the radio form" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages(s"$prefix.radio-1-title")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages(s"$prefix.radio-2-title")
      document.getElementsByClass("govuk-hint govuk-radios__hint govuk-hint govuk-radios__hint").eq(0).text() shouldBe messages(s"$prefix.radio-1-hint")
      document.getElementsByClass("govuk-hint govuk-radios__hint govuk-hint govuk-radios__hint").eq(1).text() shouldBe messages(s"$prefix.radio-2-hint")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(false, true) {
      document.getElementById("incomeSources.add.foreignPropertyAccountingMethod-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"$prefix.no-selection")
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"$prefix.no-selection")
    }
    "render the back url" in new Setup(true, true) {
      document.getElementById("back").attr("href") shouldBe backUrl
    }
  }

  "ForeignPropertyAccountingMethod - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-fieldset__legend").text() shouldBe messages(s"$prefix.heading")
    }

    "render the radio form" in new Setup(true) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages(s"$prefix.radio-1-title")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages(s"$prefix.radio-2-title")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the continue button" in new Setup(true) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the input error" in new Setup(true, true) {
      document.getElementById(s"$prefix-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"$prefix.no-selection")
    }
    "render the error summary" in new Setup(true, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"$prefix.no-selection")
    }
    "render the back url" in new Setup(true, true) {
      document.getElementById("back").attr("href") shouldBe backUrl
    }
  }


}
