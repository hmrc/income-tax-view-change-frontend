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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.add.IncomeSourcesAccountingMethodForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.noIncomeDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

class IncomeSourcesAccountingMethodViewSpec extends TestSupport {
  val incomeSourcesAccountingMethodView: IncomeSourcesAccountingMethod = app.injector.instanceOf[IncomeSourcesAccountingMethod]

  val prefixSoleTrader: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val prefixUKProperty: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val prefixForeignProperty: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

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


  class Setup(isAgent: Boolean, incomeSourcePrefix: String, incomeSourceType: String, error: Boolean = false) extends TestSupport {

    val form: Form[_] = IncomeSourcesAccountingMethodForm(incomeSourcePrefix)

    val (backUrl, postAction) = incomeSourceType match {
      case SelfEmployment.key =>
        (controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.handleRequest(SelfEmployment.key, isAgent, isChange = false).url, controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submitAgent (SelfEmployment.key))
      case UkProperty.key =>
        (controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.handleRequest(UkProperty.key, isAgent, isChange = false).url, controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submitAgent(UkProperty.key))
      case ForeignProperty.key =>
        (controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.handleRequest(ForeignProperty.key, isAgent, isChange = false).url, controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submitAgent(ForeignProperty.key))
    }

    lazy val view: HtmlFormat.Appendable = incomeSourcesAccountingMethodView(
        incomeSourceType,
        form,
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl
    )

    lazy val viewWithInputErrors: HtmlFormat.Appendable = incomeSourcesAccountingMethodView(
        incomeSourceType,
        form = form.withError(s"$incomeSourcePrefix", s"$incomeSourcePrefix.no-selection"),
        postAction = postAction,
        isAgent = isAgent,
        backUrl = backUrl
    )

    lazy val document: Document = if (error) Jsoup.parse(contentAsString(viewWithInputErrors)) else Jsoup.parse(contentAsString(view))
  }

  def incomeSourcesAccountingMethodTest(prefix: String, isAgent: Boolean, incomeSourceType: String): Unit = {
    "render the heading for" + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementsByClass("govuk-fieldset__legend").text() shouldBe messages(s"$prefix.heading")
    }
    "render the dropdown for" + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementsByClass("govuk-details__summary").text() shouldBe messages(s"$prefix.example")
      document.getElementsByClass("govuk-body").eq(0).text() shouldBe messages(s"$prefix.drop-down-text")
    }

    "render the radio form for" + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementsByClass("govuk-label govuk-radios__label").eq(0).text() shouldBe messages(s"$prefix.radio-1-title")
      document.getElementsByClass("govuk-label govuk-radios__label").eq(1).text() shouldBe messages(s"$prefix.radio-2-title")
      document.getElementsByClass("govuk-hint govuk-radios__hint govuk-hint govuk-radios__hint").eq(0).text() shouldBe messages(s"$prefix.radio-1-hint")
      document.getElementsByClass("govuk-hint govuk-radios__hint govuk-hint govuk-radios__hint").eq(1).text() shouldBe messages(s"$prefix.radio-2-hint")
      document.getElementsByClass("govuk-radios").size() shouldBe 1
    }
    "render the back link with the correct URL for" + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType) {
      document.getElementById("back").text() shouldBe messages("base.back")
      document.getElementById("back").attr("href") shouldBe backUrl
    }
    "render the input error for" + incomeSourceType in new Setup(isAgent, prefix, incomeSourceType, true) {
      document.getElementById(s"$prefix-error").text() shouldBe messages("base.error-prefix") + " " +
        messages(s"$prefix.no-selection")
      document.getElementById("error-summary-heading").text() shouldBe messages("base.error_summary.heading")
      document.getElementsByClass("govuk-list govuk-error-summary__list").text() shouldBe messages(s"$prefix.no-selection")
    }
  }

  "IncomeSourcesAccountingMethod - Individual" should {
    incomeSourcesAccountingMethodTest(prefixSoleTrader, false, SelfEmployment.key)
    incomeSourcesAccountingMethodTest(prefixUKProperty, false, UkProperty.key)
    incomeSourcesAccountingMethodTest(prefixForeignProperty, false, ForeignProperty.key)
  }

  "IncomeSourcesAccountingMethod - Agent" should {
    incomeSourcesAccountingMethodTest(prefixSoleTrader, true, SelfEmployment.key)
    incomeSourcesAccountingMethodTest(prefixUKProperty, true, UkProperty.key)
    incomeSourcesAccountingMethodTest(prefixForeignProperty, true, ForeignProperty.key)
  }

}
