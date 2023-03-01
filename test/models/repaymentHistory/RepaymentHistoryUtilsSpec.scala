/*
 * Copyright 2023 HM Revenue & Customs
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

package models.repaymentHistory


import models.financialDetails.Payment
import models.repaymentHistory.RepaymentHistoryUtils._
import org.scalatest.Matchers
import testUtils.TestSupport

import java.time.LocalDate

class RepaymentHistoryUtilsSpec extends TestSupport with Matchers {

  val repaymentRequestNumber: String = "000000003135"

  val payments: List[Payment] = List(
    Payment(reference = Some("mfa1"), amount = Some(-10000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("mfa1"), lot = None, lotItem = None, dueDate = None,
      documentDate = LocalDate.parse("2020-04-13"), Some("AY777777202201"), mainType = Some("ITSA Overpayment Relief")),
    Payment(reference = Some("mfa2"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("mfa2"), lot = None, lotItem = None, dueDate = None,
      documentDate = LocalDate.parse("2020-04-12"), Some("AY777777202210"), mainType = Some("ITSA Overpayment Relief")),
    Payment(reference = Some("cutover1"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("desc1"), lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2019-12-25")),
      documentDate = LocalDate.parse("2019-04-25"), Some("AY777777202202"), mainType = Some("ITSA Cutover Credits")),
    Payment(reference = Some("payment1"), amount = Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"),
      Some(LocalDate.parse("2019-12-26")), LocalDate.parse("2019-12-25"), Some("DOCID01"),
      mainType = Some("SA Balancing Charge")),
    Payment(Some("payment2"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some(LocalDate.parse("2019-12-25")),
      LocalDate.parse("2019-12-25"), Some("DOCID02"), mainType = Some("SA Balancing Charge")),
    Payment(reference = Some("bcc1"), amount = Some(-12000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("bcc1"), lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2019-12-24")),
      documentDate = LocalDate.parse("2019-04-24"), Some("AY777777202203"), mainType = Some("SA Balancing Charge Credit")),
  )

  val repaymentHistory = List(
    RepaymentHistory(
      amountApprovedforRepayment = Some(100.0),
      amountRequested = 200.0,
      repaymentMethod = Some("BACD"),
      totalRepaymentAmount = Some(300.0),
      repaymentItems = Some(Seq[RepaymentItem](
        RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002231"),
              amount = Some(400.0),
              fromDate = Some(LocalDate.parse("2021-07-23")),
              toDate = Some(LocalDate.parse("2021-08-23")),
              rate = Some(12.12)
            )
          )
        )
      )),
      estimatedRepaymentDate = Some(LocalDate.parse("2021-08-21")),
      creationDate = Some(LocalDate.parse("2021-07-21")),
      repaymentRequestNumber = repaymentRequestNumber
    ),
    RepaymentHistory(
      amountApprovedforRepayment = Some(100.0),
      amountRequested = 200.0,
      repaymentMethod = Some("BACD"),
      totalRepaymentAmount = Some(301.0),
      repaymentItems = Some(Seq[RepaymentItem](
        RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002231"),
              amount = Some(400.0),
              fromDate = Some(LocalDate.parse("2021-07-23")),
              toDate = Some(LocalDate.parse("2021-08-23")),
              rate = Some(12.12)
            )
          )
        )
      )),
      estimatedRepaymentDate = Some(LocalDate.parse("2021-08-20")),
      creationDate = Some(LocalDate.parse("2021-07-21")),
      repaymentRequestNumber = repaymentRequestNumber
    ))

  private def groupedRepayments(isAgent: Boolean = false) = List(
    (2021, List(PaymentHistoryEntry(LocalDate.parse("2021-08-20"), "paymentHistory.refund", Some(301.0), None, s"${if (isAgent) "agents/" else ""}refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber),
      PaymentHistoryEntry(LocalDate.parse("2021-08-21"), "paymentHistory.refund", Some(300.0), None, s"${if (isAgent) "agents/" else ""}refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber))),
  )

  private def groupedPayments(cutoverEnabled: Boolean = true, mfaEnabled: Boolean = true, isAgent: Boolean = false) = {
    val bcc = List(PaymentHistoryEntry(LocalDate.parse("2019-12-24"), "paymentHistory.balancingChargeCredit", Some(-12000.0), None,
      s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2019", "AY777777202203"))
    val cutover = if (cutoverEnabled) List(PaymentHistoryEntry(LocalDate.parse("2019-12-25"), "paymentHistory.paymentFromEarlierYear", Some(-11000.0), None,
      s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2019", "AY777777202202")) else Nil
    val standardPayments = List(
      PaymentHistoryEntry(LocalDate.parse("2019-12-25"), "paymentHistory.paymentToHmrc", Some(10000), Some("DOCID02"), s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=DOCID02", "2019-12-25 &pound;10,000.00"),
      PaymentHistoryEntry(LocalDate.parse("2019-12-26"), "paymentHistory.paymentToHmrc", Some(10000), Some("DOCID01"), s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=DOCID01", "2019-12-26 &pound;10,000.00")
    )
    val mfa = if (mfaEnabled) List((2020, List(PaymentHistoryEntry(LocalDate.parse("2020-04-12"), "paymentHistory.mfaCredit", Some(-11000.0), None, s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2020", "AY777777202210"),
      PaymentHistoryEntry(LocalDate.parse("2020-04-13"), "paymentHistory.mfaCredit", Some(-10000.0), None, s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2020", "AY777777202201")))) else List()

    mfa ++ List((2019, bcc ++ cutover ++ standardPayments))
  }

  "RepaymentHistoryUtils" should {
    "getGroupedPaymentHistoryData should combine payments and repayments and group by year" when {
      "both payments and repayments are present" in {
        getGroupedPaymentHistoryData(payments, repaymentHistory, isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedRepayments() ++ groupedPayments()
      }

      "only payments are present" in {
        getGroupedPaymentHistoryData(payments, List(), isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedPayments()
      }

      "only repayments are present" in {
        getGroupedPaymentHistoryData(List(), repaymentHistory, isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedRepayments()
      }

      "cutover credits are NOT present when switch is OFF" in {
        getGroupedPaymentHistoryData(payments, List(), isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = false, languageUtils
        )(messages) shouldBe groupedPayments(false, true)
      }

      "mfa credits are NOT present when switch is OFF" in {
        getGroupedPaymentHistoryData(payments, List(), isAgent = false,
          MFACreditsEnabled = false, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedPayments(true, false)
      }
    }
  }
}
