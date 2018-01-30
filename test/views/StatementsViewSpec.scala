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

import java.time.ZonedDateTime

import assets.Messages.{Statements => messages}
import config.FrontendAppConfig
import utils.TestSupport
import assets.TestConstants._
import play.api.http.Status
import play.twirl.api.HtmlFormat
import play.api.test.FakeRequest
import play.api.i18n.Messages.Implicits._
import play.api.test.Helpers._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import models._
import utils.ImplicitCurrencyFormatter._

class StatementsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testYear1 = 2018
  val testYear2 = 2019

  val charge2018: SubItemModel = SubItemModel(
    subItem = Some("000"),
    dueDate = Some("2018-1-1".toLocalDate),
    amount = Some(1000.00),
    clearingDate = None,
    clearingReason = None,
    outgoingPaymentMethod = None,
    paymentLock = None,
    clearingLock = None,
    interestLock = None,
    dunningLock = None,
    returnFlag = None,
    paymentReference = None,
    paymentAmount = None,
    paymentMethod = None,
    paymentLot = None,
    paymentLotItem = None,
    clearingSAPDocument = None,
    statisticalDocument = None,
    returnReason = None,
    promiseToPay = None
  )

  val payment2018: SubItemModel = SubItemModel(
    subItem = Some("002"),
    dueDate = None,
    amount = None,
    clearingDate = Some("2018-1-1".toLocalDate),
    clearingReason = Some(""),
    outgoingPaymentMethod = Some(""),
    paymentLock = Some(""),
    clearingLock = Some(""),
    interestLock = Some(""),
    dunningLock = Some(""),
    returnFlag = Some(false),
    paymentReference = Some("XX005000002273"),
    paymentAmount = Some(600.00),
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
    dueDate = Some("2019-1-2".toLocalDate),
    amount = Some(1200.00),
    clearingDate = None,
    clearingReason = None,
    outgoingPaymentMethod = None,
    paymentLock = None,
    clearingLock = None,
    interestLock = None,
    dunningLock = None,
    returnFlag = None,
    paymentReference = None,
    paymentAmount = None,
    paymentMethod = None,
    paymentLot = None,
    paymentLotItem = None,
    clearingSAPDocument = None,
    statisticalDocument = None,
    returnReason = None,
    promiseToPay = None
  )

  val payment2019: SubItemModel = SubItemModel(
    subItem = Some("002"),
    dueDate = None,
    amount = None,
    clearingDate = Some("2019-1-2".toLocalDate),
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
    taxPeriodFrom = Some("2017-4-6".toLocalDate),
    taxPeriodTo = Some("2018-4-5".toLocalDate),
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
    outstandingAmount = Some(400.00),
    clearedAmount = Some(600.00),
    accruedInterest = Some(0.00),
    items = Seq(charge2018, payment2018)
  ), testYear1)

  val transactionModel2019: TransactionModelWithYear = TransactionModelWithYear(TransactionModel(
    chargeType = Some(""),
    mainType = Some(""),
    periodKey = Some(""),
    periodKeyDescription = Some(""),
    taxPeriodFrom = Some("2018-4-6".toLocalDate),
    taxPeriodTo = Some("2019-4-5".toLocalDate),
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
    items = Seq(charge2019, payment2019)
  ), testYear2)

  val errorModel: FinancialTransactionsErrorModel = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR,"ISE")

  private def pageSetup(model: Seq[TransactionModelWithYear]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.statements(model)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Statements view" should {
    lazy val statementsModel = Seq(transactionModel2018, transactionModel2019)

    val setup = pageSetup(statementsModel)
    import setup._

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the an intro para '${messages.p1}'" in {
      document.getElementById("statements-p1").text() shouldBe messages.p1
    }

    "have a link to jump to the specified tax year section" in {
      document.getElementById(s"statement-$testYear1").text() shouldBe messages.taxYear(testYear1)
      document.getElementById(s"statement-$testYear2").text() shouldBe messages.taxYear(testYear2)
    }

    "have a heading for each taxYear" in {
      document.getElementById(s"$testYear1-tax-year").text() shouldBe messages.taxYear(testYear1)
      document.getElementById(s"$testYear2-tax-year").text() shouldBe messages.taxYear(testYear2)
    }

    "display the original charge for each taxYear" in {
      document.getElementById(s"$testYear1-total").text() shouldBe transactionModel2018.model.originalAmount.get.toCurrencyString
      document.getElementById(s"$testYear2-total").text() shouldBe transactionModel2019.model.originalAmount.get.toCurrencyString
    }

    "state the amount left to pay for each tax year" in {
      document.getElementById(s"$testYear1-still-to-pay").text() shouldBe messages.stillToPay(transactionModel2018.model.outstandingAmount.get.toCurrencyString)
      document.getElementById(s"$testYear2-still-to-pay").text() shouldBe messages.stillToPay(transactionModel2019.model.outstandingAmount.get.toCurrencyString)
    }

    "state the due date for each tax year" in {
      document.getElementById(s"$testYear1-due-by").text() shouldBe messages.dueBy("31 January " + (testYear1 + 1))
      document.getElementById(s"$testYear2-due-by").text() shouldBe messages.dueBy("31 January " + (testYear2 + 1))
    }

    "Have a heading of 'Your transactions' which contains details of charges and bullet points of payments made" in {
      document.getElementById(s"$testYear1-transactions").text() shouldBe messages.transactions
      document.getElementById(s"$testYear2-transactions").text() shouldBe messages.transactions

      document.getElementById(s"$testYear1-charge-text").text() shouldBe messages.charge(charge2018.amount.get.toCurrencyString)
      document.getElementById(s"$testYear2-charge-text").text() shouldBe messages.charge(charge2019.amount.get.toCurrencyString)

      document.getElementById(s"$testYear1-paid-0").text() shouldBe messages.youPaid(payment2018.paymentAmount.get.toCurrencyString, payment2018.clearingDate.get.toShortDate)
      document.getElementById(s"$testYear2-paid-0").text() shouldBe messages.youPaid(payment2019.paymentAmount.get.toCurrencyString, payment2019.clearingDate.get.toShortDate)
    }
  }

}
