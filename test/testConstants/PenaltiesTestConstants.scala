/*
 * Copyright 2025 HM Revenue & Customs
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

package testConstants

import models.penalties.appealInformation._
import models.penalties.breathingSpace.BreathingSpace
import models.penalties.latePayment._
import models.penalties.lateSubmission._
import models.penalties.{GetPenaltyDetails, Totalisations}
import play.api.libs.json.{JsObject, Json}
import testUtils.TestSupport

import java.time.LocalDate

object PenaltiesTestConstants extends TestSupport {

  val appealInformation: AppealInformationType = AppealInformationType(
    appealStatus = Some(UnappealableStatus),
    appealLevel = Some(HmrcAppealLevel),
    appealDescription = Some("Late")
  )

  val timeToPay: TimeToPay = TimeToPay(
    TTPStartDate = Some(LocalDate.of(2023, 10, 12)),
    TTPEndDate = Some(LocalDate.of(2024, 10, 12))
  )

  val lppDetailsFull: LPPDetails = LPPDetails(
    principalChargeReference = "12345678901234",
    penaltyCategory = FirstPenaltyLPPPenaltyCategory,
    penaltyStatus = PostedLPPPenaltyStatus,
    penaltyAmountAccruing = 99.99,
    penaltyAmountPosted = 1001.45,
    penaltyAmountPaid = Some(1001.45),
    penaltyAmountOutstanding = Some(99.99),
    LPP1LRCalculationAmount = Some(99.99),
    LPP1LRDays = Some("15"),
    LPP1LRPercentage = Some(2.21),
    LPP1HRCalculationAmount = Some(99.99),
    LPP1HRDays = Some("31"),
    LPP1HRPercentage = Some(2.21),
    LPP2Days = Some("31"),
    LPP2Percentage = Some(4.59),
    penaltyChargeCreationDate = Some(LocalDate.of(2069, 10, 30)),
    communicationsDate = Some(LocalDate.of(2069, 10, 30)),
    penaltyChargeReference = Some("1234567890"),
    penaltyChargeDueDate = Some(LocalDate.of(2069, 10, 30)),
    appealInformation = Some(Seq(appealInformation)),
    principalChargeDocNumber = "123456789012",
    principalChargeMainTransaction = "4700",
    principalChargeSubTransaction = "1174",
    principalChargeBillingFrom = LocalDate.of(2069, 10, 30),
    principalChargeBillingTo = LocalDate.of(2069, 10, 30),
    principalChargeDueDate = LocalDate.of(2069, 10, 30),
    principalChargeLatestClearing = None,
    timeToPay = Some(Seq(timeToPay))
  )

  val lppDetailsJson: JsObject = Json.obj(
    "principalChargeReference" -> "12345678901234",
    "penaltyCategory" -> "LPP1",
    "penaltyStatus" -> "P",
    "penaltyAmountAccruing" -> 99.99,
    "penaltyAmountPosted" -> 1001.45,
    "penaltyAmountPaid" -> 1001.45,
    "penaltyAmountOutstanding"-> 99.99,
    "LPP1LRCalculationAmount" -> 99.99,
    "LPP1LRDays" -> "15",
    "LPP1LRPercentage" -> 2.21,
    "LPP1HRCalculationAmount" -> 99.99,
    "LPP1HRDays" -> "31",
    "LPP1HRPercentage" -> 2.21,
    "LPP2Days" -> "31",
    "LPP2Percentage" -> 4.59,
    "penaltyChargeCreationDate" -> "2069-10-30",
    "communicationsDate" -> "2069-10-30",
    "penaltyChargeReference" -> "1234567890",
    "penaltyChargeDueDate" -> "2069-10-30",
    "appealInformation" -> Json.arr(
      Json.obj(
        "appealStatus" -> "99",
        "appealLevel" -> "01",
        "appealDescription" -> "Late"
      )
    ),
    "principalChargeDocNumber" -> "123456789012",
    "principalChargeMainTransaction" -> "4700",
    "principalChargeSubTransaction" -> "1174",
    "principalChargeBillingFrom" -> "2069-10-30",
    "principalChargeBillingTo" -> "2069-10-30",
    "principalChargeDueDate" -> "2069-10-30",
    "timeToPay" -> Json.arr(
      Json.obj(
        "TTPStartDate" -> "2023-10-12",
        "TTPEndDate" -> "2024-10-12"
      )
    )
  )

  val lspSummary: LSPSummary = LSPSummary(
    activePenaltyPoints = 2,
    inactivePenaltyPoints = 0,
    PoCAchievementDate = Some(LocalDate.of(2020, 5, 7)),
    regimeThreshold = 2,
    penaltyChargeAmount = 145.33
  )

  val lateSubmission: LateSubmission = LateSubmission(
    lateSubmissionID = "001",
    taxPeriod = Some("23AA"),
    taxReturnStatus = Some(FulfilledTaxReturnStatus),
    taxPeriodStartDate = Some(LocalDate.of(2022, 1, 1)),
    taxPeriodEndDate = Some(LocalDate.of(2022, 12, 31)),
    taxPeriodDueDate = Some(LocalDate.of(2023, 2, 1)),
    returnReceiptDate = Some(LocalDate.of(2023, 2, 1))
  )

  val lspDetails: LSPDetails = LSPDetails(
    penaltyNumber = "12345678901234",
    penaltyOrder = Some("01"),
    penaltyCategory = Some(PointLSPPenaltyCategory),
    penaltyStatus = ActiveLSPPenaltyStatus,
    FAPIndicator = Some("X"),
    penaltyCreationDate = LocalDate.of(2022, 10, 30),
    triggeringProcess = "P123",
    penaltyExpiryDate = LocalDate.of(2022, 10, 30),
    expiryReason = Some("EXP"),
    communicationsDate = Some(LocalDate.of(2022, 10, 30)),
    lateSubmissions = Option(Seq(lateSubmission)),
    appealInformation = Some(Seq(appealInformation)),
    chargeReference = Some("CHARGEREF1"),
    chargeAmount = Some(200),
    chargeOutstandingAmount = Some(200),
    chargeDueDate = Some(LocalDate.of(2022, 10, 30))
  )

  val lateSubmissionPenalty: LateSubmissionPenalty = LateSubmissionPenalty(lspSummary, Seq(lspDetails))

  val totalisationsModel: Totalisations = Totalisations(
    LSPTotalValue = Some(200),
    penalisedPrincipalTotal = Some(2000),
    LPPPostedTotal = Some(165.25),
    LPPEstimatedTotal = Some(15.26)
  )

  val latePaymentPenaltyModel: LatePaymentPenalty =
    LatePaymentPenalty(
      details = Some(Seq(lppDetailsFull)),
      manualLPPIndicator = Some(false))

  val breathingSpace: BreathingSpace = BreathingSpace(
    BSStartDate = LocalDate.of(2020, 4, 6),
    BSEndDate = LocalDate.of(2020, 12, 30)
  )

  val getPenaltyDetails: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = Some(totalisationsModel),
    lateSubmissionPenalty = Some(lateSubmissionPenalty),
    latePaymentPenalty = Some(latePaymentPenaltyModel),
    breathingSpace = Some(breathingSpace)
  )

}
