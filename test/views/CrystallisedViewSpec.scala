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

package views

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import assets.EstimatesTestConstants._
import assets.FinancialTransactionsTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import implicits.ImplicitCurrencyFormatter._
import models.calculation.CalculationDataModel
import models.financialTransactions.TransactionModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class CrystallisedViewSpec extends TestSupport {

  val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), singleBusinessIncome)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), propertyIncomeOnly)(FakeRequest())

  override def beforeEach(): Unit = {
    mockAppConfig.features.calcBreakdownEnabled(true)
  }

  private def pageSetup(calcDataModel: CalculationDataModel, transactions: TransactionModel, user: MtdItUser[_]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.crystallised(
      calculationDisplaySuccessModel(calcDataModel),
      transactions,
      testYear
    )(FakeRequest(), applicationMessages, mockAppConfig, user)

    lazy val document: Document = Jsoup.parse(contentAsString(page))

    implicit val model: CalculationDataModel = calcDataModel
  }

  "The Crystallised view" should {
    val setup = pageSetup(busPropBRTCalcDataModel, transactionModel(), bizAndPropertyUser)
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

    s"have the UTR reference '${crysMessages.utrHeading}'" in {
      document.select("#utr-reference-heading").text() shouldBe crysMessages.utrHeading
    }

    s"have an Owed Tax section" which {

      lazy val owedTaxSection = document.getElementById("owed-tax")

      "has a section for What you owe" which {

        lazy val wyoSection = owedTaxSection.getElementById("whatYouOwe")

        s"has the correct 'whatYouOwe' p1 paragraph '${crysMessages.p1}'" in {

          wyoSection.getElementById("inYearP1").text shouldBe crysMessages.p1

        }

        "have a what you owe heading displayed with the correct value" in {
          wyoSection.select("div.bordered-box p.bold-medium").text shouldBe crysMessages.owed("£1,400")
        }

        "have the correct message for the tax year due date" in {
          wyoSection.select("p.form-hint").text shouldBe crysMessages.payDeadline
        }

      }

      s"has the correct 'p1' text '${crysMessages.p1}'" in {
        document.getElementById("inYearP1").text shouldBe messages.Crystallised.p1
      }
    }

    "have the calculation breakdown section and link hidden" when {
      "when the breakdown is not required" in {
        mockAppConfig.features.calcBreakdownEnabled(false)
        val setup = pageSetup(busPropBRTCalcDataModel, transactionModel(), bizUser)
        import setup._
        document.getElementById("inYearCalcBreakdown") shouldBe null
      }

      "when the breakdown is not required and the bill has been paid off" in {
        mockAppConfig.features.calcBreakdownEnabled(false)
        val setup = pageSetup(busPropBRTCalcDataModel, paidTransactionModel(), bizUser)
        import setup._
        document.getElementById("inYearCalcBreakdown") shouldBe null
        document.select("section h2").text() shouldBe messages.Crystallised.noBreakdownContent("£3,400")
      }
    }

    "have the calculation breakdown section visible" when {

      "the bill has been paid and breakdown is required without a progressive disclosure element" in {
        mockAppConfig.features.calcBreakdownEnabled(true)
        val setup = pageSetup(busPropBRTCalcDataModel, paidTransactionModel(), bizUser)
        import setup._
        document.select("table.income-table").size() shouldBe 1
      }

      "the bill has not been paid and breakdown is required inside a progressive disclosure element" in {
        mockAppConfig.features.calcBreakdownEnabled(true)
        val setup = pageSetup(busPropBRTCalcDataModel, transactionModel(), bizUser)
        import setup._
        document.select("table.income-table").size() shouldBe 1
      }
    }

    "NOT have payment related content when the bill has been paid" in {
      val setup = pageSetup(busPropBRTCalcDataModel, paidTransactionModel(), bizAndPropertyUser)
      import setup._
      Option(document.getElementById("adjustments")) shouldBe None
      Option(document.getElementById("changes")) shouldBe None
      document.select("div.divider--bottom p.bold-medium").size() shouldBe 0
      document.select("div.divider--bottom p.form-hint").size() shouldBe 0
      document.select("section#inYearCalcBreakdown div.form-group p").size() shouldBe 0
    }

    "have a couple of sentences about adjustments when the bill has not been paid" in {
      val setup = pageSetup(busPropBRTCalcDataModel, transactionModel(), bizAndPropertyUser)
      import setup._
      document.getElementById("adjustments").text shouldBe crysMessages.errors
      document.getElementById("changes").text shouldBe crysMessages.changes
    }

    "NOT show a button to go to payments" when {
      "not eligible for payments" in {
        mockAppConfig.features.paymentEnabled(true)
        val setup = pageSetup(busPropBRTCalcDataModel, paidTransactionModel(), bizAndPropertyUser)
        import setup._
        Option(document.getElementById("payment-button")) shouldBe None
      }

      "the bill has no outstanding amount" in {
        mockAppConfig.features.paymentEnabled(true)
        val setup = pageSetup(busPropBRTCalcDataModel, paidTransactionModel().copy(outstandingAmount = None), bizAndPropertyUser)
        import setup._
        Option(document.getElementById("payment-button")) shouldBe None
      }
    }

    "show a button to go to payments, when eligible for payments" in {
      mockAppConfig.features.paymentEnabled(true)
      val setup = pageSetup(busPropBRTCalcDataModel, transactionModel(), bizAndPropertyUser)
      import setup._
      document.getElementById("payment-button").text() shouldBe messages.Crystallised.payNow
      document.getElementById("payment-button").attr("href") shouldBe
        controllers.routes.PaymentController.paymentHandoff(transactionModel().outstandingAmount.get.toPence).url
    }
  }
}
