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
import forms.manageBusinesses.cease.DeclareIncomeSourceCeasedForm.declaration
import forms.manageBusinesses.cease.DeclareIncomeSourceCeasedForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.Call
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import views.html.manageBusinesses.cease.DeclareIncomeSourceCeasedView

class DeclareIncomeSourceCeasedViewSpec extends TestSupport {

  val declarePropertyCeasedView: DeclareIncomeSourceCeasedView = app.injector.instanceOf[DeclareIncomeSourceCeasedView]

  val testBusinessName: String = "Big Business"

  class Setup(isAgent: Boolean, incomeSourceType: IncomeSourceType, error: Boolean = false, businessName: Option[String] = None) {

    val backUrl: String = {
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
      "render the correct h1" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementById("heading").text() shouldBe messages(s"incomeSources.cease.${incomeSourceType.key}.heading")
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

      "render the p1" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementById("confirm-cease-p1").text() shouldBe messages(s"incomeSources.cease.${incomeSourceType.key}.p1")
      }

      "render the confirm and continue button" in new Setup(isAgent = isAgent, incomeSourceType = incomeSourceType) {
        document.getElementById("confirm-button").text() shouldBe messages(s"incomeSources.cease.${incomeSourceType.key}.continue")
      }
    }
  }
}
