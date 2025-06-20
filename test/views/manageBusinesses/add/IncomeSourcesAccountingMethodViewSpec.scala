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

package views.manageBusinesses.add

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.IncomeSourcesAccountingMethodForm
import models.core.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.manageBusinesses.add.IncomeSourcesAccountingMethod

class IncomeSourcesAccountingMethodViewSpec extends TestSupport {
  val incomeSourcesAccountingMethodView: IncomeSourcesAccountingMethod = app.injector.instanceOf[IncomeSourcesAccountingMethod]

  val prefixSoleTrader: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val prefixUKProperty: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val prefixForeignProperty: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

  val errorMessageKey: String = "incomeSources.add.AccountingMethod.no-selection"

  val testUser: MtdItUser[_] = defaultMTDITUser(Some(Individual),
    noIncomeDetails, fakeRequestWithNinoAndOrigin("pta"))


  class Setup(isAgent: Boolean, incomeSourcePrefix: String, incomeSourceType: IncomeSourceType, error: Boolean = false) {

    val form: Form[_] = IncomeSourcesAccountingMethodForm(incomeSourceType)

    val (backUrl, postAction) = incomeSourceType match {
      case SelfEmployment =>
        (controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = isAgent, mode = NormalMode).url, controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.submit(SelfEmployment, isAgent))
      case UkProperty =>
        (controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = isAgent, mode = NormalMode).url, controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.submit(UkProperty, isAgent))
      case _ =>
        (controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = isAgent, mode = NormalMode).url, controllers.manageBusinesses.add.routes.IncomeSourcesAccountingMethodController.submit(ForeignProperty, isAgent))
    }

    lazy val view: HtmlFormat.Appendable = incomeSourcesAccountingMethodView(
      cashOrAccrualsFlag = Some(""),
      incomeSourceType,
      form,
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl
    )

    lazy val viewWithInputErrors: HtmlFormat.Appendable = incomeSourcesAccountingMethodView(
      cashOrAccrualsFlag = Some(""),
      incomeSourceType,
      form = form.withError(s"$incomeSourcePrefix", s"$errorMessageKey"),
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl
    )

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  def incomeSourcesAccountingMethodTest(prefix: String, isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
    "render the heading for " + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementsByClass("govuk-fieldset__legend").text() shouldBe messages(s"$prefix.heading")
      document.getElementById(s"$prefix-caption").text() shouldBe messages("accessibility.this-section-is") + " " + messages(s"incomeSources.add.${incomeSourceType.messagesSuffix}")
    }
    "render the dropdown for " + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementsByClass("govuk-details__summary").text() shouldBe messages(s"$prefix.example")
      document.getElementsByClass("govuk-body").eq(0).text() shouldBe messages(s"$prefix.drop-down-text")
    }

    "render the radio form for " + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages(s"$prefix.radio-1-title")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages(s"$prefix.radio-2-title")
      document.getElementsByClass("govuk-hint govuk-radios__hint govuk-hint govuk-radios__hint").eq(0).text() shouldBe messages(s"$prefix.radio-1-hint")
      document.getElementsByClass("govuk-hint govuk-radios__hint govuk-hint govuk-radios__hint").eq(1).text() shouldBe messages(s"$prefix.radio-2-hint")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL for " + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementById("back-fallback").text() shouldBe messages("base.back")
      document.getElementById("back-fallback").attr("href") shouldBe backUrl
    }
    "render the input error for " + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType, true) {
      document.getElementById(s"$prefix-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"$errorMessageKey")
      document.getElementsByClass("govuk-error-summary__title").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"$errorMessageKey")
    }
  }

  "IncomeSourcesAccountingMethod - Individual" should {
    incomeSourcesAccountingMethodTest(prefixSoleTrader, isAgent = false, SelfEmployment)
    incomeSourcesAccountingMethodTest(prefixUKProperty, isAgent = false, UkProperty)
    incomeSourcesAccountingMethodTest(prefixForeignProperty, isAgent = false, ForeignProperty)
  }

  "IncomeSourcesAccountingMethod - Agent" should {
    incomeSourcesAccountingMethodTest(prefixSoleTrader, isAgent = true, SelfEmployment)
    incomeSourcesAccountingMethodTest(prefixUKProperty, isAgent = true, UkProperty)
    incomeSourcesAccountingMethodTest(prefixForeignProperty, isAgent = true, ForeignProperty)
  }

}
