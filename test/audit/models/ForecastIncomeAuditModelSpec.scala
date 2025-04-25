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

import authV2.AuthActionsTestData.defaultMTDITUser
import models.incomeSourceDetails.IncomeSourceDetailsModel
import models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}
import play.api.libs.json.Json
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class ForecastIncomeAuditModelSpec extends TestSupport {

  val transactionName = "forecast-income"
  val auditEvent = "ForecastIncome"

  val endOfYearEstimate = EndOfYearEstimate(
    incomeSource = Some(List(
      IncomeSource(
        incomeSourceType = "01",
        incomeSourceName = Some("Trading Inc"),
        taxableIncome = 100
      ),
      IncomeSource(
        incomeSourceType = "01",
        incomeSourceName = Some("Sales Co"),
        taxableIncome = 200
      ),
      IncomeSource(
        incomeSourceType = "02",
        incomeSourceName = None,
        taxableIncome = 600
      ),
      IncomeSource(
        incomeSourceType = "03",
        incomeSourceName = None,
        taxableIncome = 400
      ),
      IncomeSource(
        incomeSourceType = "04",
        incomeSourceName = None,
        taxableIncome = 1800
      ),
      IncomeSource(
        incomeSourceType = "05",
        incomeSourceName = Some("Weyland Yutani"),
        taxableIncome = 700
      ),
      IncomeSource(
        incomeSourceType = "05",
        incomeSourceName = Some("Skynet"),
        taxableIncome = 800
      ),
      IncomeSource(
        incomeSourceType = "06",
        incomeSourceName = None,
        taxableIncome = 2300
      ),
      IncomeSource(
        incomeSourceType = "07",
        incomeSourceName = None,
        taxableIncome = 2300
      ),
      IncomeSource(
        incomeSourceType = "08",
        incomeSourceName = None,
        taxableIncome = 2000
      ),
      IncomeSource(
        incomeSourceType = "09",
        incomeSourceName = None,
        taxableIncome = 500
      ),
      IncomeSource(
        incomeSourceType = "10",
        incomeSourceName = None,
        taxableIncome = 500
      ),
      IncomeSource(
        incomeSourceType = "11",
        incomeSourceName = None,
        taxableIncome = 1200
      ),
      IncomeSource(
        incomeSourceType = "12",
        incomeSourceName = None,
        taxableIncome = 1700
      ),
      IncomeSource(
        incomeSourceType = "13",
        incomeSourceName = None,
        taxableIncome = 1800
      ),
      IncomeSource(
        incomeSourceType = "14",
        incomeSourceName = None,
        taxableIncome = 32500
      ),
      IncomeSource(
        incomeSourceType = "15",
        incomeSourceName = None,
        taxableIncome = 1700
      ),
      IncomeSource(
        incomeSourceType = "16",
        incomeSourceName = None,
        taxableIncome = 2100
      ),
      IncomeSource(
        incomeSourceType = "17",
        incomeSourceName = None,
        taxableIncome = 1500
      ),
      IncomeSource(
        incomeSourceType = "18",
        incomeSourceName = None,
        taxableIncome = 2500
      ),
      IncomeSource(
        incomeSourceType = "19",
        incomeSourceName = None,
        taxableIncome = 1000
      ),
      IncomeSource(
        incomeSourceType = "20",
        incomeSourceName = None,
        taxableIncome = 2200
      ),
      IncomeSource(
        incomeSourceType = "21",
        incomeSourceName = None,
        taxableIncome = 1300
      ),
      IncomeSource(
        incomeSourceType = "22",
        incomeSourceName = None,
        taxableIncome = 2100
      ),
      IncomeSource(
        incomeSourceType = "98",
        incomeSourceName = None,
        taxableIncome = 1500
      )
    )),
    totalEstimatedIncome = Some(12500),
    totalTaxableIncome = Some(12500),
    incomeTaxAmount = Some(5000.99),
    nic2 = Some(5000.99),
    nic4 = Some(5000.99),
    totalNicAmount = Some(5000.99),
    totalTaxDeductedBeforeCodingOut = Some(5000.99),
    saUnderpaymentsCodedOut = Some(5000.99),
    totalStudentLoansRepaymentAmount = Some(5000.99),
    totalAnnuityPaymentsTaxCharged = Some(5000.99),
    totalRoyaltyPaymentsTaxCharged = Some(5000.99),
    totalTaxDeducted = Some(-99999999999.99),
    incomeTaxNicAmount = Some(-99999999999.99),
    cgtAmount = Some(5000.99),
    incomeTaxNicAndCgtAmount = Some(5000.99)
  )

  val testUserIndividual = defaultMTDITUser(Some(Individual),
    IncomeSourceDetailsModel(testNino ,testMtditid, None, Nil, Nil))

  val testUserAgent = defaultMTDITUser(Some(Agent),
    IncomeSourceDetailsModel(testNino ,testMtditid, None, Nil, Nil))

  def testForecastIncomeAuditModelIndividual( endOfYearEstimate: EndOfYearEstimate = endOfYearEstimate,
                                            ): ForecastIncomeAuditModel = ForecastIncomeAuditModel(
    user = testUserIndividual,
    endOfYearEstimate = endOfYearEstimate
  )

  def testForecastIncomeAuditModelAgent(endOfYearEstimate: EndOfYearEstimate = endOfYearEstimate,
                                       ): ForecastIncomeAuditModel = ForecastIncomeAuditModel(
    user = testUserAgent,
    endOfYearEstimate = endOfYearEstimate
  )

  "The ForecastIncomeAuditModel" should {

    s"Have the correct transaction name of '$transactionName'" in {
      testForecastIncomeAuditModelIndividual().transactionName shouldBe transactionName
    }

    s"Have the correct audit event type of '$auditEvent'" in {
      testForecastIncomeAuditModelIndividual().auditType shouldBe auditEvent
    }

    "Have the correct details for the audit event" in {
      testForecastIncomeAuditModelIndividual().detail shouldBe commonAuditDetails(Individual) ++ Json.obj(
        "profitFrom" -> Json.arr(
          Json.obj(
            "name" -> "Trading Inc",
            "amount" -> 100,
          ),
          Json.obj(
            "name" -> "Sales Co",
            "amount" -> 200,
          )
        ),
        "payFrom" -> Json.arr(
          Json.obj(
            "name" -> "Weyland Yutani",
            "amount" -> 700,
          ),
          Json.obj(
            "name" -> "Skynet",
            "amount" -> 800,
          )
        ),
        "profitFromUKFurnishedHolidayLettings" -> 1800,
        "profitFromUKLandandProperty" -> 1700,
        "capitalGains" -> 2100,
        "profitFromUKLandandProperty" -> 600,
        "dividendsFromForeignCompanies" -> 2300,
        "nonPayeIncome" -> 1300,
        "UKSecurities" -> 2500,
        "dividendsFromUKCompanies" -> 500,
        "profitFromEEAholidayPropertyLettings" -> 400,
        "profitFromForeignLand" -> 1700,
        "profitsFromTrustsAndEstates" -> 2000,
        "shareSchemes" -> 1800,
        "foreignInterest" -> 2100,
        "totalForecastIncome" -> 12500,
        "gainsOnLifeInsurance" -> 1700,
        "otherIncome" -> 1000,
        "giftAidAndPayrollGiving" -> 1500,
        "otherDividends" -> 1500,
        "foreignIncome" -> 2300,
        "foreignPension" -> 2200,
        "interestFromBanks" -> 500,
        "profitFromPartnership" -> 32500,
        "stateBenefitIncome" -> 1200
      )
    }

    "Have the agent details for the audit event" in {
      val agentJson = testForecastIncomeAuditModelAgent().detail
      (agentJson \ "userType").as[String] shouldBe "Agent"
    }
  }
}
