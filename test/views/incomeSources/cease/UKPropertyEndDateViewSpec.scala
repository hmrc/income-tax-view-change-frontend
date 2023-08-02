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

import auth.MtdItUser
import forms.incomeSources.cease.UKPropertyEndDateForm
import forms.models.DateFormElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNavHtml, testNino}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.ukPropertyIncome
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.cease.UKPropertyEndDate

class UKPropertyEndDateViewSpec extends TestSupport {

  val UKPropertyEndDateView: UKPropertyEndDate = app.injector.instanceOf[UKPropertyEndDate]

  val testUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None,
    incomeSources = ukPropertyIncome
  )(fakeRequestCeaseUKPropertyDeclarationComplete)


  class Setup(isAgent: Boolean, error: Boolean = false) {
    val mockDateService: DateService = app.injector.instanceOf[DateService]
    val form: Form[DateFormElement] = new UKPropertyEndDateForm(mockDateService).apply(testUser)

    lazy val view: HtmlFormat.Appendable = if (isAgent) {
      UKPropertyEndDateView(
        UKPropertyEndDateForm = form,
        postAction = controllers.incomeSources.cease.routes.UKPropertyEndDateController.submitAgent(),
        isAgent = true,
        backUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url)(agentUserConfirmedClient(), implicitly)
    } else {
      UKPropertyEndDateView(
        UKPropertyEndDateForm = form,
        postAction = controllers.incomeSources.cease.routes.UKPropertyEndDateController.submit(),
        isAgent = false,
        backUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url)(individualUser, implicitly)
    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = if (isAgent) {
      UKPropertyEndDateView(
        UKPropertyEndDateForm = form.withError(FormError("uk-property-end-date", "incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")),
        postAction = controllers.incomeSources.cease.routes.UKPropertyEndDateController.submitAgent(),
        isAgent = true,
        backUrl = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.showAgent().url)(agentUserConfirmedClient(), implicitly)
    } else {
      UKPropertyEndDateView(
        UKPropertyEndDateForm = form.withError(FormError("uk-property-end-date", "incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")),
        postAction = controllers.incomeSources.cease.routes.UKPropertyEndDateController.submit(),
        isAgent = false,
        backUrl = controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url)(individualUser, implicitly)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "UKPropertyEndDateView - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.heading")
    }
    "render the hint" in new Setup(false) {
      document.getElementById("uk-property-end-date-hint").text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.hint")
    }
    "render the date form" in new Setup(false) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url
    }
    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(false, true) {
      document.getElementById("uk-property-end-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
  }

  "UKPropertyEndDateView - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.heading")
    }
    "render the hint" in new Setup(true) {
      document.getElementById("uk-property-end-date-hint").text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.hint")
    }
    "render the date form" in new Setup(true) {
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url
    }
    "render the continue button" in new Setup(true) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(true, true) {
      document.getElementById("uk-property-end-date-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
    "render the error summary" in new Setup(true, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
  }

}
