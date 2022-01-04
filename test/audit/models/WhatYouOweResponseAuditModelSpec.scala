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

package audit.models

import java.time.LocalDate
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr}
import testConstants.FinancialDetailsTestConstants.{dueDateOverdue, whatYouOwePartialChargesList}
import auth.MtdItUser
import models.core.AccountingPeriodModel
import models.financialDetails.{BalanceDetails, WhatYouOweChargesList}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.libs.json.{JsValue, Json}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

class WhatYouOweResponseAuditModelSpec extends TestSupport {

  val transactionName = "what-you-owe-response"
  val auditEvent = "WhatYouOweResponse"


  val dueDateInFuture: String = LocalDate.now().plusDays(45).toString
  val dueDateIsSoon: String = LocalDate.now().plusDays(1).toString

  val outStandingCharges: String = LocalDate.now().minusDays(30).toString

  def testWhatYouOweResponseAuditModel(userType: Option[String] = Some("Agent"),
                                       yearOfMigration: Option[String] = Some("2015"),
                                       chargesList: WhatYouOweChargesList = whatYouOwePartialChargesList,
                                      ): WhatYouOweResponseAuditModel = WhatYouOweResponseAuditModel(
    user = MtdItUser(
      mtditid = testMtditid,
      nino = testNino,
      userName = Some(Name(Some("firstName"), Some("lastName"))),
      incomeSources = IncomeSourceDetailsModel(testMtditid, yearOfMigration, List.empty, None),
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = userType,
      arn = if (userType.contains("Agent")) Some(testArn) else None
    ),
    chargesList = chargesList
  )

  "The WhatYouOweResponseAuditModel" should {

    s"Have the correct transaction name of '$transactionName'" in {
      testWhatYouOweResponseAuditModel().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testWhatYouOweResponseAuditModel().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testWhatYouOweResponseAuditModel(Some("Individual")).detail shouldBe Json.obj(
        "userType" -> "Individual",
        "saUtr" -> testSaUtr,
        "nationalInsuranceNumber" -> testNino,
        "credId" -> testCredId,
        "mtditid" -> testMtditid,
        "balanceDetails" -> Json.obj(
          "balanceDueWithin30Days" -> 1.0,
          "overDueAmount" -> 2.0,
          "totalBalance" -> 3.0
        ),
        "charges" -> Json.arr(
          Json.obj(
            "chargeType" -> "Payment on account 1 of 2",
            "dueDate" -> dueDateOverdue.head.get,
            "outstandingAmount" -> 50,
            "chargeUnderReview" -> true
          ),
          Json.obj(
            "chargeType" -> "Payment on account 2 of 2",
            "dueDate" -> dueDateOverdue(1).get,
            "outstandingAmount" -> 75,
            "accruingInterest" -> 24.05,
            "interestRate" -> "6.2%",
            "interestFromDate" -> "2019-05-25",
            "interestEndDate" -> "2019-06-25",
            "chargeUnderReview" -> false
          ),
          Json.obj(
            "chargeType" -> "Payment on account 2 of 2",
            "dueDate" -> dueDateIsSoon,
            "outstandingAmount" -> 100,
            "chargeUnderReview" -> false
          ),
          Json.obj(
            "chargeType" -> "Payment on account 1 of 2",
            "dueDate" -> dueDateInFuture,
            "outstandingAmount" -> 125,
            "chargeUnderReview" -> false
          ),
          Json.obj(
            "chargeType" -> "Late payment interest for payment on account 1 of 2",
            "dueDate" -> "2019-06-25",
            "outstandingAmount" -> 42.5,
            "chargeUnderReview" -> true
          ),
          Json.obj("accruingInterest" -> 12.67,
            "chargeType" -> "Remaining balance",
            "dueDate" -> outStandingCharges,
            "outstandingAmount" -> 123456.67
          )
        )
      )
    }

    "Have the agent details for the audit event" in {
      val agentJson = testWhatYouOweResponseAuditModel(Some("Agent")).detail

      (agentJson \ "userType").as[String] shouldBe "Agent"
      (agentJson \ "agentReferenceNumber").as[String] shouldBe testArn
    }

    "Not include balanceDetails data in details for the audit event" when {
      def balanceDetailsJson(auditModel: WhatYouOweResponseAuditModel): Option[JsValue] =
        (auditModel.detail \ "balanceDetails").toOption


      "user's unknown year of migration" in {
        balanceDetailsJson(testWhatYouOweResponseAuditModel(yearOfMigration = None)) shouldBe None
      }

      "user's first year of migration" in {
        val currentTaxYear = AccountingPeriodModel.determineTaxYearFromPeriodEnd(LocalDate.now)
        balanceDetailsJson(testWhatYouOweResponseAuditModel(yearOfMigration = Some(currentTaxYear.toString))) shouldBe None
      }

      "user's second or more year of migration and balance details contains all zero amounts" in {
        val prevTaxYear = AccountingPeriodModel.determineTaxYearFromPeriodEnd(LocalDate.now) - 1
        val chargesModelWithSomeBalanceDetails = whatYouOwePartialChargesList.copy(
          balanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 0)
        )

        balanceDetailsJson(testWhatYouOweResponseAuditModel(yearOfMigration = Some(prevTaxYear.toString),
          chargesList = chargesModelWithSomeBalanceDetails)) shouldBe None
      }

      "user's second or more year of migration and balance details contains some zero amounts" in {
        val prevTaxYear = AccountingPeriodModel.determineTaxYearFromPeriodEnd(LocalDate.now) - 1
        val chargesModelWithSomeBalanceDetails = whatYouOwePartialChargesList.copy(
          balanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 3)
        )

        balanceDetailsJson(testWhatYouOweResponseAuditModel(yearOfMigration = Some(prevTaxYear.toString),
          chargesList = chargesModelWithSomeBalanceDetails)) shouldBe Some(Json.obj(
          "totalBalance" -> 3.0
        ))
      }
    }
  }
}
