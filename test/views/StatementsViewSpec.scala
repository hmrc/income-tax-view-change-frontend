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

class StatementsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val successSubItemModel: SubItemModel = SubItemModel(
    subItem = Some(""),
    dueDate = Some("2018-1-1".toLocalDate),
    amount = Some(0.00),
    clearingDate = Some("2018-1-1".toLocalDate),
    clearingReason = Some(""),
    outgoingPaymentMethod = Some(""),
    paymentLock = Some(""),
    clearingLock = Some(""),
    interestLock = Some(""),
    dunningLock = Some(""),
    returnFlag = Some(false),
    paymentReference = Some(""),
    paymentAmount = Some(0.00),
    paymentMethod = Some(""),
    paymentLot = Some(""),
    paymentLotItem = Some(""),
    clearingSAPDocument = Some(""),
    statisticalDocument = Some(""),
    returnReason = Some(""),
    promiseToPay = Some("")
  )

  val successTransactionModel: TransactionModel = TransactionModel(
    chargeType = Some(""),
    mainType = Some(""),
    periodKey = Some(""),
    periodKeyDescription = Some(""),
    taxPeriodFrom = Some("2018-1-1".toLocalDate),
    taxPeriodTo = Some("2018-1-2".toLocalDate),
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
    originalAmount = Some(0.00),
    outstandingAmount = Some(0.00),
    clearedAmount = Some(0.00),
    accruedInterest = Some(0.00),
    items = Seq(successSubItemModel)
  )
  val errorModel: FinancialTransactionsErrorModel = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR,"ISE")

  private def pageSetup(model: FinancialTransactionsModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.statements(model)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Statements view" should {
    lazy val statementsModel = FinancialTransactionsModel(
      idType = "",
      idNumber = "",
      regimeType = "",
      processingDate = ZonedDateTime,
      financialTransactions = Seq(successTransactionModel)
    )


    val setup = pageSetup(statementsModel)
    import setup._

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the an intro para '${messages.p1}'" in {
      document.getElementById("statements-p1").text() shouldBe messages.p1
    }
  }

}
