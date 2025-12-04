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

package helpers.servicemocks

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.{ComponentSpecBase, WiremockHelper}
import play.api.http.Status.{NO_CONTENT, OK}


object PenaltyDetailsStub extends ComponentSpecBase {

  def getUrl(mtdid: String = "123456789"): String =
    s"/penalties/ITSA/etmp/penalties/MTDITID/$mtdid"

  def stubNoPenaltiesDataResponse(mtdid: String): StubMapping = {
    WiremockHelper.stubGet(getUrl(mtdid), NO_CONTENT, "")
  }

  def stubPenaltiesDetailResponse(mtdid: String): StubMapping = {
    WiremockHelper.stubGet(getUrl(mtdid), OK, """{
  "totalisations" : {
    "LSPTotalValue" : 200,
    "penalisedPrincipalTotal" : 2000,
    "LPPPostedTotal" : 165.25,
    "LPPEstimatedTotal" : 15.26
  },
  "lateSubmissionPenalty" : {
    "summary" : {
      "activePenaltyPoints" : 2,
      "inactivePenaltyPoints" : 0,
      "PoCAchievementDate" : "2020-05-07",
      "regimeThreshold" : 2,
      "penaltyChargeAmount" : 145.33
    },
    "details" : [ {
      "penaltyNumber" : "12345678901234",
      "penaltyOrder" : "01",
      "penaltyCategory" : "P",
      "penaltyStatus" : "ACTIVE",
      "FAPIndicator" : "X",
      "penaltyCreationDate" : "2022-10-30",
      "triggeringProcess" : "P123",
      "penaltyExpiryDate" : "2022-10-30",
      "expiryReason" : "EXP",
      "communicationsDate" : "2022-10-30",
      "lateSubmissions" : [ {
        "lateSubmissionID" : "001",
        "taxPeriod" : "23AA",
        "taxReturnStatus" : "Fulfilled",
        "taxPeriodStartDate" : "2022-01-01",
        "taxPeriodEndDate" : "2022-12-31",
        "taxPeriodDueDate" : "2023-02-01",
        "returnReceiptDate" : "2023-02-01"
      } ],
      "appealInformation" : [ {
        "appealStatus" : "99",
        "appealLevel" : "01",
        "appealDescription" : "Late"
      } ],
      "chargeReference" : "CHARGEREF1",
      "chargeAmount" : 200,
      "chargeOutstandingAmount" : 200,
      "chargeDueDate" : "2022-10-30"
    } ]
  },
  "latePaymentPenalty" : {
    "details" : [ {
      "principalChargeReference" : "12345678901234",
      "penaltyCategory" : "LPP1",
      "penaltyStatus" : "P",
      "penaltyAmountAccruing" : 99.99,
      "penaltyAmountPosted" : 1001.45,
      "penaltyAmountPaid" : 1001.45,
      "penaltyAmountOutstanding" : 99.99,
      "LPP1LRCalculationAmount" : 99.99,
      "LPP1LRDays" : "15",
      "LPP1LRPercentage" : 2.21,
      "LPP1HRCalculationAmount" : 99.99,
      "LPP1HRDays" : "31",
      "LPP1HRPercentage" : 2.21,
      "LPP2Days" : "31",
      "LPP2Percentage" : 4.59,
      "penaltyChargeCreationDate" : "2069-10-30",
      "communicationsDate" : "2069-10-30",
      "penaltyChargeReference" : "1234567890",
      "penaltyChargeDueDate" : "2069-10-30",
      "appealInformation" : [ {
        "appealStatus" : "99",
        "appealLevel" : "01",
        "appealDescription" : "Late"
      } ],
      "principalChargeDocNumber" : "123456789012",
      "principalChargeMainTransaction" : "4700",
      "principalChargeSubTransaction" : "1174",
      "principalChargeBillingFrom" : "2069-10-30",
      "principalChargeBillingTo" : "2069-10-30",
      "principalChargeDueDate" : "2069-10-30",
      "timeToPay" : [ {
        "TTPStartDate" : "2023-10-12",
        "TTPEndDate" : "2024-10-12"
      } ]
    } ],
    "manualLPPIndicator" : false
    },
    "breathingSpace" : {
      "BSStartDate" : "2020-04-06",
      "BSEndDate" : "2020-12-30"
    }
  }
  """)
  }


}
