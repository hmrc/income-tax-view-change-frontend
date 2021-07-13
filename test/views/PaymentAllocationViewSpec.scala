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

package views

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import assets.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import assets.PaymentAllocationChargesTestConstants.{documentDetail, financialDetail}
import auth.MtdItUser
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.paymentAllocationCharges.FinancialDetailsWithDocumentDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.paymentAllocation
import assets.MessagesLookUp.{PaymentAllocation => paymentAllocationMessages}


class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  class Setup(paymentAllocationModel: FinancialDetailsWithDocumentDetailsModel = singleTestPaymentAllocationCharge) {
    val testBackUrl: String = "/test-url"

    val page: Html = paymentAllocation(paymentAllocationModel, mockImplicitDateFormatter, "testBackURL")
    val document: Document = Jsoup.parse(page.body)

  }

  "Payment Allocation Page" should {
    "have a heading" in new Setup(){
      document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
    }

    "have a the correct date" in new Setup(){
      document.getElementsByTag("td").eq(1).text shouldBe paymentAllocationMessages.date
    }

    "have a the correct Amount" in new Setup(){
      document.getElementsByTag("td").last.text shouldBe paymentAllocationMessages.amount
    }

    "have info text" in new Setup(){
      document.getElementById("payments-allocation-info").text shouldBe paymentAllocationMessages.info
    }
  }
}
