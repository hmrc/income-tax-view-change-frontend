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

package models.liabilitycalculation.view

import models.liabilitycalculation.taxcalculation.{CgtTaxBands, Nic4Bands, TaxBands}
import models.liabilitycalculation.{Calculation, ReliefsClaimed}
import play.api.libs.json.{Json, OFormat}

case class Messages(info: Option[Seq[Message]] = None, warnings: Option[Seq[Message]] = None, errors: Option[Seq[Message]] = None) {
  // When updating the accepted messages also update the audit for the TaxCalculationDetailsResponseAuditModel
  private val acceptedMessages: Seq[String] = Seq("C22202", "C22203", "C22206", "C22207", "C22210", "C22211",
    "C22212", "C22213", "C22214", "C22215", "C22216", "C22217", "C22218")
  val allMessages: Seq[Message] = {
    info.getOrElse(Seq.empty) ++ warnings.getOrElse(Seq.empty) ++ errors.getOrElse(Seq.empty)
  }
  val genericMessages: Seq[Message] = allMessages.filter(message => acceptedMessages.contains(message.id))
}

object Messages {
  implicit val format: OFormat[Messages] = Json.format[Messages]
}

case class Message(id: String, text: String)

object Message {
  implicit val format: OFormat[Message] = Json.format[Message]
}

case class TaxDueSummaryViewModel(
                                   taxRegime: Option[String] = None,
                                   class2VoluntaryContributions: Option[Boolean] = None,
                                   messages: Messages = Messages(),
                                   lossesAppliedToGeneralIncome: Option[Int] = None,
                                   grossGiftAidPayments: Option[Int] = None,
                                   giftAidTax: Option[String] = None,
                                   marriageAllowanceTransferredInAmount: Option[String] = None,
                                   reliefsClaimed: Seq[ReliefsClaimed],
                                   totalResidentialFinanceCostsRelief: Option[BigDecimal] = None,
                                   totalForeignTaxCreditRelief: Option[BigDecimal] = None,
                                   topSlicingReliefAmount: Option[BigDecimal] = None,
                                   totalTaxableIncome: Option[Int] = None,
                                   payPensionsProfit: Seq[TaxBands] = Seq(),
                                   savingsAndGains: Seq[TaxBands] = Seq(),
                                   lumpSums: Seq[TaxBands] = Seq(),
                                   dividends: Seq[TaxBands] = Seq(),
                                   gainsOnLifePolicies: Seq[TaxBands] = Seq(),
                                   totalNotionalTax: Option[BigDecimal] = None,
                                   incomeTaxDueAfterTaxReductions: Option[BigDecimal] = None,
                                   totalPensionSavingsTaxCharges: Option[BigDecimal] = None,
                                   statePensionLumpSumCharges: Option[BigDecimal] = None,
                                   payeUnderpaymentsCodedOut: Option[BigDecimal] = None,
                                   nic4Bands: Seq[Nic4Bands] = Seq(),
                                   class2NicsAmount: Option[BigDecimal] = None,
                                   totalCapitalGainsIncome: Option[BigDecimal] = None,
                                   adjustments: Option[BigDecimal] = None,
                                   foreignTaxCreditRelief: Option[BigDecimal] = None,
                                   taxOnGainsAlreadyPaid: Option[BigDecimal] = None,
                                   capitalGainsTaxDue: Option[BigDecimal] = None,
                                   capitalGainsOverpaid: Option[BigDecimal] = None,
                                   cgtTxBands: Seq[CgtTaxBands] = Seq(),
                                   taxableGains: Option[BigDecimal] = None,
                                   rate: Option[BigDecimal] = None,
                                   taxAmount: Option[BigDecimal] = None,
                                   totalStudentLoansRepaymentAmount: Option[BigDecimal] = None,
                                   saUnderpaymentsCodedOut: Option[BigDecimal] = None,
                                   totalIncomeTaxAndNicsDue: Option[BigDecimal] = None,
                                   totalTaxDeducted: Option[BigDecimal] = None,
                                   ukLandAndProperty: Option[BigDecimal] = None,
                                   bbsi: Option[BigDecimal] = None,
                                   cis: Option[BigDecimal] = None,
                                   voidedIsa: Option[BigDecimal] = None,
                                   payeEmployments: Option[BigDecimal] = None,
                                   occupationalPensions: Option[BigDecimal] = None,
                                   stateBenefits: Option[BigDecimal] = None,
                                   specialWithholdingTaxOrUkTaxPaid: Option[BigDecimal] = None,
                                   inYearAdjustmentCodedInLaterTaxYear: Option[BigDecimal] = None
                                 ) {

  def get(calcOpt: Option[Calculation]): TaxDueSummaryViewModel = {
    calcOpt match {
      case Some(calc) => TaxDueSummaryViewModel(
        taxRegime = None,
        class2VoluntaryContributions = None,
        messages = Messages(),
        lossesAppliedToGeneralIncome = None,
        grossGiftAidPayments = None,
        giftAidTax = None,
        marriageAllowanceTransferredInAmount = None,
        reliefsClaimed: Seq[ReliefsClaimed],
        totalResidentialFinanceCostsRelief = None,
        totalForeignTaxCreditRelief = None,
        topSlicingReliefAmount = None,
        totalTaxableIncome = None,
        payPensionsProfit = Seq(),
        savingsAndGains = Seq(),
        lumpSums = Seq(),
        dividends = Seq(),
        gainsOnLifePolicies = Seq(),
        totalNotionalTax = None,
        incomeTaxDueAfterTaxReductions = None,
        totalPensionSavingsTaxCharges = None,
        statePensionLumpSumCharges = None,
        payeUnderpaymentsCodedOut = None,
        nic4Bands = Seq(),
        class2NicsAmount = None,
        totalCapitalGainsIncome = None,
        adjustments = None,
        foreignTaxCreditRelief = None,
        taxOnGainsAlreadyPaid = None,
        capitalGainsTaxDue = None,
        capitalGainsOverpaid = None,
        cgtTxBands = Seq(),
        taxableGains = None,
        rate = None,
        taxAmount = None,
        totalStudentLoansRepaymentAmount = None,
        saUnderpaymentsCodedOut = None,
        totalIncomeTaxAndNicsDue = None,
        totalTaxDeducted = None,
        ukLandAndProperty = None,
        bbsi = None,
        cis = None,
        voidedIsa = None,
        payeEmployments = None,
        occupationalPensions = None,
        stateBenefits = None,
        specialWithholdingTaxOrUkTaxPaid = None,
        inYearAdjustmentCodedInLaterTaxYear = None
      )
      case None => TaxDueSummaryViewModel()
    }
  }




