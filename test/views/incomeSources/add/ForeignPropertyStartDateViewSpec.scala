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
import forms.incomeSources.add.ForeignPropertyStartDateForm
import forms.models.DateFormElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.add.ForeignPropertyStartDate

class ForeignPropertyStartDateViewSpec extends TestSupport {

  val foreignPropertyStartDateView: ForeignPropertyStartDate = app.injector.instanceOf[ForeignPropertyStartDate]

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
    val mockDateService: DateService = app.injector.instanceOf[DateService]
    val form: Form[DateFormElement] = new ForeignPropertyStartDateForm(mockDateService)(languageUtils).apply

    lazy val view: HtmlFormat.Appendable = if (isAgent) {
      foreignPropertyStartDateView(
        foreignPropertyStartDateForm = form,
        postAction = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.submitAgent(),
        isAgent = true,
        backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
    } else {
      foreignPropertyStartDateView(
        foreignPropertyStartDateForm = form,
        postAction = controllers.incomeSources.cease.routes.UKPropertyEndDateController.submit(),
        isAgent = false,
        backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
        origin = Some("pta"))(testUser, messages)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = if (isAgent) {
      foreignPropertyStartDateView(
        foreignPropertyStartDateForm = form.withError(FormError("foreign-property-start-date", "incomeSources.add.foreignProperty.startDate.error.invalid")),
        postAction = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.submitAgent(),
        isAgent = true,
        backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
    } else {
      foreignPropertyStartDateView(
        foreignPropertyStartDateForm = form.withError(FormError("foreign-property-start-date", "incomeSources.add.foreignProperty.startDate.error.invalid")),
        postAction = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.submit(),
        isAgent = false,
        backUrl = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url,
        origin = Some("pta"))
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "ForeignPropertyStartDateView - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.heading")
    }
    "render the hint" in new Setup(false) {
      document.getElementById("foreign-property-start-date-hint").text() shouldBe s"${messages("incomeSources.add.foreignProperty.startDate.hint")} ${messages("incomeSources.add.foreignProperty.startDate.hintExample")}"
    }
    "render the date form" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(false, true) {
      document.getElementById("foreign-property-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.error.invalid")
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.error.invalid")
    }
  }

  "ForeignPropertyStartDateView - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.heading")
    }
    "render the hint" in new Setup(true) {
      document.getElementById("foreign-property-start-date-hint").text() shouldBe s"${messages("incomeSources.add.foreignProperty.startDate.hint")} ${messages("incomeSources.add.foreignProperty.startDate.hintExample")}"
    }
    "render the date form" in new Setup(true) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url
    }
    "render the continue button" in new Setup(true) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(true, true) {
      document.getElementById("foreign-property-start-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.error.invalid")
    }
    "render the error summary" in new Setup(true, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.error.invalid")
    }
  }
}
