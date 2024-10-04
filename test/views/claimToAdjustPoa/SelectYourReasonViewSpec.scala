/*
 * Copyright 2024 HM Revenue & Customs
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

package views.claimToAdjustPoa

import forms.adjustPoa.SelectYourReasonFormProvider
import models.claimToAdjustPoa.{MainIncomeLower, SelectYourReason}
import models.core.Mode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.Html
import testUtils.TestSupport
import views.html.claimToAdjustPoa.SelectYourReasonView

class SelectYourReasonViewSpec extends TestSupport {

  val selectYourReasonView: SelectYourReasonView = app.injector.instanceOf[SelectYourReasonView]
  lazy val form: Form[SelectYourReason] = new SelectYourReasonFormProvider().apply()
  val taxYear = 2024
  val mode: Mode = ???

  val view: Html = selectYourReasonView(form, taxYear, false, )
  val document: Document = Jsoup.parse(selectYourReasonView.toString)

  val title = "Select Your Reason - Manage your Income Tax updates - GOV.UK"
  val caption = "2024 to 2025 tax year"
  val heading = "Select your reason"
  val paragraph1 = "You can only reduce your payments on account for one of the reasons listed below. If none of these apply to you, " +
    "you will not be able to continue."
  val paragraph2 = "If you cannot afford to pay your tax bill in full, you can contact HMRC to set up a payment plan (opens in new tab)."
  val paragraph2Link = "https://www.gov.uk/difficulties-paying-hmrc"
  val subheading = "Why are you reducing your payments on account?"
  val bullet1 = "My main income will be lower"
  val bullet1Hint = "For example, sole trader or property business profits."
  val bullet2 = "My other income will be lower"
  val bullet2Hint = "For example, dividend payments or pension income."
  val bullet3 = "My tax allowances or reliefs will be higher"
  val bullet3Hint = "For example, marriage allowance, pension relief or payments to charity."
  val bullet4 = "More of my income will be taxed at source"
  val butllet4Hint = "For example, under PAYE."

  "SelectYourReasonView" should {

    "render the correct title" in {
      document.title() shouldBe title
    }
  }

}
