/*
 * Copyright 2019 HM Revenue & Customs
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

package views.getLatestCalculation

import assets.BaseTestConstants._
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.BillsTestConstants._
import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import models.calculation.BillsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import implicits.ImplicitCurrencyFormatter._
import testUtils.TestSupport

class BillViewSpec extends TestSupport {

  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), singleBusinessIncome)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), propertyIncomeOnly)(FakeRequest())

  private def pageSetup(model: BillsViewModel, user: MtdItUser[_]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.getLatestCalculation.bill(model)(FakeRequest(), applicationMessages, frontendAppConfig, user)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The bill view" should {
    val setup = pageSetup(unpaidBillsViewModel, bizAndPropertyUser)
    import setup._
    val messages = new Messages.Calculation(testYear)
    val crysMessages = new Messages.Calculation(testYear).Crystallised

    s"have the title '${crysMessages.tabTitle}'" in {
      document.title() shouldBe crysMessages.tabTitle
    }

    "have a breadcrumb trail" in {
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-bills").text shouldBe breadcrumbMessages.bills
      document.getElementById("breadcrumb-finalised-bill").text shouldBe breadcrumbMessages.finalisedBill(testYear)
    }

    s"have the sub-heading '${crysMessages.subHeading}'" in {
      document.getElementById("sub-heading").text() shouldBe crysMessages.subHeading
    }

    s"have the page heading '${crysMessages.heading}'" in {
      document.getElementById("heading").text() shouldBe crysMessages.heading
    }

    s"have an Owed Tax section" which {

      s"has the correct 'warning' text '${crysMessages.warning}'" in {
        document.getElementById("warning").text shouldBe messages.Crystallised.warning
      }
    }

    "NOT show a button to go to payments, when the payment feature is disabled" in {
      frontendAppConfig.features.paymentEnabled(false)
      val setup = pageSetup(paidBillsViewModel, bizAndPropertyUser)
      import setup._
      Option(document.getElementById("payment-button")) shouldBe None
    }

    "NOT show a button to go to payments, when the payment feature is enabled but the bill is paid" in {
      frontendAppConfig.features.paymentEnabled(true)
      val setup = pageSetup(paidBillsViewModel, bizAndPropertyUser)
      import setup._
      Option(document.getElementById("payment-button")) shouldBe None
    }

    "show a button to go to payments, when the payment feature is enabled and the bill is not paid" in {
      frontendAppConfig.features.paymentEnabled(true)
      val setup = pageSetup(unpaidBillsViewModel, bizAndPropertyUser)
      import setup._
      document.getElementById("payment-button").text() shouldBe messages.Crystallised.payNow
      document.getElementById("payment-button").attr("href") shouldBe
      controllers.routes.PaymentController.paymentHandoff(unpaidBillsViewModel.currentBill.toPence).url
    }
  }
}
