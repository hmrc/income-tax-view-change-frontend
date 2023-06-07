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
import forms.incomeSources.add.ForeignPropertyStartDateCheckForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import services.DateService
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.add.ForeignPropertyStartDateCheck

class ForeignPropertyStartDateCheckViewSpec extends TestSupport {
  val foreignPropertyStartDateCheckView: ForeignPropertyStartDateCheck = app.injector.instanceOf[ForeignPropertyStartDateCheck]

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
    val form: Form[ForeignPropertyStartDateCheckForm] = ForeignPropertyStartDateCheckForm.form
    val startDate = "2 April 2022"
    lazy val view: HtmlFormat.Appendable = if (isAgent) {
      foreignPropertyStartDateCheckView(
        form = form,
        foreignPropertyStartDate = startDate,
        isAgent = true,
        backUrl = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.showAgent().url)
    } else {
      foreignPropertyStartDateCheckView(
        form = form,
        foreignPropertyStartDate = startDate,
        isAgent = false,
        backUrl = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.show().url)

    }

    lazy val viewWithInputErrors: HtmlFormat.Appendable = if (isAgent) {
      foreignPropertyStartDateCheckView(
        form = form.withError("foreign-property-start-date-check", "incomeSources.add.foreignProperty.startDate.check.error"),
        foreignPropertyStartDate = startDate,
        isAgent = true,
        backUrl = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.showAgent().url)

    } else {
      foreignPropertyStartDateCheckView(
        form = form.withError("foreign-property-start-date-check", "incomeSources.add.foreignProperty.startDate.check.error"),
        foreignPropertyStartDate = startDate,
        isAgent = false,
        backUrl = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.show().url)
    }

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  "ForeignPropertyStartDateCheckView - Individual" should {
    "render the heading" in new Setup(false) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.check.heading")
    }

    "render the hint" in new Setup(false) {
      document.getElementById("foreign-property-start-date-check-hint").text() shouldBe startDate
    }

    "render the date check form" in new Setup(false) {
      document.getElementById("incomeSources.add.foreignProperty.startDate.check-yes-response").attr("value") shouldBe messages("incomeSources.add.foreignProperty.startDate.check.radio.yes")
      document.getElementById("incomeSources.add.foreignProperty.startDate.check-no-response").attr("value") shouldBe messages("incomeSources.add.foreignProperty.startDate.check.radio.no")
    }

    "render the back link with the correct URL" in new Setup(false) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.add.routes.ForeignPropertyStartDateController.show().url
    }

    "render the continue button" in new Setup(false) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }

    "render the form with post action" in new Setup(false) {
      document.getElementById("foreign-property-start-date-check-form").attr("action") shouldBe controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show()
    }

    "render the error message" in new Setup(false, true) {
      document.getElementById("foreign-property-start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.check.error")
    }

    "render the error summary" in new Setup(false, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.check.error")
    }
  }

  "ForeignPropertyStartDateCheckView - Agent" should {
    "render the heading" in new Setup(true) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.check.heading")
    }

    "render the hint" in new Setup(true) {
      document.getElementById("foreign-property-start-date-check-hint").text() shouldBe startDate
    }

    "render the date check form" in new Setup(true) {
      document.getElementById("incomeSources.add.foreignProperty.startDate.check-yes-response").attr("value") shouldBe messages("incomeSources.add.foreignProperty.startDate.check.radio.yes")
      document.getElementById("incomeSources.add.foreignProperty.startDate.check-no-response").attr("value") shouldBe messages("incomeSources.add.foreignProperty.startDate.check.radio.no")
    }

    "render the back link with the correct URL" in new Setup(true) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe controllers.incomeSources.add.routes.ForeignPropertyStartDateController.showAgent().url
    }

    "render the continue button" in new Setup(true) {
      document.getElementById("continue-button").text() shouldBe messages("base.continue")
    }

    "render the form with post action" in new Setup(true) {
      document.getElementById("foreign-property-start-date-check-form").attr("action") shouldBe controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent()
    }

    "render the error message" in new Setup(true, true) {
      document.getElementById("foreign-property-start-date-check-error").text() shouldBe messages("base.error-prefix") + " " +
        messages("incomeSources.add.foreignProperty.startDate.check.error")
    }

    "render the error summary" in new Setup(true, true) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe messages("incomeSources.add.foreignProperty.startDate.check.error")
    }

  }
}
