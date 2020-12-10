/*
 * Copyright 2020 HM Revenue & Customs
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
import java.time.format.DateTimeFormatter

import assets.BaseTestConstants.{testMtditid, testNino, testRetrievedUserName, testTimeStampString}
import assets.FinancialTransactionsTestConstants._
import assets.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import assets.MessagesLookUp.{Breadcrumbs => breadcrumbMessages, PaymentDue => paymentDueMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitCurrencyFormatter._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import javax.inject.Inject
import models.financialTransactions.FinancialTransactionsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils

class PaymentsDueViewSpec @Inject() (val languageUtils: LanguageUtils) extends TestSupport with FeatureSwitching with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  implicit val mockImplicitDateFormatter: ImplicitDateFormatterImpl = new ImplicitDateFormatterImpl(mockLanguageUtils)


  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testRetrievedUserName), businessAndPropertyAligned)(FakeRequest())

  class Setup(model: List[FinancialTransactionsModel], paymentEnabled: Boolean = false) {
    val html: HtmlFormat.Appendable = views.html.paymentDue(model, paymentEnabled, mockImplicitDateFormatter)(FakeRequest(), implicitly, mockAppConfig)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))
  }

  val unpaidFinancialTransactions: List[FinancialTransactionsModel] = List(FinancialTransactionsModel(
    Some("MTDBSA"),
    Some("XQIT00000000001"),
    Some("ITSA"),
    testTimeStampString.toZonedDateTime,
    Some(List(transactionModel()))
  ))

  val noFinancialTransactions: List[FinancialTransactionsModel] = List(FinancialTransactionsModel(
    Some("MTDBSA"),
    Some("XQIT00000000001"),
    Some("ITSA"),
    testTimeStampString.toZonedDateTime,
    None
  ))


  "The Payments due view" should {


    "when the user has bills for a single taxYear" should {

      s"have the title '${paymentDueMessages.title}'" in new Setup(unpaidFinancialTransactions) {
        pageDocument.title() shouldBe paymentDueMessages.title
      }

      s"have the heading '${paymentDueMessages.heading}'" in new Setup(unpaidFinancialTransactions) {
        pageDocument.getElementsByTag("h1").text shouldBe paymentDueMessages.heading
      }

      s"have the description  ${paymentDueMessages.description}" in new Setup(unpaidFinancialTransactions) {
        pageDocument.select("p1").text shouldBe paymentDueMessages.description
      }

      "display current unpaid bills" in new Setup(unpaidFinancialTransactions) {
        val testTaxYearTo = unpaidFinancialTransactions.head.financialTransactions.get.head.taxPeriodTo.get.getYear
        val testTaxYearFrom = unpaidFinancialTransactions.head.financialTransactions.get.head.taxPeriodFrom.get.getYear
        pageDocument.getElementById(s"payments-due-$testTaxYearTo").text shouldBe paymentDueMessages.taxYearPeriod(testTaxYearFrom.toString, testTaxYearTo.toString)

        pageDocument.getElementById(s"payments-due-outstanding-$testTaxYearTo").text shouldBe unpaidFinancialTransactions.head.financialTransactions.get.head.outstandingAmount.get.toCurrencyString

        pageDocument.getElementById(s"payments-due-on-$testTaxYearTo").text shouldBe s"${paymentDueMessages.due} ${unpaidFinancialTransactions.head.financialTransactions.get.head.due.get.toLongDate}"

      }

      "have a breadcrumb trail" in new Setup(unpaidFinancialTransactions) {
        pageDocument.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        pageDocument.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        pageDocument.getElementById("breadcrumb-payments-due").text shouldBe breadcrumbMessages.payementsDue
      }


      "have a link to the bill" in new Setup(unpaidFinancialTransactions) {
        val testTaxYearTo = unpaidFinancialTransactions.head.financialTransactions.get.head.taxPeriodTo.get.getYear
        val testTaxYearFrom = unpaidFinancialTransactions.head.financialTransactions.get.head.taxPeriodFrom.get.getYear

        pageDocument.getElementById(s"bills-link-$testTaxYearTo").text shouldBe paymentDueMessages.billLink
        pageDocument.select(s"#bills-link-$testTaxYearTo a").attr("aria-label") shouldBe paymentDueMessages.billLinkAria(testTaxYearFrom.toString, testTaxYearTo.toString)

        val expectedUrl = controllers.routes.CalculationController.renderCalculationPage(testTaxYearTo).url
        pageDocument.select(s"#bills-link-$testTaxYearTo a").attr("href") shouldBe expectedUrl
      }

      "have a link to payments" in new Setup(unpaidFinancialTransactions, paymentEnabled = true) {
        val testTaxYearTo = unpaidFinancialTransactions.head.financialTransactions.get.head.taxPeriodTo.get.getYear
        val testTaxYearFrom = unpaidFinancialTransactions.head.financialTransactions.get.head.taxPeriodFrom.get.getYear

        pageDocument.getElementById(s"payment-link-$testTaxYearTo").text shouldBe paymentDueMessages.payNow
        pageDocument.select(s"#payment-link-$testTaxYearTo a").attr("aria-label") shouldBe paymentDueMessages.payNowAria(testTaxYearFrom.toString, testTaxYearTo.toString)

        pageDocument.select(s"#payment-link-$testTaxYearTo a")
          .attr("href") shouldBe controllers.routes.PaymentController.paymentHandoff(unpaidFinancialTransactions.head.financialTransactions.get.head.outstandingAmount.get.toPence).url
      }

    }

    "without any bills" should {

      s"have the title '${paymentDueMessages.title}'" in new Setup(List.empty) {

        pageDocument.title() shouldBe paymentDueMessages.title
      }

      "state that you've had no bills" in new Setup(List.empty) {
        pageDocument.getElementById("payments-due-none").text shouldBe paymentDueMessages.noBills
      }


    }
  }

}
