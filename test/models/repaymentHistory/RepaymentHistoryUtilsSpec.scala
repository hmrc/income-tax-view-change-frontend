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

package models.repaymentHistory


import models.financialDetails.Payment
import models.repaymentHistory.RepaymentHistoryUtils._
import org.scalatest.Matchers
import testUtils.{TestSupport}

import java.time.LocalDate

class RepaymentHistoryUtilsSpec extends TestSupport with Matchers {

  val paymentsWithMFA: List[Payment] = List(
    Payment(reference = Some("mfa1"), amount = Some(-10000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("ITSA Overpayment Relief"), lot = None, lotItem = None, dueDate = None,
      documentDate = "2020-04-13", Some("AY777777202201")),
    Payment(reference = Some("mfa2"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("ITSA Overpayment Relief"), lot = None, lotItem = None, dueDate = None,
      documentDate = "2020-04-12", Some("AY777777202210")),
    Payment(reference = Some("cutover1"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("desc1"), lot = None, lotItem = None, dueDate = Some("2019-12-25"),
      documentDate = "2019-04-25", Some("AY777777202202")),
    Payment(reference = Some("payment1"), amount = Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"),
      Some("2019-12-26"), "2019-12-25", Some("DOCID01")),
    Payment(Some("payment2"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some("2019-12-25"),
      "2019-12-25", Some("DOCID02"))
  )

  val repaymentHistory = List(
    RepaymentHistory(
      amountApprovedforRepayment = Some(100.0),
      amountRequested = 200.0,
      repaymentMethod = "BACD",
      totalRepaymentAmount = 300.0,
      repaymentItems = Seq[RepaymentItem](
        RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002231"),
              amount = Some(400.0),
              fromDate = Some(LocalDate.parse("2021-07-23")),
              toDate = Some(LocalDate.parse("2021-08-23")),
              rate = Some(500.0)
            )
          )
        )
      ),
      estimatedRepaymentDate = LocalDate.parse("2021-08-21"),
      creationDate = LocalDate.parse("2021-07-21"),
      repaymentRequestNumber = "000000003135"
    ),
    RepaymentHistory(
      amountApprovedforRepayment = Some(100.0),
      amountRequested = 200.0,
      repaymentMethod = "BACD",
      totalRepaymentAmount = 301.0,
      repaymentItems = Seq[RepaymentItem](
        RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002231"),
              amount = Some(400.0),
              fromDate = Some(LocalDate.parse("2021-07-23")),
              toDate = Some(LocalDate.parse("2021-08-23")),
              rate = Some(500.0)
            )
          )
        )
      ),
      estimatedRepaymentDate = LocalDate.parse("2021-08-20"),
      creationDate = LocalDate.parse("2021-07-21"),
      repaymentRequestNumber = "000000003135"
    ))

  private def groupedRepayments(isAgent: Boolean = false) = List(
    (2021, List(PaymentHistoryEntry("2021-08-20", "paymentHistory.refund", Some(301.0), None, s"${if (isAgent) "agents/" else ""}refund-to-taxpayer", "000000003135"),
      PaymentHistoryEntry("2021-08-21", "paymentHistory.refund", Some(300.0), None, s"${if (isAgent) "agents/" else ""}refund-to-taxpayer", "000000003135"))),
  )

  private def groupedPayments(cutoverEnabled: Boolean = true, mfaEnabled: Boolean = true, isAgent: Boolean = false) = {
    val cutover = if (cutoverEnabled) List(PaymentHistoryEntry("2019-12-25", "paymentHistory.paymentFromEarlierYear", Some(-11000.0), None,
      s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=AY777777202202", "AY777777202202")) else Nil
    val standardPayments = List(
      PaymentHistoryEntry("2019-12-25", "paymentHistory.paymentToHmrc", Some(10000), Some("DOCID02"), s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=DOCID02", "2019-12-25 &pound;10,000.00"),
      PaymentHistoryEntry("2019-12-26", "paymentHistory.paymentToHmrc", Some(10000), Some("DOCID01"), s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=DOCID01", "2019-12-26 &pound;10,000.00")
    )
    val mfa = if (mfaEnabled) List((2020, List(PaymentHistoryEntry("2020-04-12", "paymentHistory.mfaCredit", Some(-11000.0), None, s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2020", "AY777777202210"),
      PaymentHistoryEntry("2020-04-13", "paymentHistory.mfaCredit", Some(-10000.0), None, s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2020", "AY777777202201")))) else List()

    mfa ++ List((2019, cutover ++ standardPayments))
  }

  "RepaymentHistoryUtils" should {
    "getGroupedPaymentHistoryData should combine payments and repayments and group by year" when {
      "both payments and repayments are present" in {
        getGroupedPaymentHistoryData(paymentsWithMFA, repaymentHistory, isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedRepayments() ++ groupedPayments()
      }

      "only payments are present" in {
        getGroupedPaymentHistoryData(paymentsWithMFA, List(), isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedPayments()
      }

      "only repayments are present" in {
        getGroupedPaymentHistoryData(List(), repaymentHistory, isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedRepayments()
      }

      "cutovercredits are NOT present when switch is OFF" in {
        getGroupedPaymentHistoryData(paymentsWithMFA, List(), isAgent = false,
          MFACreditsEnabled = true, CutOverCreditsEnabled = false, languageUtils
        )(messages) shouldBe groupedPayments(false, true)
      }

      "mfa credits are NOT present when switch is OFF" in {
        getGroupedPaymentHistoryData(paymentsWithMFA, List(), isAgent = false,
          MFACreditsEnabled = false, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedPayments(true, false)
      }

      "agent links present when isAgent is TRUE" in {
        getGroupedPaymentHistoryData(paymentsWithMFA, repaymentHistory, isAgent = true,
          MFACreditsEnabled = true, CutOverCreditsEnabled = true, languageUtils
        )(messages) shouldBe groupedRepayments(isAgent = true) ++ groupedPayments(isAgent = true)
      }
    }
  }
}
