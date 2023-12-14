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

package audit.models

import auth.MtdItUser
import models.financialDetails.Payment
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.libs.json._
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup._
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class PaymentHistoryResponseAuditModelSpec extends TestSupport {

  val transactionName = "payment-history-response"
  val auditEvent = "PaymentHistoryResponse"
  val paymentFromEarlierYear: String = messages("paymentHistory.paymentFromEarlierYear")

  private def paymentHistoryAuditFullTxm(userType: Option[AffinityGroup] = Some(Individual), MFA: Boolean = true,
                                         CutOver: Boolean = true) = PaymentHistoryResponseAuditModel(
    mtdItUser = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel(testNino, testMtditid, None, List.empty, List.empty),
      btaNavPartial = None,
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = userType,
      arn = if (userType.contains(Agent)) Some(testArn) else None
    ),
    payments = Seq(
      Payment(reference = Some("payment1"), amount = Some(100.00), outstandingAmount = None,
        method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
        dueDate = Some(LocalDate.parse("2018-02-01")), documentDate = LocalDate.parse("2018-02-05"), None),
      Payment(reference = Some("cutover1"), amount = Some(-100.00), outstandingAmount = None,
        method = Some("method"), documentDescription = None, lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2018-02-02")),
        documentDate = LocalDate.parse("2018-02-05"), None, mainType = Some("ITSA Cutover Credits")),
      Payment(reference = Some("cutover2"), amount = Some(-100.00), outstandingAmount = None,
        method = Some("method"), documentDescription = None, lot = None, lotItem = None, dueDate = Some(LocalDate.parse("2018-02-03")),
        documentDate = LocalDate.parse("2018-02-05"), None, mainType = Some("ITSA Cutover Credits")),
      Payment(reference = Some("mfa1"), amount = Some(-100.00), outstandingAmount = None,
        method = Some("method"), mainType = Some("ITSA Overpayment Relief"), lot = None, lotItem = None,
        dueDate = None, documentDate = LocalDate.parse("2018-02-04"), transactionId = None, documentDescription = None),
      Payment(reference = Some("mfa2"), amount = Some(-100.00), outstandingAmount = None,
        method = Some("method"), mainType = Some("ITSA Overpayment Relief"), lot = None, lotItem = None,
        dueDate = None, documentDate = LocalDate.parse("2018-02-05"), transactionId = None, documentDescription = None)
    ),
    CutOverCreditsEnabled = CutOver,
    MFACreditsEnabled = MFA,
  )

  val paymentHistoryAuditMin: PaymentHistoryResponseAuditModel = PaymentHistoryResponseAuditModel(
    mtdItUser = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = None,
      incomeSources = IncomeSourceDetailsModel(testNino, testMtditid, None, List.empty, List.empty),
      saUtr = None,
      credId = None,
      userType = None,
      arn = None
    ),
    payments = Seq.empty[Payment],
    CutOverCreditsEnabled = false,
    MFACreditsEnabled = false,
  )

  def getExpectedJson(MFA: Boolean = true, CutOver: Boolean = true): JsObject = {
    def getCutOver(CutOver:Boolean): JsArray = if (CutOver) Json.arr(
      Json.obj(
        "paymentDate" -> "2018-02-02",
        "description" -> "Credit from an earlier tax year",
        "amount" -> -100.00
      ),
      Json.obj(
        "paymentDate" -> "2018-02-03",
        "description" -> "Credit from an earlier tax year",
        "amount" -> -100.00
      ),
    ) else Json.arr()

    def getMFA(MFA: Boolean): JsArray = if (MFA) Json.arr(
      Json.obj(
      "paymentDate" -> "2018-02-04",
      "description" -> "Credit from HMRC adjustment",
      "amount" -> -100.00
    ),
    Json.obj(
      "paymentDate" -> "2018-02-05",
      "description" -> "Credit from HMRC adjustment",
      "amount" -> -100.00
    )) else Json.arr()

    Json.obj(
      "mtditid" -> testMtditid,
      "nino" -> testNino,
      "saUtr" -> testSaUtr,
      "credId" -> testCredId,
      "userType" -> "Individual",
      "paymentHistory" -> (Json.arr(
        Json.obj(
          "paymentDate" -> "2018-02-01",
          "description" -> "Payment Made to HMRC",
          "amount" -> 100.00
        )
      ) ++ getCutOver(CutOver) ++ getMFA(MFA))
    )
  }

  "The PaymentHistoryRequestAuditModel" should {

    s"Have the correct transaction name of '$transactionName'" in {
      paymentHistoryAuditFullTxm().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      paymentHistoryAuditFullTxm().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" when {
      "Audit expected behaviour" when {
        "CutOverCredits is enabled, MFACredits is enabled" in {
          paymentHistoryAuditFullTxm(MFA = true, CutOver = true).detail shouldBe getExpectedJson(true, true)
        }

        "CutOverCredits is enabled, MFACredits is disabled" in {
          paymentHistoryAuditFullTxm(MFA = false, CutOver = true).detail shouldBe getExpectedJson(false, true)
        }

        "CutOverCredits is disabled, MFACredits is enabled" in {
          paymentHistoryAuditFullTxm(MFA = true, CutOver = false).detail shouldBe getExpectedJson(true, false)
        }

        "CutOverCredits is disabled, MFACredits is disabled" in {
          paymentHistoryAuditFullTxm(MFA = false, CutOver = false).detail shouldBe getExpectedJson(false, false)
        }

        "the audit is empty" in {
          paymentHistoryAuditMin.detail shouldBe Json.obj(
            "mtditid" -> testMtditid,
            "nino" -> testNino,
            "paymentHistory" -> Json.arr()
          )
        }
      }
    }
  }
}

