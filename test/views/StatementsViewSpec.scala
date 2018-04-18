/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.Messages.{Statements => messages}
import config.FrontendAppConfig
import models.financialTransactions.{FinancialTransactionsErrorModel, SubItemModel, TransactionModel, TransactionModelWithYear}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.ImplicitCurrencyFormatter._
import utils.ImplicitDateFormatter._
import utils.TestSupport

class StatementsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testYear1 = 2018
  val testYear2 = 2019

  val charge2018: SubItemModel = SubItemModel(
    subItem = Some("000"),
    dueDate = Some("2019-1-31"),
    amount = Some(1000.00)
  )

  val payment2018: SubItemModel = SubItemModel(
    subItem = Some("002"),
    clearingDate = Some("2018-1-1"),
    clearingReason = Some(""),
    outgoingPaymentMethod = Some(""),
    paymentLock = Some(""),
    clearingLock = Some(""),
    interestLock = Some(""),
    dunningLock = Some(""),
    returnFlag = Some(false),
    paymentReference = Some("XX005000002273"),
    paymentAmount = Some(1000.00),
    paymentMethod = Some(""),
    paymentLot = Some(""),
    paymentLotItem = Some(""),
    clearingSAPDocument = Some(""),
    statisticalDocument = Some(""),
    returnReason = Some(""),
    promiseToPay = Some("")
  )

  val charge2019: SubItemModel = SubItemModel(
    subItem = Some("000"),
    dueDate = Some("2020-1-31"),
    amount = Some(1200.00)
  )

  val payment2019: SubItemModel = SubItemModel(
    subItem = Some("002"),
    clearingDate = Some("2019-1-2"),
    clearingReason = Some(""),
    outgoingPaymentMethod = Some(""),
    paymentLock = Some(""),
    clearingLock = Some(""),
    interestLock = Some(""),
    dunningLock = Some(""),
    returnFlag = Some(false),
    paymentReference = Some("XX005000002274"),
    paymentAmount = Some(610.00),
    paymentMethod = Some(""),
    paymentLot = Some(""),
    paymentLotItem = Some(""),
    clearingSAPDocument = Some(""),
    statisticalDocument = Some(""),
    returnReason = Some(""),
    promiseToPay = Some("")
  )

  val transactionModel2018: TransactionModelWithYear = TransactionModelWithYear(TransactionModel(
    chargeType = Some(""),
    mainType = Some(""),
    periodKey = Some(""),
    periodKeyDescription = Some(""),
    taxPeriodFrom = Some("2017-4-6"),
    taxPeriodTo = Some("2018-4-5"),
    businessPartner = Some(""),
    contractAccountCategory = Some(""),
    contractAccount = Some(""),
    contractObjectType = Some(""),
    contractObject = Some(""),
    sapDocumentNumber = Some(""),
    sapDocumentNumberItem = Some(""),
    chargeReference = Some(""),
    mainTransaction = Some(""),
    subTransaction = Some(""),
    originalAmount = Some(1000.00),
    outstandingAmount = Some(0),
    clearedAmount = Some(1000.00),
    accruedInterest = Some(0.00),
    items = Some(Seq(charge2018, payment2018))
  ), testYear1)

  val transactionModel2019: TransactionModelWithYear = TransactionModelWithYear(TransactionModel(
    chargeType = Some(""),
    mainType = Some(""),
    periodKey = Some(""),
    periodKeyDescription = Some(""),
    taxPeriodFrom = Some("2018-4-6"),
    taxPeriodTo = Some("2019-4-5"),
    businessPartner = Some(""),
    contractAccountCategory = Some(""),
    contractAccount = Some(""),
    contractObjectType = Some(""),
    contractObject = Some(""),
    sapDocumentNumber = Some(""),
    sapDocumentNumberItem = Some(""),
    chargeReference = Some(""),
    mainTransaction = Some(""),
    subTransaction = Some(""),
    originalAmount = Some(1200.00),
    outstandingAmount = Some(590.00),
    clearedAmount = Some(610.00),
    accruedInterest = Some(0.00),
    items = Some(Seq(charge2019, payment2019))
  ), testYear2)

  val errorModel: FinancialTransactionsErrorModel = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR,"ISE")

  private def pageSetup(model: Seq[TransactionModelWithYear]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.statements(model)(FakeRequest(), applicationMessages, mockAppConfig, testMtdUserNoNino)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Statements view" when {

    "a 'Seq' of 2 transaction models are passed through" should {
      lazy val statementsModel = Seq(transactionModel2018, transactionModel2019)

      val setup = pageSetup(statementsModel)
      import setup._

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      s"have the an intro para '${messages.p1}'" in {
        document.getElementById("statements-p1").text() shouldBe messages.p1
      }

      "have a heading for each taxYear" in {
        document.getElementById(s"$testYear1-tax-year").text() shouldBe messages.taxYear(testYear1)
        document.getElementById(s"$testYear2-tax-year").text() shouldBe messages.taxYear(testYear2)
      }

      "state that there is nothing left to pay for the tax year that has been fully paid" in {
        document.getElementById(s"$testYear1-nothing-to-pay").text() shouldBe messages.nothingToPay
        document.getElementById(s"$testYear1-still-to-pay") should be(null)
      }

      "state the amount left to pay for the tax year that has not been fully paid" in {
        document.getElementById(s"$testYear2-nothing-to-pay") should be(null)
        document.getElementById(s"$testYear2-still-to-pay").text() shouldBe messages.stillToPay(transactionModel2019.model.outstandingAmount.get.toCurrencyString)
      }

      "have accessible aria-label to provide context for screen readers about the payment link" in {
        document.getElementById(s"$testYear2-payment-link").attr("aria-label") shouldBe messages.paymentAriaLabel(testYear2)
      }

      "state that the bill has been paid for the tax year that has been fully paid" in {
        document.getElementById(s"$testYear1-paid-bill").text() shouldBe messages.paidBill
        document.getElementById(s"$testYear1-due-by") should be(null)
      }

      "state the due date for each tax year that has not been fully paid" in {
        mockAppConfig.features.paymentEnabled(false)
        val setup = pageSetup(statementsModel)
        import setup._
        document.getElementById(s"$testYear2-paid-bill") should be(null)
        document.getElementById(s"$testYear2-due-by").text() shouldBe messages.dueBy("31 January " + (testYear2 + 1))
      }

      "feature enabled state the due date for each tax year that has not been fully paid" in {
        mockAppConfig.features.paymentEnabled(true)
        val setup = pageSetup(statementsModel)
        import setup._
        document.getElementById(s"$testYear2-paid-bill") should be(null)
        document.getElementById(s"$testYear2-due-by").text() shouldBe messages.dueByWithLink("31 January " + (testYear2 + 1))
        document.getElementById(s"$testYear2-payment-link").attr("href") shouldBe
          controllers.routes.PaymentController.paymentHandoff(transactionModel2019.model.outstandingAmount.get.toPence).url
      }

      "Have a heading of 'Your transactions' which contains details of charges and bullet points of payments made" in {
        document.getElementById(s"$testYear1-transactions").text() shouldBe messages.transactions
        document.getElementById(s"$testYear2-transactions").text() shouldBe messages.transactions

        document.getElementById(s"$testYear1-charge-text").text() shouldBe messages.charge(charge2018.amount.get.toCurrencyString)
        document.getElementById(s"$testYear2-charge-text").text() shouldBe messages.charge(charge2019.amount.get.toCurrencyString)

        document.getElementById(s"$testYear1-paid-0").text() shouldBe messages.youPaid(payment2018.paymentAmount.get.toCurrencyString, payment2018.clearingDate.get.toShortDate)
        document.getElementById(s"$testYear2-paid-0").text() shouldBe messages.youPaid(payment2019.paymentAmount.get.toCurrencyString, payment2019.clearingDate.get.toShortDate)
      }

      "say where to find earlier transactions, with the correct link" in {
        document.getElementById("earlier-statements").text() shouldBe messages.earlierTransactions
        document.getElementById("view-sa-calcs").attr("href") shouldBe mockAppConfig.selfAssessmentUrl
      }

      "show a back link to the Income Tax home page" in {
        document.getElementById("it-home-back") shouldNot be(null)
      }
    }

    "an empty 'Seq' is passed through" should {
      val setup = pageSetup(Seq())
      import setup._

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "state that you've currently had no transactions since you started reporting" in {
        document.getElementById("statements-no-transactions").text() shouldBe messages.noTransactions
      }

      "show a back link to the Income Tax home page" in {
        document.getElementById("it-home-back") shouldNot be(null)
      }
    }
  }
}
