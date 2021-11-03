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

import testConstants.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName}
import testConstants.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testConstants.PaymentAllocationsTestConstants._
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
import testConstants.MessagesLookUp.{PaymentAllocation => paymentAllocationMessages}


class PaymentAllocationViewSpec extends ViewSpec with ImplicitDateFormatter {
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val paymentAllocationView = app.injector.instanceOf[PaymentAllocation]

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned,
    Some("testUtr"), Some("testCredId"), Some("Individual"), None)(FakeRequest())

  val singleTestPaymentAllocationChargeWithOutstandingAmountZero: FinancialDetailsWithDocumentDetailsModel = FinancialDetailsWithDocumentDetailsModel(
    List(documentDetail.copy(outstandingAmount = Some(0))),
    List(financialDetail)
  )

  class Setup(paymentAllocationModel: PaymentAllocationViewModel = paymentAllocationViewModel) {
    paymentAllocationViewModel.originalPaymentAllocationWithClearingDate(0).allocationDetail.get.chargeType.get
    val testBackUrl: String = "/test-url"

    val page: Html = paymentAllocationView(paymentAllocationModel, "testBackURL")
    val document: Document = Jsoup.parse(page.body)

  }

  "Payment Allocation Page for non LPI" should {
    "check that the first section information is present" when {
      "checking the heading" in new Setup() {
        document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
      }

      "checking there is a correct date" in new Setup() {
        val result = document.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text
         result shouldBe paymentAllocationMessages.date
      }

      "checking there is a correct Amount" in new Setup() {
        val result = document.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text
        result shouldBe paymentAllocationMessages.amount
      }

      "checking there is the info text" in new Setup() {
        document.getElementsByClass("govuk-inset-text").text shouldBe paymentAllocationMessages.info
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in  new Setup(){
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationMessages.paymentAllocationHeading


      }

      "has table headers" in new Setup() {
        val allTableHeadings = document.selectHead("thead")
        allTableHeadings.selectNth("th", 1).text() shouldBe paymentAllocationMessages.tableHeadings(0)
        allTableHeadings.selectNth("th", 2).text() shouldBe paymentAllocationMessages.tableHeadings(1)
        allTableHeadings.selectNth("th", 3).text() shouldBe paymentAllocationMessages.tableHeadings(2)

      }

      "has a payment within the table" in new Setup() {
        val allTableData =  document.selectHead("tbody").selectHead("tr")
        "getting payment allocation information" in {
          allTableData.selectNth("td", 1).text() shouldBe paymentAllocationMessages.tableDataPaymentAllocation
        }
        "getting payment allocation Date Allocated" in {
          allTableData.selectNth("td", 2).text() shouldBe paymentAllocationMessages.tableDataDateAllocated
        }
        "getting payment allocation Amount" in {
          allTableData.selectNth("td", 3).text() shouldBe paymentAllocationMessages.tableDataAmount
        }
      }

      "has a Credit on account row within payment details" in new Setup() {
        val allTableData =  document.getElementById("credit-on-account").getElementsByTag("td")
        "getting payment allocation information" in {
          allTableData.get(0).text() shouldBe paymentAllocationMessages.creditOnAccount
        }
        "getting payment allocation Amount" in {
          allTableData.get(2).text() shouldBe paymentAllocationMessages.creditOnAccountAmount
        }
      }

      "should not have Credit on account row within payment details" in new Setup(paymentAllocationViewModel.copy(
        paymentAllocationChargeModel = singleTestPaymentAllocationChargeWithOutstandingAmountZero)) {
        document.getElementById("credit-on-account") shouldBe null
      }
    }
  }
  "Payment Allocation Page for LPI" should {
    "check that the first section information is present" when {
      "checking the heading" in new Setup(paymentAllocationViewModelLpi) {
        document.getElementsByTag("h1").text shouldBe paymentAllocationMessages.heading
      }

      "checking there is a correct date" in new Setup(paymentAllocationViewModelLpi) {
        val result = document.selectHead("dl > div:nth-child(1) > dd:nth-child(2)").text
        result shouldBe paymentAllocationMessages.date
      }

      "checking there is a correct Amount" in new Setup(paymentAllocationViewModelLpi) {
        val result = document.selectHead("dl > div:nth-child(2) > dd:nth-child(2)").text
        result shouldBe paymentAllocationMessages.amount
      }

      "checking there is the info text" in new Setup(paymentAllocationViewModelLpi) {
        document.getElementsByClass("govuk-inset-text").text shouldBe paymentAllocationMessages.info
      }
    }

    "check that the second section information is present" when {
      "has a main heading" in  new Setup(paymentAllocationViewModelLpi){
        document.getElementsByTag("h2").eq(0).text() shouldBe paymentAllocationMessages.paymentAllocationHeading
      }

      "has table headers" in new Setup(paymentAllocationViewModelLpi) {
        val allTableHeadings = document.selectHead("thead")
        allTableHeadings.selectNth("th", 1).text() shouldBe paymentAllocationMessages.tableHeadings(0)
        allTableHeadings.selectNth("th", 2).text() shouldBe paymentAllocationMessages.tableHeadings(1)
        allTableHeadings.selectNth("th", 3).text() shouldBe paymentAllocationMessages.tableHeadings(2)

      }

      "has a payment within the table" in new Setup(paymentAllocationViewModelLpi) {
        val allTableData =  document.selectHead("tbody").selectHead("tr")
//        "getting payment allocation information"
        allTableData.selectNth("td", 1).text() shouldBe paymentAllocationMessages.tableDataPaymentAllocationLpi

//        "getting payment allocation Date Allocated"
        allTableData.selectNth("td", 2).text() shouldBe paymentAllocationMessages.tableDataDateAllocatedLpi

//        "getting payment allocation Amount"
        allTableData.selectNth("td", 3).text() shouldBe paymentAllocationMessages.tableDataAmountLpi

      }
    }
  }
}
