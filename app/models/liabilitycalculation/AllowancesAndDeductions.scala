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

package models.liabilitycalculation

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class AllowancesAndDeductionsViewModel(
                            personalAllowance: Option[BigDecimal] = None,
                            reducedPersonalAllowance: Option[BigDecimal] = None,
                            personalAllowanceBeforeTransferOut: Option[BigDecimal] = None,
                            transferredOutAmount: Option[BigDecimal] = None,
                            pensionContributions: Option[BigDecimal] = None,
                            lossesAppliedToGeneralIncome: Option[BigDecimal] = None,
                            giftOfInvestmentsAndPropertyToCharity: Option[BigDecimal] = None,
                            grossAnnuityPayments: Option[BigDecimal] = None,
                            qualifyingLoanInterestFromInvestments: Option[BigDecimal] = None,
                            postCessationTradeReceipts: Option[BigDecimal] = None,
                            paymentsToTradeUnionsForDeathBenefits: Option[BigDecimal] = None,
                            totalAllowancesAndDeductions: Option[BigDecimal] = None,
                            totalReliefs: Option[BigDecimal] = None
                          ) {
  val totalAllowancesDeductionsReliefs: Option[BigDecimal] = (totalAllowancesAndDeductions ++ totalReliefs).reduceOption(_ + _)

  val personalAllowanceDisplayValue: Option[BigDecimal] =
    personalAllowanceBeforeTransferOut.fold(reducedPersonalAllowance.fold(personalAllowance)(Some(_)))(Some(_))
}

case class AllowancesAndDeductions(
                                    personalAllowance: Option[Int] = None,
                                    marriageAllowanceTransferOut: Option[MarriageAllowanceTransferOut] = None,
                                    reducedPersonalAllowance: Option[Int] = None,
                                    giftOfInvestmentsAndPropertyToCharity: Option[Int] = None,
                                    lossesAppliedToGeneralIncome: Option[Int] = None,
                                    qualifyingLoanInterestFromInvestments: Option[BigDecimal] = None,
                                    postCessationTradeReceipts: Option[BigDecimal] = None,
                                    paymentsToTradeUnionsForDeathBenefits: Option[BigDecimal] = None,
                                    grossAnnuityPayments: Option[BigDecimal] = None,
                                    pensionContributions: Option[BigDecimal] = None
                                  )
//case class AllowancesAndDeductions(personalAllowance: Option[BigDecimal] = None,
//                                   reducedPersonalAllowance: Option[BigDecimal] = None,
//                                   personalAllowanceBeforeTransferOut: Option[BigDecimal] = None,
//                                   marriageAllowanceTransfer: Option[BigDecimal] = None,
//                                   totalPensionContributions: Option[BigDecimal] = None,
//                                   lossesAppliedToGeneralIncome: Option[BigDecimal] = None,
//                                   giftOfInvestmentsAndPropertyToCharity: Option[BigDecimal] = None,
//                                   totalAllowancesAndDeductions: Option[BigDecimal] = None,
//                                   totalTaxableIncome: Option[BigDecimal] = None,
//                                   totalReliefs: Option[BigDecimal] = None,
//                                   grossAnnualPayments: Option[BigDecimal] = None,
//                                   qualifyingLoanInterestFromInvestments: Option[BigDecimal] = None,
//                                   postCessationTradeReceipts: Option[BigDecimal] = None,
//                                   paymentsToTradeUnionsForDeathBenefits: Option[BigDecimal] = None
//                                  ) {

object AllowancesAndDeductions {
  implicit val writes: OWrites[AllowancesAndDeductions] = (
    (JsPath \ "personalAllowance").writeNullable[Int] and
      (JsPath \ "marriageAllowanceTransferOut").writeNullable[MarriageAllowanceTransferOut] and
      (JsPath \ "reducedPersonalAllowance").writeNullable[Int] and
      (JsPath \ "giftOfInvestmentsAndPropertyToCharity").writeNullable[Int] and
      (JsPath \ "lossesAppliedToGeneralIncome").writeNullable[Int] and
      (JsPath \ "qualifyingLoanInterestFromInvestments").writeNullable[BigDecimal] and
      (JsPath \ "post-cessationTradeReceipts").writeNullable[BigDecimal] and
      (JsPath \ "paymentsToTradeUnionsForDeathBenefits").writeNullable[BigDecimal] and
      (JsPath \ "grossAnnuityPayments").writeNullable[BigDecimal] and
      (JsPath \ "pensionContributions").writeNullable[BigDecimal]
    ) (unlift(AllowancesAndDeductions.unapply))

  implicit val reads: Reads[AllowancesAndDeductions] = (
    (JsPath \ "personalAllowance").readNullable[Int] and
      (JsPath \ "marriageAllowanceTransferOut").readNullable[MarriageAllowanceTransferOut] and
      (JsPath \ "reducedPersonalAllowance").readNullable[Int] and
      (JsPath \ "giftOfInvestmentsAndPropertyToCharity").readNullable[Int] and
      (JsPath \ "lossesAppliedToGeneralIncome").readNullable[Int] and
      (JsPath \ "qualifyingLoanInterestFromInvestments").readNullable[BigDecimal] and
      (JsPath \ "post-cessationTradeReceipts").readNullable[BigDecimal] and
      (JsPath \ "paymentsToTradeUnionsForDeathBenefits").readNullable[BigDecimal] and
      (JsPath \ "grossAnnuityPayments").readNullable[BigDecimal] and
      (JsPath \ "pensionContributions").readNullable[BigDecimal]
    ) (AllowancesAndDeductions.apply _)
}

case class MarriageAllowanceTransferOut(
                                         personalAllowanceBeforeTransferOut: BigDecimal,
                                         transferredOutAmount: BigDecimal
                                       )

object MarriageAllowanceTransferOut {
  implicit val format: OFormat[MarriageAllowanceTransferOut] = Json.format[MarriageAllowanceTransferOut]
}
