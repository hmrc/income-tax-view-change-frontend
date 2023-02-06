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
import models.core.AccountingPeriodModel
import models.financialDetails.{BalanceDetails, WhatYouOweChargesList}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.libs.json.{JsValue, Json}
import testConstants.BaseTestConstants.{testArn, testCredId, testMtditid, testNino, testSaUtr}
import testConstants.FinancialDetailsTestConstants.{dueDateOverdue, whatYouOwePartialChargesList}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class WhatYouOweResponseAuditModelSpec extends TestSupport {

  val transactionName = "what-you-owe-response"
  val auditEvent = "WhatYouOweResponse"
  val lpiPaymentOnAccount1: String = messages("whatYouOwe.lpi.paymentOnAccount1.text")
  val paymentOnAccount1: String = messages("whatYouOwe.paymentOnAccount1.text")
  val paymentOnAccount2: String = messages("whatYouOwe.paymentOnAccount2.text")
  val class2Nic: String = messages("whatYouOwe.class2Nic.text")

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
      btaNavPartial = None,
      saUtr = Some(testSaUtr),
      credId = Some(testCredId),
      userType = userType,
      arn = if (userType.contains("Agent")) Some(testArn) else None
    ),
    whatYouOweChargesList = chargesList,
    dateService,

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
            "chargeType" -> lpiPaymentOnAccount1,
            "dueDate" -> Some(LocalDate.parse("2019-06-25")),
            "outstandingAmount" -> 42.5,
            "chargeUnderReview" -> true,
            "endTaxYear" -> 2023,
            "overDue" -> true
          ),
          Json.obj(
            "chargeType" -> paymentOnAccount2,
            "dueDate" -> dueDateOverdue(1).get,
            "outstandingAmount" -> 75,
            "accruingInterest" -> 24.05,
            "interestRate" -> "6.2%",
            "interestFromDate" -> "2019-05-25",
            "interestEndDate" -> "2019-06-25",
            "chargeUnderReview" -> false,
            "endTaxYear" -> 2023,
            "overDue" -> true
          ),
          Json.obj(
            "chargeType" -> paymentOnAccount2,
            "dueDate" -> dueDateIsSoon,
            "outstandingAmount" -> 100,
            "accruingInterest" -> 100,
            "interestRate" -> "100%",
            "interestFromDate" -> "2018-03-29",
            "interestEndDate" -> "2018-03-29",
            "chargeUnderReview" -> false,
            "endTaxYear" -> 2023,
            "overDue" -> false
          ),
          Json.obj(
            "chargeType" -> paymentOnAccount1,
            "dueDate" -> dueDateInFuture,
            "outstandingAmount" -> 125,
            "accruingInterest" -> 100,
            "interestRate" -> "100%",
            "interestFromDate" -> "2018-03-29",
            "interestEndDate" -> "2018-03-29",
            "chargeUnderReview" -> false,
            "endTaxYear" -> 2023,
            "overDue" -> false
          ),
          Json.obj("accruingInterest" -> 12.67,
            "chargeType" -> "Remaining balance",
            "dueDate" -> outStandingCharges,
            "outstandingAmount" -> 123456.67
          )
        ),
        "codingOut" -> Json.obj( "amountCodedOut" -> 2500,
          "endTaxYear" -> 2022)
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
          balanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 0, None, None, None,None)
        )

        balanceDetailsJson(testWhatYouOweResponseAuditModel(yearOfMigration = Some(prevTaxYear.toString),
          chargesList = chargesModelWithSomeBalanceDetails)) shouldBe None
      }

      "user's second or more year of migration and balance details contains some zero amounts" in {
        val prevTaxYear = AccountingPeriodModel.determineTaxYearFromPeriodEnd(LocalDate.now) - 1
        val chargesModelWithSomeBalanceDetails = whatYouOwePartialChargesList.copy(
          balanceDetails = BalanceDetails(balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 3, None, None, None,None)
        )

        balanceDetailsJson(testWhatYouOweResponseAuditModel(yearOfMigration = Some(prevTaxYear.toString),
          chargesList = chargesModelWithSomeBalanceDetails)) shouldBe Some(Json.obj(
          "totalBalance" -> 3.0
        ))
      }
    }

    "produce a full audit Json model" when {
      "the audit 7b feature switch is on" in {
        val auditJson = testWhatYouOweResponseAuditModel(
          chargesList = whatYouOwePartialChargesList.copy(
            balanceDetails = BalanceDetails(
              balanceDueWithin30Days = 0, overDueAmount = 0, totalBalance = 3, None, None, None, Some(BigDecimal(-1000.0)))
          )
        )

        (auditJson.detail \ "balanceDetails" \ "creditAmount").toString() shouldBe "JsDefined(-1000)"
        (auditJson.detail \ "charges")(0) shouldBe Json.obj(
          "chargeUnderReview" -> true,
          "outstandingAmount" -> 42.5,
          "chargeType" -> lpiPaymentOnAccount1,
          "dueDate" -> "2019-06-25",
          "endTaxYear" -> LocalDate.now().getYear,
          "overDue" -> true
        )
      }
    }
  }
}
