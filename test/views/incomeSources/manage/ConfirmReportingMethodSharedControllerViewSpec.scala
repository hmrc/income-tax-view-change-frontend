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

package views.incomeSources.manage

import auth.MtdItUser
import forms.incomeSources.cease.UKPropertyEndDateForm
import forms.incomeSources.manage.ConfirmReportingMethodForm
import forms.models.DateFormElement
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino, testPropertyIncomeId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{ukPlusForeignPropertyWithSoleTraderIncomeSource, ukPropertyIncome}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.cease.UKPropertyEndDate
import views.html.incomeSources.manage.ConfirmReportingMethod

class ConfirmReportingMethodSharedControllerViewSpec extends TestSupport {

  val confirmReportingMethodView: ConfirmReportingMethod = app.injector.instanceOf[ConfirmReportingMethod]

  val testUser: MtdItUser[_] = MtdItUser(
    mtditid = testMtditid,
    nino = testNino,
    userName = None,
    btaNavPartial = None,
    saUtr = None,
    credId = Some("12345-credId"),
    userType = Some(Individual),
    arn = None,
    incomeSources = ukPlusForeignPropertyWithSoleTraderIncomeSource
  )(fakeRequestNoSession)

  val testTaxYear = "2021-2022"
  val testTaxYearStartYear = "2021"
  val testTaxYearEndYear = "2022"

  val testChangeToAnnual = "annual"

  val testChangeToQuarterly = "quarterly"

  class Setup(isAgent: Boolean, error: Boolean = false) {
    val form: Form[ConfirmReportingMethodForm] = ConfirmReportingMethodForm.form

    lazy val view: HtmlFormat.Appendable =
      confirmReportingMethodView(
        form = form,
        postAction = {
          if (isAgent) controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.submitUKPropertyAgent(testMtditid, testTaxYear, testChangeToAnnual)
          else controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.submitUKProperty(testMtditid, testTaxYear, testChangeToAnnual)
        },
        isAgent = isAgent,
        backUrl = {
          if (isAgent) controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showUkProperty.url
          else controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showUkPropertyAgent.url
        },
        taxYearStartYear = testTaxYearStartYear,
        taxYearEndYear = testTaxYearEndYear,
        reportingMethod = testChangeToQuarterly
      )


    lazy val viewWithInputErrors: HtmlFormat.Appendable =
      confirmReportingMethodView(
        form = form.withError(FormError("incomeSources.manage.propertyReportingMethod", "incomeSources.manage.propertyReportingMethod.error.quarterly")),
        postAction = {
          if (isAgent) controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.submitSoleTraderBusinessAgent(testMtditid, testTaxYear, testChangeToAnnual)
          else controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.submitSoleTraderBusinessAgent(testMtditid, testTaxYear, testChangeToAnnual)
        },
        isAgent = true,
        backUrl = {
          if (isAgent) controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(testPropertyIncomeId).url
          else controllers.incomeSources.manage.routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(testPropertyIncomeId).url
        },
        taxYearStartYear = testTaxYearStartYear,
        taxYearEndYear = testTaxYearEndYear,
        reportingMethod = testChangeToQuarterly
      )

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "UKPropertyEndDateView - Individual" should {
    "render the heading" in new Setup(false) {
//      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.heading")
    }
    "render the hint" in new Setup(false) {
//      document.getElementById("uk-property-end-date-hint").text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.hint")
    }
    "render the date form" in new Setup(false) {
//      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
//      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
//      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
//      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(false) {
//      document.getElementById("back").text() shouldBe messages("base.back")
//      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseUKPropertyController.show().url
    }
    "render the continue button" in new Setup(false) {
//      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(false, true) {
//      document.getElementById("uk-property-end-date-error").text() shouldBe messages("base.error-prefix") + " " +
//        messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
    "render the error summary" in new Setup(false, true) {
//      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
//      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
  }

  "UKPropertyEndDateView - Agent" should {
    "render the heading" in new Setup(true) {
//      document.getElementsByClass("govuk-fieldset__heading").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.heading")
    }
    "render the hint" in new Setup(true) {
//      document.getElementById("uk-property-end-date-hint").text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.hint")
    }
    "render the date form" in new Setup(true) {
//      document.getElementsByClass("govuk-label govuk-date-input__label").eq(0).text() shouldBe "Day"
//      document.getElementsByClass("govuk-label govuk-date-input__label").eq(1).text() shouldBe "Month"
//      document.getElementsByClass("govuk-label govuk-date-input__label").eq(2).text() shouldBe "Year"
//      document.getElementsByClass("govuk-date-input__item").size() shouldBe 3
    }
    "render the back link with the correct URL" in new Setup(true) {
//      document.getElementById("back").text() shouldBe messages("base.back")
//      document.getElementById("back").attr("href") shouldBe controllers.incomeSources.cease.routes.CeaseUKPropertyController.showAgent().url
    }
    "render the continue button" in new Setup(true) {
//      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }
    "render the error message" in new Setup(true, true) {
//      document.getElementById("uk-property-end-date-error").text() shouldBe messages("base.error-prefix") + " " +
//        messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
    "render the error summary" in new Setup(true, true) {
//      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
//      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.cease.UKPropertyEndDate.error.beforeStartDate")
    }
  }

}
