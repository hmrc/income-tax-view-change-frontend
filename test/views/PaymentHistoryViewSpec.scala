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
import org.jsoup.nodes.Element
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.appConfig.saForAgents
import testUtils.ViewSpec
import views.html.PaymentHistory

import java.time.LocalDate


class PaymentHistoryViewSpec extends ViewSpec with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  lazy val paymentHistoryView: PaymentHistory = app.injector.instanceOf[PaymentHistory]

  object PaymentHistoryMessages {
    val heading: String = messages("paymentHistory.heading")
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
  }

  val testPayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"),
      "2019-12-25", Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, Some("lot"), Some("lotitem"), Some("2007-03-23"),
      "2019-12-25", Some("DOCID02"))
  )

  val testNoPaymentLot: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, None, None, Some("2019-12-25"), "2019-12-25", Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, None, None, Some("2007-03-23"), "2007-03-23", Some("DOCID02"))
  )

  val paymentsnotFull: List[Payment] = List(
    Payment(reference = Some("reference"), amount = Some(-10000.00), None, method = Some("method"), None, lot = None,
      lotItem = None, dueDate = Some("2018-04-25"), documentDate = "2018-04-25", Some("AY777777202206"))
  )

  val paymentsMFA: List[Payment] = List(
    Payment(reference = Some("reference"), amount = Some(-10000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("ITSA Overpayment Relief"), lot = None, lotItem = None, dueDate = None,
      documentDate = "2020-04-25", Some("AY777777202201")),
    Payment(reference = Some("reference"), amount = Some(-10000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("ITSA Literary Artistic Spread"), lot = None, lotItem = None, dueDate = None,
      documentDate = "2019-04-25", Some("AY777777202202")),
    Payment(reference = Some("reference"), amount = Some(-10000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("ITSA Post Cessation Claim"), lot = None, lotItem = None, dueDate = None,
      documentDate = "2018-04-25", Some("AY777777202203"))
  )


  val emptyPayments: List[Payment] = List(
    Payment(reference = None, amount = None, outstandingAmount = None, method = None, documentDescription = None,
      lot = None, lotItem = None, dueDate = None, documentDate = "", transactionId = None)
  )

  class PaymentHistorySetup(testPayments: List[Payment], saUtr: Option[String] = Some("1234567890"), isAgent: Boolean = false) extends Setup(
    paymentHistoryView(testPayments, CutOverCreditsEnabled = false, "testBackURL", saUtr, isAgent = isAgent)(FakeRequest(), implicitly)
  )

  class PaymentHistorySetup1(paymentsnotFull: List[Payment], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(paymentsnotFull, CutOverCreditsEnabled = true, "testBackURL", saUtr, isAgent = false)(FakeRequest(), implicitly)
  )

  class PaymentHistorySetupMFA(testPayments: List[Payment], MFACreditsEnabled: Boolean, saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(testPayments, CutOverCreditsEnabled = true, "testBackURL", saUtr, isAgent = false,
      MFACreditsEnabled = MFACreditsEnabled)(FakeRequest(), implicitly)
  )

  val testMultiplePayments: List[Payment] = List(
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"), "2019-12-25", Some("DOCID01")),
    Payment(Some("AAAAA"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"), "2019-12-25", Some("DOCID01")),
    Payment(Some("AAAAA"), Some(90000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"), "2019-12-25", Some("DOCID01")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, Some("lot"), Some("lotitem"), Some("2007-03-23"), "2007-03-23", Some("DOCID02")),
    Payment(Some("BBBBB"), Some(5000), None, Some("tnemyap"), None, Some("lot"), Some("lotitem"), Some("2007-03-23"), "2007-03-23", Some("DOCID02"))
  )

  val paymentHistoryMessageInfo = s"${messages("paymentHistory.info")} ${messages("taxYears.oldSa.agent.content.2")}${messages("pagehelp.opensInNewTabText")}. ${messages("paymentHistory.info.2")}"

  "The payments history view with payment response model" should {
    "when the user has payment history for a single Year" should {
      s"have the title '${PaymentHistoryMessages.title}'" in new PaymentHistorySetup(testPayments) {
        document.title() shouldBe PaymentHistoryMessages.title
      }

      s"have the h1 heading '${PaymentHistoryMessages.heading}'" in new PaymentHistorySetup(testPayments) {
        layoutContent.selectHead("h1").text shouldBe PaymentHistoryMessages.heading
      }

      s"has the h2 heading '${PaymentHistoryMessages.heading}'" in new PaymentHistorySetup(testPayments) {
        layoutContent.selectHead("h2").text.contains(PaymentHistoryMessages.partialH2Heading)
      }

      s"has a table of payment history" which {
        s"has the table caption" in new PaymentHistorySetup(testPayments) {
          layoutContent.selectHead("div").selectNth("div", 2).selectHead("table")
            .selectHead("caption").text.contains(PaymentHistoryMessages.partialH2Heading)
        }
        s"has table headings for each table column" in new PaymentHistorySetup(testPayments) {
          val row: Element = layoutContent.selectHead("div").selectNth("div", 2).selectHead("table").selectHead("thead").selectHead("tr")
          row.selectNth("th", 1).text shouldBe PaymentHistoryMessages.paymentHeadingDate
          row.selectNth("th", 2).text shouldBe PaymentHistoryMessages.paymentHeadingDescription
          row.selectNth("th", 3).text shouldBe PaymentHistoryMessages.paymentHeadingAmount
        }
      }

      s"have the information  ${PaymentHistoryMessages.info}" in new PaymentHistorySetup(testPayments) {
        layoutContent.select(Selectors.p).text shouldBe PaymentHistoryMessages.info
        layoutContent.selectFirst(Selectors.p).hasCorrectLink(PaymentHistoryMessages.saLink, "http://localhost:8930/self-assessment/ind/1234567890/account")
      }

      s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(testPayments, None) {
        layoutContent.select("#payment-history-info").text should not be PaymentHistoryMessages.info
      }

      "display payment history by year" in new PaymentHistorySetup(testPayments) {
        val orderedPayments: Map[Int, List[Payment]] = testPayments.groupBy { payment => LocalDate.parse(payment.dueDate.get).getYear }
        for (((year, payments), index) <- orderedPayments.zipWithIndex) {
          layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
          val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.dueDate.get).toLongDate
              row.selectNth("td", 2).text shouldBe PaymentHistoryMessages.paymentToHmrc + s" ${LocalDate.parse(payment.dueDate.get).toLongDate} ${payment.amount.get.toCurrencyString}"
              row.selectNth("td", 2).select("a").attr("href") shouldBe s"/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=${payment.transactionId.get}"
              row.selectNth("td", 3).text shouldBe payment.amount.get.toCurrencyString
          }
        }
      }

      "display payment history by year with CutOverCredits FS On" in new PaymentHistorySetup1(paymentsnotFull) {
        val orderedPayments: Map[Int, List[Payment]] = paymentsnotFull.groupBy { payment => LocalDate.parse(payment.dueDate.get).getYear }
        for (((year, payments), index) <- orderedPayments.zipWithIndex) {
          layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
          val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.dueDate.get).toLongDate
              row.selectNth("td", 2).text shouldBe PaymentHistoryMessages.earlierPaymentToHMRC + s" ${payment.transactionId.get}"
              row.selectNth("td", 2).select("a").attr("href") shouldBe s"/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=${payment.transactionId.get}"
          }
        }
      }

      "display no earlier payment history with FS off" in new PaymentHistorySetup(paymentsnotFull) {
        document.title() shouldBe PaymentHistoryMessages.title
        layoutContent.selectHead("h1").text shouldBe PaymentHistoryMessages.heading
        layoutContent.selectHead("h2").text.contains(PaymentHistoryMessages.partialH2Heading)
        layoutContent.getElementById("paymentFromEarlierYear") shouldBe null
      }

      "display MFA Credits with FS On" in new PaymentHistorySetupMFA(paymentsMFA, true) {
        val orderedPayments: Map[Int, List[Payment]] = paymentsMFA.groupBy { payment =>
          if (payment.validMFACreditDescription()) LocalDate.parse(payment.documentDate).getYear
          else LocalDate.parse(payment.dueDate.get).getYear
        }
        for (((year, payments), index) <- orderedPayments.zipWithIndex) {
          layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
          val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.documentDate).toLongDate
              row.selectNth("td", 2).text shouldBe messages("paymentHistory.mfaCredit") + s" ${payment.transactionId.get}"
              val url = s"/report-quarterly/income-and-expenses/view/credits-from-hmrc/${LocalDate.parse(payment.documentDate).getYear}"
              row.selectNth("td", 2).select("a").attr("href") shouldBe url
          }
        }
      }

      "display MFA Credits with FS Off" in new PaymentHistorySetupMFA(paymentsMFA, false) {
        document.title() shouldBe PaymentHistoryMessages.title
        layoutContent.selectHead("h1").text shouldBe PaymentHistoryMessages.heading
        layoutContent.selectHead("h2").text.contains(PaymentHistoryMessages.partialH2Heading)
        layoutContent.getElementById("mfacredit") shouldBe null
        layoutContent.getElementById("paymentFromEarlierYear") shouldBe null
      }

      "display payment history by year with multiple payments for the same year" in new PaymentHistorySetup(testMultiplePayments) {
        val orderedPayments: Map[Int, List[Payment]] = testMultiplePayments.groupBy { payment => LocalDate.parse(payment.dueDate.get).getYear }
        for (((year, payments), index) <- orderedPayments.zipWithIndex) {
          layoutContent.selectHead(s"#accordion-with-summary-sections-heading-$year").text shouldBe PaymentHistoryMessages.button(year)
          val sectionContent = layoutContent.selectHead(s"#accordion-default-content-${index + 1}")
          val tbody = sectionContent.selectHead("table > tbody")
          payments.zipWithIndex.foreach {
            case (payment, index) =>
              val row = tbody.selectNth("tr", index + 1)
              row.selectNth("td", 1).text shouldBe LocalDate.parse(payment.dueDate.get).toLongDate
              row.selectNth("td", 2).text shouldBe PaymentHistoryMessages.paymentToHmrc + s" ${LocalDate.parse(payment.dueDate.get).toLongDate} ${payment.amount.get.toCurrencyString} Item ${index + 1}"
              row.selectNth("td", 2).select("a").attr("href") shouldBe s"/report-quarterly/income-and-expenses/view/payment-made-to-hmrc?documentNumber=${payment.transactionId.get}"
              row.selectNth("td", 3).text shouldBe payment.amount.get.toCurrencyString
          }
        }
      }
    }
  }

  "The payments history view with payment response model when logged as an Agent" should {
    s"have the information ${messages("paymentHistory.info")}" in new PaymentHistorySetup(testPayments, isAgent = true) {
      layoutContent.select(Selectors.p).text shouldBe paymentHistoryMessageInfo
      layoutContent.selectFirst(Selectors.p).hasCorrectLink(s"${messages("taxYears.oldSa.agent.content.2")}${messages("pagehelp.opensInNewTabText")}", saForAgents)
    }

    s"not have the information  ${PaymentHistoryMessages.info} when no utr is provided" in new PaymentHistorySetup(testPayments, None, isAgent = true) {
      layoutContent.select("#payment-history-info").text should not be paymentHistoryMessageInfo
    }
  }

  "The payments history view with an empty payment response model" should {
    "throw a MissingFieldException" in {
      val thrownException = intercept[MissingFieldException] {
        paymentHistoryView(emptyPayments, CutOverCreditsEnabled = false, "testBackURL", None, isAgent = false)
      }
      thrownException.getMessage shouldBe "Missing Mandatory Expected Field: Payment Date"
    }
  }

  class PaymentHistorySetupWhenAgentView(testPayments: List[Payment], saUtr: Option[String] = Some("1234567890")) extends Setup(
    paymentHistoryView(testPayments, CutOverCreditsEnabled = false, "testBackURL", saUtr, isAgent = true)(FakeRequest(), implicitly)
  )

  "The payments history view with payment response model" should {
    "when the user has payment history for a single Year" should {
      s"have the title '${PaymentHistoryMessages.titleWhenAgentView}'" in new PaymentHistorySetupWhenAgentView(testPayments) {
        document.title() shouldBe PaymentHistoryMessages.titleWhenAgentView
      }
    }
  }
}
