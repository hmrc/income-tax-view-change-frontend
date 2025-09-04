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


import models.financialDetails._
import models.repaymentHistory.RepaymentHistoryUtils._
import org.scalatest.matchers.should.Matchers
import testConstants.ChargeConstants
import testConstants.FinancialDetailsTestConstants.{id1040000123, id1040000124, id1040000125}
import testUtils.TestSupport

import java.time.LocalDate

class RepaymentHistoryUtilsSpec extends TestSupport with Matchers with ChargeConstants {

  val repaymentRequestNumber: String = "000000003135"


  val payments: List[Payment] = List(
    Payment(reference = Some("mfa1"), amount = Some(-10000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("mfa1"), lot = None, lotItem = None, dueDate = None,
      documentDate = LocalDate.parse("2020-04-13"), Some("AY777777202201"), mainType = Some("ITSA Overpayment Relief"), mainTransaction = Some("4004")),
    Payment(reference = Some("mfa2"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("mfa2"), lot = None, lotItem = None, dueDate = None,
      documentDate = LocalDate.parse("2020-04-12"), Some("AY777777202210"), mainType = Some("ITSA Overpayment Relief"), mainTransaction = Some("4004")),
    Payment(reference = Some("cutover1"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("desc1"), lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2019-12-25")),
      documentDate = LocalDate.parse("2019-04-25"), Some("AY777777202202"), mainType = Some("ITSA Cutover Credits"), mainTransaction = Some("6110")),
    Payment(reference = Some("payment1"), amount = Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"),
      Some(LocalDate.parse("2019-12-26")), LocalDate.parse("2019-12-25"), Some("DOCID01"),
      mainType = Some("Payment"), mainTransaction = Some("0060")),
    Payment(Some("payment2"), Some(10000), None, Some("Payment"), None, Some("lot"), Some("lotitem"), Some(LocalDate.parse("2019-12-25")),
      LocalDate.parse("2019-12-25"), Some("DOCID02"),
      mainType = Some("Payment"), mainTransaction = Some("0060")),
    Payment(reference = Some("bcc1"), amount = Some(-12000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("bcc1"), lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2019-12-24")),
      documentDate = LocalDate.parse("2019-04-24"), Some("AY777777202203"), mainType = Some("SA Balancing Charge Credit"), mainTransaction = Some("4905")),
    Payment(reference = Some("rar-poa1"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("desc1"), lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2019-12-23")),
      documentDate = LocalDate.parse("2019-04-20"), Some("AY777777202298"), mainType = Some("SA POA 1 Reconciliation Credit"), mainTransaction = Some("4912")),
    Payment(reference = Some("rar-poa1"), amount = Some(-11000.00), Some(-150.00), method = Some("method"),
      documentDescription = Some("desc1"), lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2019-12-23")),
      documentDate = LocalDate.parse("2019-04-20"), Some("AY777777202299"), mainType = Some("SA POA 2 Reconciliation Credit"), mainTransaction = Some("4914"))
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
      repaymentRequestNumber = repaymentRequestNumber,
      status = RepaymentHistoryStatus("A")
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
      repaymentRequestNumber = repaymentRequestNumber,
      status = RepaymentHistoryStatus("A")
    ),
    RepaymentHistory(
      amountApprovedforRepayment = Some(100.0),
      amountRequested = 200.0,
      repaymentMethod = Some("BACD"),
      totalRepaymentAmount = Some(304.0),
      repaymentItems = Some(Seq[RepaymentItem](
        RepaymentItem(repaymentSupplementItem =
          Seq(
            RepaymentSupplementItem(
              parentCreditReference = Some("002420002233"),
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
      repaymentRequestNumber = repaymentRequestNumber,
      status = RepaymentHistoryStatus("I")
    ))


  val codedOutBcdOrPoaCharges: List[ChargeItem] =
    financialDetailsBalancingChargesCi
      .map(_.copy(
        codedOutStatus = Some(Accepted),
      )) ++ List(poa1WithCodingOutAccepted, poa2WithCodingutAccepted)

  private def groupedRepayments(isAgent: Boolean = false) = List(
    (2021, List(PaymentHistoryEntry(LocalDate.parse("2021-08-20"), Repayment, Some(301.0), None, s"${if (isAgent) "agents/" else ""}refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber),
      PaymentHistoryEntry(LocalDate.parse("2021-08-21"), Repayment, Some(300.0), None, s"${if (isAgent) "agents/" else ""}refund-to-taxpayer/$repaymentRequestNumber", repaymentRequestNumber))),
  )

  private def groupedPayments(cutoverEnabled: Boolean = true, mfaEnabled: Boolean = true, isAgent: Boolean = false) = {
    val bcc = List(PaymentHistoryEntry(LocalDate.parse("2019-12-24"), BalancingChargeCreditType, Some(-12000.0), None,
      s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2019", "AY777777202203"))
    val cutover = if (cutoverEnabled) List(PaymentHistoryEntry(LocalDate.parse("2019-12-25"), CutOverCreditType, Some(-11000.0), None,
      s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2019", "AY777777202202")) else Nil
    val reviewAndReconcileCredits = List(
      PaymentHistoryEntry(LocalDate.parse("2019-12-23"), PoaOneReconciliationCredit, Some(-11000.0), None,
      s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}tax-years/2019/charge?id=AY777777202298", "AY777777202298"),
      PaymentHistoryEntry(LocalDate.parse("2019-12-23"), PoaTwoReconciliationCredit, Some(-11000.0), None,
        s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}tax-years/2019/charge?id=AY777777202299", "AY777777202299"),
    )
    val standardPayments = List(
      PaymentHistoryEntry(LocalDate.parse("2019-12-25"), PaymentType, Some(10000), Some("DOCID02"), s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=DOCID02", "2019-12-25 &pound;10,000.00"),
      PaymentHistoryEntry(LocalDate.parse("2019-12-26"), PaymentType, Some(10000), Some("DOCID01"), s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}payment-made-to-hmrc?documentNumber=DOCID01", "2019-12-26 &pound;10,000.00")
    )
    val mfa = if (mfaEnabled) List((2020, List(PaymentHistoryEntry(LocalDate.parse("2020-04-12"), MfaCreditType, Some(-11000.0), None, s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2020", "AY777777202210"),
      PaymentHistoryEntry(LocalDate.parse("2020-04-13"), MfaCreditType, Some(-10000.0), None, s"/report-quarterly/income-and-expenses/view/${if (isAgent) "agents/" else ""}credits-from-hmrc/2020", "AY777777202201")))) else List()

    val codedOutBCDCharges = List(
      PaymentHistoryEntry(LocalDate.parse("2023-12-05"), PaymentType, Some(75), Some(id1040000124), "", "2023-12-05 &pound;75.00"),
      PaymentHistoryEntry(LocalDate.parse("2023-12-14"), PaymentType, Some(50), Some(id1040000125), "", "2023-12-14 &pound;50.00")
    )

    val codedOutPoaOneCharge = List(
      PaymentHistoryEntry(LocalDate.parse("2018-03-29"), PaymentType, Some(1000), Some(id1040000123), "", "2018-03-29 &pound;1,000.00")
    )

    val codedOutPoaTwoCharge = List(
      PaymentHistoryEntry(LocalDate.parse("2018-03-29"), PaymentType, Some(400), Some(id1040000124), "", "2018-03-29 &pound;400.00")
    )

    mfa ++ List((2019, reviewAndReconcileCredits ++ bcc ++ cutover ++ codedOutBCDCharges ++ codedOutPoaOneCharge ++ codedOutPoaTwoCharge ++ standardPayments))
  }

  "RepaymentHistoryUtils" should {
    "getGroupedPaymentHistoryData should combine payments and approved repayments and group by year" when {
      "both payments and repayments are present" in {
        getGroupedPaymentHistoryData(payments, repaymentHistory, codedOutBcdOrPoaCharges, isAgent = false, languageUtils
        )(messages, dateService ) shouldBe groupedRepayments() ++ groupedPayments()
      }

      "only payments are present" in {
        getGroupedPaymentHistoryData(payments, List(), codedOutBcdOrPoaCharges, isAgent = false, languageUtils
        )(messages, dateService) shouldBe groupedPayments()
      }

      "only approved repayments are present" in {
        getGroupedPaymentHistoryData(List(), repaymentHistory, codedOutBcdOrPoaCharges, isAgent = false, languageUtils
        )(messages, dateService) shouldBe groupedRepayments()
      }

    }
  }
}
