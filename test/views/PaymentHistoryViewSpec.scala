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

import config.FrontendAppConfig
import exceptions.MissingFieldException
import implicits.ImplicitCurrencyFormatter._
import implicits.ImplicitDateFormatter
import models.financialDetails.Payment
import models.repaymentHistory.PaymentHistoryEntry
import org.jsoup.nodes.Element
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.appConfig.saForAgents
import testUtils.ViewSpec
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.PaymentHistory

import java.time.LocalDate
import scala.collection.mutable.ListBuffer


class PaymentHistoryViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  lazy val paymentHistoryView: PaymentHistory = app.injector.instanceOf[PaymentHistory]

  object PaymentHistoryMessages {
    val heading: String = messages("paymentHistory.heading")
    val head: String = messages("Payment and refund history")
    val title: String = messages("titlePattern.serviceName.govUk", heading)
    val titleWhenAgentView: String = messages("agent.titlePattern.serviceName.govUk", heading)

    val info: String = s"${messages("PaymentHistory.classicSA")} ${messages("taxYears.oldSa.content.link")}${messages("pagehelp.opensInNewTabText")}."

    def button(year: Int): String = s"$year payments"

    val paymentToHmrc: String = messages("paymentHistory.paymentToHmrc")

    val CardRef: String = messages("paymentsHistory.CardRef")
    val earlierPaymentToHMRC: String = messages("paymentAllocation.earlyTaxYear.heading")

    val paymentHeadingDate: String = messages("paymentHistory.table.header.date")
    val paymentHeadingDescription: String = messages("paymentHistory.table.header.description")
    val paymentHeadingAmount: String = messages("paymentHistory.table.header.amount")
    val partialH2Heading = "payments"
    val saLink: String = s"${messages("whatYouOwe.sa-link")}${messages("pagehelp.opensInNewTabText")}"
    val pageHeading:  String = s"${messages("home.paymentHistoryRefund.heading")}"
  }

  val paymentEntriesMFA = List(
    (2020, List(PaymentHistoryEntry(date = "2020-04-13", description = "desc1", amount = Some(-10000.00), transactionId = Some("TRANS123"),
      linkUrl = "link1", visuallyHiddenText = "hidden-text1"),
      PaymentHistoryEntry(date = "2020-12-25", description = "desc1", amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1"))),
    (2021, List(PaymentHistoryEntry(date = "2019-04-25", description = "desc1", amount = Some(-10000.00), transactionId = Some("TRANS123"),
      linkUrl = "link1", visuallyHiddenText = "hidden-text1"),
      PaymentHistoryEntry(date = "2018-04-25", description = "desc1", amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1"))),
    (2022, List(PaymentHistoryEntry(date = "2019-09-25", description = "desc1", amount = Some(-10000.00), transactionId = Some("TRANS123"),
      linkUrl = "link1", visuallyHiddenText = "hidden-text1"),
      PaymentHistoryEntry(date = "2019-12-25", description = "desc1", amount = Some(-10000.00), transactionId = Some("TRANS123"),
        linkUrl = "link1", visuallyHiddenText = "hidden-text1")))
  )

  val expectedDatesOrder = List("13 April 2020", "25 December 2020", "25 April 2019", "25 September 2019", "25 December 2019", "25 April 2018")

  val emptyPayments = List(
    (2021, List(PaymentHistoryEntry(date = "2019-09-25", description = "desc1", amount = None, transactionId = Some("TRANS123"),
      linkUrl = "link1", visuallyHiddenText = "hidden-text1")))
  )

  class PaymentHistorySetup(testPayments: List[(Int, List[PaymentHistoryEntry])], saUtr: Option[String] = Some("1234567890"),
                            isAgent: Boolean = false) extends Setup(
    paymentHistoryView(testPayments, paymentHistoryAndRefundsEnabled = true,
      "testBackURL", saUtr, isAgent = isAgent)(FakeRequest(), implicitly)
  )

//  class PaymentHistorySetup1(paymentsnotFull: List[(Int, List[PaymentHistoryEntry])], saUtr: Option[String] = Some("1234567890")) extends Setup(
//    paymentHistoryView(paymentsnotFull, "testBackURL", saUtr, isAgent = false)(FakeRequest(), implicitly)
//  )

//  class PaymentHistorySetupMFA(testPayments: List[(Int, List[PaymentHistoryEntry])], MFACreditsEnabled: Boolean, saUtr: Option[String] = Some("1234567890")) extends Setup(
//    paymentHistoryView(testPayments, "testBackURL", saUtr, isAgent = false)(FakeRequest(), implicitly)
//  )

  val paymentHistoryMessageInfo = s"${messages("paymentHistory.info")} ${messages("taxYears.oldSa.agent.content.2")}${messages("pagehelp.opensInNewTabText")}. ${messages("paymentHistory.info.2")}"

  "The payments history view with payment response model" should {
    "when the user has payment history for a single Year" should {
      s"have the title '${PaymentHistoryMessages.title}'" in new PaymentHistorySetup(paymentEntriesMFA) {
        document.title() shouldBe PaymentHistoryMessages.title
        layoutContent.selectHead("h1").text shouldBe PaymentHistoryMessages.head
        layoutContent.selectHead("h2").text.contains(PaymentHistoryMessages.partialH2Heading)
      }

      s"has a table of payment history" which {
        s"has the table caption" in new PaymentHistorySetup(paymentEntriesMFA) {
          layoutContent.selectHead("div").selectNth("div", 2).selectHead("table")
            .selectHead("caption").text.contains(PaymentHistoryMessages.partialH2Heading)
        }
        s"has table headings for each table column" in new PaymentHistorySetup(paymentEntriesMFA) {
          val row: Element = layoutContent.selectHead("div").selectNth("div", 2).selectHead("table").selectHead("thead").selectHead("tr")
          row.selectNth("th", 1).text shouldBe PaymentHistoryMessages.paymentHeadingDate
          row.selectNth("th", 2).text shouldBe PaymentHistoryMessages.paymentHeadingDescription
          row.selectNth("th", 3).text shouldBe PaymentHistoryMessages.paymentHeadingAmount
        }
      }



      s"have the information  ${PaymentHistoryMessages.info}" in new PaymentHistorySetup(paymentEntriesMFA) {
        layoutContent.select(Selectors.p).text shouldBe PaymentHistoryMessages.info
        layoutContent.selectFirst(Selectors.p).hasCorrectLink(PaymentHistoryMessages.saLink, "http://localhost:8930/self-assessment/ind/1234567890/account")
      }

      s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(paymentEntriesMFA, None) {
        layoutContent.select("#payment-history-info").text should not be PaymentHistoryMessages.info
      }

      "display payment history by year" in new PaymentHistorySetup(paymentEntriesMFA) {
        for (((year, payments), index) <- paymentEntriesMFA.zipWithIndex) {
          layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
          val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.date).toLongDate
              row.selectNth("td", 2).text shouldBe s"desc1 hidden-text1 Item ${index + 1}"
              row.selectNth("td", 2).select("a").attr("href") shouldBe s"link1"
              row.selectNth("td", 3).text shouldBe payment.amount.get.abs.toCurrencyString
          }
        }
      }

      s"Display the heading ${PaymentHistoryMessages.pageHeading}" in new PaymentHistorySetup(paymentEntriesMFA) {
        layoutContent.selectHead("h1").text shouldBe PaymentHistoryMessages.pageHeading
      }
    }
  }

  "The payments history view with payment response model when logged as an Agent" should {
    s"have the information ${messages("paymentHistory.info")}" in new PaymentHistorySetup(paymentEntriesMFA, isAgent = true) {
      layoutContent.select(Selectors.p).text shouldBe paymentHistoryMessageInfo
      layoutContent.selectFirst(Selectors.p).hasCorrectLink(s"${messages("taxYears.oldSa.agent.content.2")}${messages("pagehelp.opensInNewTabText")}", saForAgents)
    }

    s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(paymentEntriesMFA, None, isAgent = true) {
      layoutContent.select("#payment-history-info").text should not be paymentHistoryMessageInfo
    }
  }

//  class PaymentHistorySetupWhenAgentView(testPayments: List[(Int, List[PaymentHistoryEntry])], saUtr: Option[String] = Some("1234567890")) extends Setup(
//    paymentHistoryView(paymentEntriesMFA, "testBackURL", saUtr, isAgent = true)(FakeRequest(), implicitly)
//  )


}
