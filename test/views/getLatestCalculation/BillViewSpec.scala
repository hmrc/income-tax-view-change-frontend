/*
 * Copyright 2021 HM Revenue & Customs
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
import assets.BillsTestConstants._
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.MessagesLookUp
import assets.MessagesLookUp.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, Payment}
import implicits.ImplicitCurrencyFormatter._
import models.calculation.BillsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class BillViewSpec extends TestSupport with FeatureSwitching {

  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    businessAndPropertyAligned, Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    singleBusinessIncome, Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName),
    propertyIncomeOnly, Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())


  private def pageSetup(model: BillsViewModel, paymentsEnabled: Boolean = false, user: MtdItUser[_]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.getLatestCalculation.bill(model, paymentsEnabled)(FakeRequest(), implicitly, appConfig, user)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The bill view" should {
    val setup = pageSetup(unpaidBillsViewModel, paymentsEnabled = false, bizAndPropertyUser)
    import setup._
    val messages = new MessagesLookUp.Calculation(testYear)
    val crysMessages = new MessagesLookUp.Calculation(testYear).Crystallised

    s"have the title '${crysMessages.tabTitle}'" in {
      document.title() shouldBe crysMessages.tabTitle
    }

    "have a breadcrumb trail" in {
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-tax-years").text shouldBe breadcrumbMessages.taxYears
      document.getElementById("breadcrumb-finalised-bill").text shouldBe breadcrumbMessages.finalisedBill(testYear)
    }

    s"have the sub-heading '${crysMessages.subHeading}'" in {
      document.getElementById("sub-heading").text() shouldBe "Bills"
    }

    s"have the page heading '${crysMessages.heading}'" in {
      document.getElementById("heading").text() shouldBe crysMessages.heading
    }


    "NOT show a button to go to payments, when the the eligibility is false" in {
      val setup = pageSetup(paidBillsViewModel, paymentsEnabled = false, bizAndPropertyUser)
      import setup._
      Option(document.getElementById("payment-button")) shouldBe None
    }

    "show a button to go to payments, when the the eligibility is true" in {
      enable(Payment)
      val setup = pageSetup(unpaidBillsViewModel, paymentsEnabled = true, bizAndPropertyUser)
      import setup._
      document.getElementById("payment-button").text() shouldBe messages.Crystallised.payNow
      document.getElementById("payment-button").attr("href") shouldBe
        controllers.routes.PaymentController.paymentHandoff(unpaidBillsViewModel.currentBill.toPence).url
    }
  }
}
