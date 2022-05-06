/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.financialDetails.{BalanceDetails, DocumentDetailWithDueDate, FinancialDetail}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.FinancialDetailsTestConstants.{documentDetailWithDueDateModel, financialDetail}
import testUtils.{TestSupport, ViewSpec}
import views.html.CreditAndRefunds
import testConstants.MessagesLookUp.{CreditAndRefunds => creditAndRefunds}


class CreditAndRefundsViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  val creditAndRefundView: CreditAndRefunds = app.injector.instanceOf[CreditAndRefunds]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]


 def balanceDetailsModel(firstPendingAmountRequested: Option[BigDecimal] = Some(4.50),
                        secondPendingAmountRequested: Option[BigDecimal] = Some(2.50)): BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    totalBalance = 3.00,
    availableCredit = Some(7.00),
    firstPendingAmountRequested = firstPendingAmountRequested,
    secondPendingAmountRequested = secondPendingAmountRequested,
    None
  )

  val documentDetailWithDueDateFinancialDetailListModel: (DocumentDetailWithDueDate, FinancialDetail) = {
    (documentDetailWithDueDateModel(paymentLot = None, outstandingAmount = Some(-1400.0)), financialDetail())
  }

  class Setup(creditCharges: List[(DocumentDetailWithDueDate, FinancialDetail)] = List(documentDetailWithDueDateFinancialDetailListModel),
              balance: BalanceDetails = balanceDetailsModel(),
              isAgent: Boolean = false,
              backUrl: String = "testString") {
    lazy val page: HtmlFormat.Appendable =
      creditAndRefundView(creditCharges, balance, isAgent = isAgent, backUrl)(FakeRequest(), implicitly, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "displaying individual credit and refund page" should {
    val link = "/report-quarterly/income-and-expenses/view/charges/payments-made?documentNumber=1040000123"

    "display the page" when {
      "a user has requested a refund" in new Setup(){

        document.title() shouldBe creditAndRefunds.title + " - Business Tax account - GOV.UK"
        layoutContent.selectHead("h1").text shouldBe creditAndRefunds.title
        document.select("dt").first().text() shouldBe s"15 May 2019 ${creditAndRefunds.paymentText}"
        document.select("dt").first().select("a").attr("href") shouldBe link

        document.getElementsByClass("govuk-button").first().text() shouldBe creditAndRefunds.claimBtn
        document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe creditAndRefunds.checkBtn


      }

      "a user has not requested a refund" in new Setup(balance = balanceDetailsModel(None, None)){

        document.title() shouldBe creditAndRefunds.title + " - Business Tax account - GOV.UK"
        layoutContent.selectHead("h1").text shouldBe creditAndRefunds.title
        document.select("dt").first().text() shouldBe s"15 May 2019 ${creditAndRefunds.paymentText}"
        document.select("dt").first().select("a").attr("href") shouldBe link

        document.getElementsByClass("govuk-button").first().text() shouldBe creditAndRefunds.claimBtn
        document.getElementsByClass("govuk-button govuk-button--secondary").isEmpty shouldBe true

      }
    }
  }

  "displaying agent credit and refund page" should {
    val link = "/report-quarterly/income-and-expenses/view/agents/charges/payments-made?documentNumber=1040000123"
    "display the page" when {
      "correct data is provided" in new Setup(isAgent = true){

        document.title() shouldBe creditAndRefunds.title + " - Your client’s Income Tax details - GOV.UK"
        layoutContent.selectHead("h1").text shouldBe creditAndRefunds.title
        document.select("dt").first().text() shouldBe s"15 May 2019 ${creditAndRefunds.paymentText}"
        document.select("dt").first().select("a").attr("href") shouldBe link

        document.getElementsByClass("govuk-button").first().text() shouldBe creditAndRefunds.claimBtn
        document.getElementsByClass("govuk-button govuk-button--secondary").text() shouldBe creditAndRefunds.checkBtn

      }
    }
  }
}
