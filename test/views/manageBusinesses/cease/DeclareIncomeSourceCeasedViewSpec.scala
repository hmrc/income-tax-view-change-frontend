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

package views.manageBusinesses.cease

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm
import forms.incomeSources.cease.DeclareIncomeSourceCeasedForm.declaration
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.cease.DeclareIncomeSourceCeased

class DeclareIncomeSourceCeasedViewSpec extends TestSupport {

  val declarePropertyCeasedView: DeclareIncomeSourceCeased = app.injector.instanceOf[DeclareIncomeSourceCeased]

  val testBusinessName: String = "Big Business"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, error: Boolean = false, businessName: Option[String] = None) {

    val backUrl = {
      if (isAgent) controllers.manageBusinesses.cease.routes.CeaseIncomeSourceController.showAgent()
      else         controllers.manageBusinesses.cease.routes.CeaseIncomeSourceController.show()
    }.url

    lazy val view: HtmlFormat.Appendable = declarePropertyCeasedView(
      form = DeclareIncomeSourceCeasedForm.form(incomeSourceType),
      incomeSourceType = incomeSourceType,
      soleTraderBusinessName = businessName,
      postAction = Call("", ""),
      isAgent = isAgent,
      backUrl = backUrl
    )(individualUser, implicitly)

    lazy val viewWithInputErrors: HtmlFormat.Appendable = declarePropertyCeasedView(
      form = DeclareIncomeSourceCeasedForm.form(incomeSourceType)
        .withError(declaration, messages(s"incomeSources.cease.${incomeSourceType.key}.checkboxError")),
      incomeSourceType = incomeSourceType,
      soleTraderBusinessName = Some(testBusinessName),
      postAction = Call("", ""),
      isAgent = isAgent,
      backUrl = backUrl
    )(individualUser, implicitly)

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  for {
    incomeSourceType <- Seq(SelfEmployment, UkProperty, ForeignProperty)
    isAgent          <- Seq(true, false)
  } yield {
    s"Declare $incomeSourceType Ceased View - isAgent = $isAgent" should {
      "render the legend" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementsByClass("govuk-fieldset__legend govuk-fieldset__legend--l").first().text() shouldBe
          messages(s"incomeSources.cease.${incomeSourceType.key}.heading")
      }
      "render the checkbox" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementById(declaration).attr("type") shouldBe "checkbox"
      }
      "render the checkbox label" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementsByClass("govuk-label govuk-checkboxes__label").first().text() shouldBe
          messages(s"incomeSources.cease.${incomeSourceType.key}.checkboxLabel")
      }

      if (incomeSourceType equals SelfEmployment) {
        "render the business-specific hint" in new Setup(isAgent = isAgent, incomeSourceType = SelfEmployment, businessName = Some(testBusinessName)) {
          document.getElementById(s"$declaration-hint").text() shouldBe messages("incomeSources.cease.SE.hint", testBusinessName)
        }
        "render the generic business hint" in new Setup(isAgent = isAgent, incomeSourceType = SelfEmployment) {
          document.getElementById(s"$declaration-hint").text() shouldBe messages("incomeSources.cease.SE.hint.noBusinessName")
        }
      } else {
        "render the property hint" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
          document.getElementById(s"$declaration-hint").text() shouldBe messages(s"incomeSources.cease.${incomeSourceType.key}.hint")
        }
      }

      "render the back link with the correct URL" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementById("back-fallback").text() shouldBe messages("base.back")
        document.getElementById("back-fallback").attr("href") shouldBe(
          if (isAgent) controllers.manageBusinesses.cease.routes.CeaseIncomeSourceController.showAgent().url
          else         controllers.manageBusinesses.cease.routes.CeaseIncomeSourceController.show().url
        )
      }
      "render the continue button" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementById("continue-button").text() shouldBe messages("base.continue")
      }
      "render the error summary" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType, error = true) {
        document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
        document.getElementsByClass("govuk-error-summary__body").first().text() shouldBe
          messages(s"incomeSources.cease.${incomeSourceType.key}.checkboxError")
      }
    }
  }
}
