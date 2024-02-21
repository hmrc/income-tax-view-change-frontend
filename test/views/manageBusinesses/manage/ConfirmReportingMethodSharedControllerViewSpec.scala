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

package views.manageBusinesses.manage

import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testMtditid, testNino, testSelfEmploymentId}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.ukPlusForeignPropertyWithSoleTraderIncomeSource
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.manageBusinesses.manage.ConfirmReportingMethod

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

  val formFieldName = "incomeSources.manage.propertyReportingMethod"

  val formErrorMessage = "incomeSources.manage.propertyReportingMethod.error"

  val yesIWantToSwitchToAnnualMessage = "incomeSources.manage.propertyReportingMethod.checkbox.annual"

  val switchToAnnualHeadingMessage = "incomeSources.manage.propertyReportingMethod.heading.annual"

  class Setup(isAgent: Boolean, error: Boolean, incomeSourceType: IncomeSourceType) {

    private lazy val manageIncomeSourceDetailsController = controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController

    val backUrl = (incomeSourceType match {
      case SelfEmployment => manageIncomeSourceDetailsController.show(isAgent, incomeSourceType, Some(testSelfEmploymentId))
      case _ => manageIncomeSourceDetailsController.show(isAgent, incomeSourceType, None)
    }).url

    def form(changeTo: String): Form[ConfirmReportingMethodForm] = ConfirmReportingMethodForm(changeTo)

    lazy val viewNoErrors: HtmlFormat.Appendable =
      confirmReportingMethodView(
        form = form(testChangeToAnnual),
        postAction = Call("POST", "/"),
        isAgent = isAgent,
        backUrl = backUrl,
        taxYearStartYear = testTaxYearStartYear,
        taxYearEndYear = testTaxYearEndYear,
        newReportingMethod = testChangeToAnnual,
        isCurrentTaxYear = true
      )

    lazy val viewWithInputErrors: HtmlFormat.Appendable =
      confirmReportingMethodView(
        form = form(testChangeToQuarterly).withError(FormError(formFieldName, formErrorMessage)),
        postAction = Call("POST", "/"),
        isAgent = isAgent,
        backUrl = backUrl,
        taxYearStartYear = testTaxYearStartYear,
        taxYearEndYear = testTaxYearEndYear,
        newReportingMethod = testChangeToQuarterly,
        isCurrentTaxYear = true
      )

    lazy val document: Document = {
      if (error) Jsoup.parse(contentAsString(viewWithInputErrors))
      else Jsoup.parse(contentAsString(viewNoErrors))
    }
  }

  "ConfirmReportingMethodView - UKProperty - Individual" should {
    "render the heading" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages(switchToAnnualHeadingMessage, testTaxYearStartYear, testTaxYearEndYear)
    }
    "render the checkbox" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
    }
    "render the checkbox label" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
      document.getElementsByClass("govuk-label govuk-checkboxes__label").text() shouldBe messages(yesIWantToSwitchToAnnualMessage)
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, UkProperty, None).url
    }
    "render the continue button" in new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-this-change")
    }
    "render the error summary message" in new Setup(isAgent = false, error = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-list govuk-error-summary__list").get(0).text() shouldBe messages(formErrorMessage)
    }
    "render the error summary heading" in new Setup(isAgent = false, error = true, incomeSourceType = UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
    }
    "render the error message" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("incomeSources.manage.propertyReportingMethod-error").text() shouldBe s"Error: ${messages(formErrorMessage)}"
    }
    "render the warning message when changing from annual -> quarterly in current tax year" in
      new Setup(isAgent = false, error = true, incomeSourceType = UkProperty) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToQuarterlyWarning.individual")}"
        ) shouldBe true
      }
    "render the warning message when changing from quarterly -> annual in current tax year" in
      new Setup(isAgent = false, error = false, incomeSourceType = UkProperty) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToAnnualWarning.individual")}"
        ) shouldBe true
      }
  }

  "ConfirmReportingMethodView - ForeignProperty - Individual" should {
    "render the heading" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages(switchToAnnualHeadingMessage, testTaxYearStartYear, testTaxYearEndYear)
    }
    "render the checkbox" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
    }
    "render the checkbox label" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
      document.getElementsByClass("govuk-label govuk-checkboxes__label").text() shouldBe messages(yesIWantToSwitchToAnnualMessage)
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, ForeignProperty, None).url
    }
    "render the continue button" in new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-this-change")
    }
    "render the error summary message" in new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-list govuk-error-summary__list").get(0).text() shouldBe messages(formErrorMessage)
    }
    "render the error summary heading" in new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
    }
    "render the error message" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("incomeSources.manage.propertyReportingMethod-error").text() shouldBe s"Error: ${messages(formErrorMessage)}"
    }
    "render the warning message when changing from annual -> quarterly in current tax year" in
      new Setup(isAgent = false, error = true, incomeSourceType = ForeignProperty) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToQuarterlyWarning.individual")}"
        ) shouldBe true
      }
    "render the warning message when changing from quarterly -> annual in current tax year" in
      new Setup(isAgent = false, error = false, incomeSourceType = ForeignProperty) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToAnnualWarning.individual")}"
        ) shouldBe true
      }
  }

  "ConfirmReportingMethodView - Sole Trader Business - Individual" should {
    "render the heading" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages(switchToAnnualHeadingMessage, testTaxYearStartYear, testTaxYearEndYear)
    }
    "render the checkbox" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
    }
    "render the checkbox label" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
      document.getElementsByClass("govuk-label govuk-checkboxes__label").text() shouldBe messages(yesIWantToSwitchToAnnualMessage)
    }
    "render the back link with the correct URL" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = false, SelfEmployment, Some(testSelfEmploymentId)).url
    }
    "render the continue button" in new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-this-change")
    }
    "render the error summary message" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-list govuk-error-summary__list").get(0).text() shouldBe messages(formErrorMessage)
    }
    "render the error summary heading" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
    }
    "render the error message" in new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("incomeSources.manage.propertyReportingMethod-error").text() shouldBe s"Error: ${messages(formErrorMessage)}"
    }
    "render the warning message when changing from annual -> quarterly in current tax year" in
      new Setup(isAgent = false, error = true, incomeSourceType = SelfEmployment) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToQuarterlyWarning.individual")}"
        ) shouldBe true
      }
    "render the warning message when changing from quarterly -> annual in current tax year" in
      new Setup(isAgent = false, error = false, incomeSourceType = SelfEmployment) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToAnnualWarning.individual")}"
        ) shouldBe true
      }
  }

  "ConfirmReportingMethodView - UKProperty - Agent" should {
    "render the heading" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages(switchToAnnualHeadingMessage, testTaxYearStartYear, testTaxYearEndYear)
    }
    "render the checkbox" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
    }
    "render the checkbox label" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
      document.getElementsByClass("govuk-label govuk-checkboxes__label").text() shouldBe messages(yesIWantToSwitchToAnnualMessage)
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = true, UkProperty, None).url
    }
    "render the continue button" in new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-this-change")
    }
    "render the error summary message" in new Setup(isAgent = true, error = true, incomeSourceType = UkProperty) {
      document.getElementsByClass("govuk-list govuk-error-summary__list").get(0).text() shouldBe messages(formErrorMessage)
    }
    "render the error summary heading" in new Setup(isAgent = true, error = true, incomeSourceType = UkProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
    }
    "render the error message" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("incomeSources.manage.propertyReportingMethod-error").text() shouldBe s"Error: ${messages(formErrorMessage)}"
    }
    "render the warning message when changing from annual -> quarterly in current tax year" in
      new Setup(isAgent = true, error = true, incomeSourceType = UkProperty) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToQuarterlyWarning.agent")}"
        ) shouldBe true
      }
    "render the warning message when changing from quarterly -> annual in current tax year" in
      new Setup(isAgent = true, error = false, incomeSourceType = UkProperty) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToAnnualWarning.agent")}"
        ) shouldBe true
      }
  }

  "ConfirmReportingMethodView - Foreign Property - Agent" should {
    "render the heading" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages(switchToAnnualHeadingMessage, testTaxYearStartYear, testTaxYearEndYear)
    }
    "render the checkbox" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
    }
    "render the checkbox label" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
      document.getElementsByClass("govuk-label govuk-checkboxes__label").text() shouldBe messages(yesIWantToSwitchToAnnualMessage)
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = true, ForeignProperty, None).url
    }
    "render the continue button" in new Setup(isAgent = true, error = false, incomeSourceType = ForeignProperty) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-this-change")
    }
    "render the error summary message" in new Setup(isAgent = true, error = true, incomeSourceType = ForeignProperty) {
      document.getElementsByClass("govuk-list govuk-error-summary__list").get(0).text() shouldBe messages(formErrorMessage)
    }
    "render the error summary heading" in new Setup(isAgent = true, error = true, incomeSourceType = ForeignProperty) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
    }
    "render the error message" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("incomeSources.manage.propertyReportingMethod-error").text() shouldBe s"Error: ${messages(formErrorMessage)}"
    }
    "render the warning message when changing from quarterly -> annual in current tax year" in
      new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToAnnualWarning.agent")}"
        ) shouldBe true
      }
    "render the warning message when changing from annual -> quarterly in current tax year" in
      new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
        document.getElementsByClass("govuk-warning-text").first().text().contains(
          s"${messages("incomeSources.manage.propertyReportingMethod.changingToQuarterlyWarning.agent")}"
        ) shouldBe true
      }
  }

  "ConfirmReportingMethodView - Sole Trader Business - Agent" should {
    "render the heading" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-fieldset__legend--l").first().text() shouldBe messages(switchToAnnualHeadingMessage, testTaxYearStartYear, testTaxYearEndYear)
    }
    "render the checkbox" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
    }
    "render the checkbox label" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-checkboxes").size() shouldBe 1
      document.getElementsByClass("govuk-label govuk-checkboxes__label").text() shouldBe messages(yesIWantToSwitchToAnnualMessage)
    }
    "render the back link with the correct URL" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe controllers.manageBusinesses.manage.routes.ManageIncomeSourceDetailsController.show(isAgent = true, SelfEmployment, Some(testSelfEmploymentId)).url
    }
    "render the continue button" in new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
      document.getElementById("confirm-button").text() shouldBe messages("base.confirm-this-change")
    }
    "render the error summary message" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementsByClass("govuk-list govuk-error-summary__list").get(0).text() shouldBe messages(formErrorMessage)
    }
    "render the error summary heading" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
    }
    "render the error message" in new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
      document.getElementById("incomeSources.manage.propertyReportingMethod-error").text() shouldBe s"Error: ${messages(formErrorMessage)}"
    }
    "render the warning message when changing from quarterly -> annual in current tax year" in
      new Setup(isAgent = true, error = false, incomeSourceType = SelfEmployment) {
        document.getElementsByClass("govuk-warning-text").first().text() shouldBe
          s"! Warning ${messages("incomeSources.manage.propertyReportingMethod.changingToAnnualWarning.agent")}"

      }
    "render the warning message when changing from annual -> quarterly in current tax year" in
      new Setup(isAgent = true, error = true, incomeSourceType = SelfEmployment) {
        document.getElementsByClass("govuk-warning-text").first().text() shouldBe
          s"! Warning ${messages("incomeSources.manage.propertyReportingMethod.changingToQuarterlyWarning.agent")}"

      }
  }
}
