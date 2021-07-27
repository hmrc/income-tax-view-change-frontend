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
import assets.PaymentAllocationsTestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsModel, PaymentAllocationViewModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.PaymentAllocation
import assets.MessagesLookUp.{PaymentAllocation => paymentAllocationMessages}


class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val paymentAllocationView = app.injector.instanceOf[PaymentAllocation]

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  val singleTestPaymentAllocationCharge: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail),
    List(financialDetail)
  )

  class Setup(paymentAllocationModel: PaymentAllocationViewModel = paymentAllocationViewModel) {
    paymentAllocationViewModel.originalPaymentAllocationWithClearingDate(0)._2.get.chargeType.get
    val testBackUrl: String = "/test-url"

  paymentAllocationChargesModel.financialDetails.head.chargeType.get + paymentAllocationChargesModel.financialDetails.head.mainType.get

    val page: Html = paymentAllocationView(paymentAllocationModel, "testBackURL")
    val document: Document = Jsoup.parse(page.body)

  }

  "Payment Allocation Page" should {
    "check that the first section information is present" when {
      "checking the heading" in new Setup() {
        document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
      }

      "checking there is a correct date" in new Setup() {
        val result = document.getElementById("payment-allocation-charge-table").getElementsByTag("td").eq(1).text
         result shouldBe paymentAllocationMessages.date
      }

      "checking there is a correct Amount" in new Setup() {
        val result = document.getElementById("payment-allocation-charge-table").getElementsByTag("td").last.text
        result shouldBe paymentAllocationMessages.amount
      }

      "checking there is the info text" in new Setup() {
        document.getElementById("payments-allocation-info").text shouldBe paymentAllocationMessages.info
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in  new Setup(){
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationMessages.paymentAllocationHeading


      }

      "has table headers" in new Setup() {
        val allTableHeadings = document.getElementById("payment-allocation-table").getElementsByTag("th")
        allTableHeadings.eq(0).text() shouldBe paymentAllocationMessages.tableHeadings(0)
        allTableHeadings.eq(1).text() shouldBe paymentAllocationMessages.tableHeadings(1)
        allTableHeadings.eq(2).text() shouldBe paymentAllocationMessages.tableHeadings(2)

      }

      "has a payment within the table" in new Setup(){
        val allTableData =  document.getElementById("payment-allocation-table").getElementsByTag("td")
        "getting payment allocation information"
        allTableData.get(0).text() shouldBe paymentAllocationMessages.tableDataPaymentAllocation
        "getting payment allocation Date Allocated"
        allTableData.get(1).text() shouldBe paymentAllocationMessages.tableDataDateAllocated
        "getting payment allocation Amount"
        allTableData.get(2).text() shouldBe paymentAllocationMessages.tableDataAmount
      }
    }
  }
}
