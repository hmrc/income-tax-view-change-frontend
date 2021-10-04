/*
 * Copyright 2021 HM Revenue & Customs
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

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import models.calculation._
import play.api.libs.json._
import utils.Utilities._


case class TaxCalculationDetailsResponseAuditModel(mtdItUser: MtdItUser[_],
                                                   calcDisplayModel: CalcDisplayModel,
                                                   taxYear: Int) extends ExtendedAuditModel {

  import implicits.ImplicitCurrencyFormatter._

  override val transactionName: String = "tax-calculation-response"
  override val auditType: String = "TaxCalculationDetailsResponse"

  private val calculation: Calculation = calcDisplayModel.calcDataModel

  private def calcMessageCodeToString(id: String): Option[String] =
    Some(id) collect {
      case "C22201" => s"Your Basic Rate limit has been increased by ${calculation.reductionsAndCharges.grossGiftAidPayments.get.toCurrencyString} to ${calcDisplayModel.getModifiedBaseTaxBand.get.apportionedBandLimit.toCurrencyString} for Gift Aid payments"
      case "C22202" => "Tax due on gift aid payments exceeds your income tax charged so you are liable for gift aid tax"
      case "C22203" => "Class 2 National Insurance has not been charged because your self-employed profits are under the small profit threshold"
      case "C22205" => s"Total loss from all income sources was capped at ${calculation.allowancesAndDeductions.lossesAppliedToGeneralIncome.get.toCurrencyString}"
      case "C22206" => "One or more of your annual adjustments have not been applied because you have submitted additional income or expenses"
      case "C22207" => "Your payroll giving amount has been included in your adjusted taxable income"
      case "C22208" => s"Your Basic Rate limit has been increased by ${calculation.reductionsAndCharges.giftAidTax.get.toCurrencyString} to ${calcDisplayModel.getModifiedBaseTaxBand.get.apportionedBandLimit.toCurrencyString} for Pension Contribution"
      case "C22209" => s"Your Basic Rate limit has been increased by ${calculation.reductionsAndCharges.giftAidTax.get.toCurrencyString} to ${calcDisplayModel.getModifiedBaseTaxBand.get.apportionedBandLimit.toCurrencyString} for Pension Contribution and Gift Aid payments"
      case "C22210" => "Employment related expenses are capped at the total amount of employment income"
      case "C22211" => "This is a forecast of your annual income tax liability based on the information you have provided to date. Any overpayments of income tax will not be refundable until after you have submitted your final declaration"
      case "C22212" => "Employment and Deduction related expenses have been limited to employment income."
      case "C22213" => "Due to your employed earnings, paying Class 2 Voluntary may not be beneficial."
      case "C22214" => "Your Class 4 has been adjusted for Class 2 due and primary Class 1 contributions."
      case "C22215" => "Due to the level of your current income, you may not be eligible for Marriage Allowance and therefore it has not been included in this calculation."
      case "C22216" => "Due to the level of your income, you are no longer eligible for Marriage Allowance and your claim will be cancelled."
      case "C22217" => "There are one or more underpayments, debts or adjustments that have not been included in the calculation as they do not relate to data that HMRC holds."
      case "C22218" => "The Capital gains tax has been included in the estimated annual liability calculation only, the actual amount of capital gains tax will be in the final declaration calculation."
    }

  private val allowedCalcMessages: Seq[String] = {
    calculation.messages.map(_.allMessages).getOrElse(Seq.empty).map(_.id).flatMap(calcMessageCodeToString)
  }

  private def calcMessagesJson(message: String): JsObject = {
    Json.obj("calculationMessage" -> message)
  }

  private def scottish: Boolean = calculation.nationalRegime.contains("Scotland")

  private def taxBandNameToString(taxBandName: String): String =
    taxBandName match {
      case "ZRT" => "Zero rate"
      case "SSR" => "Starting rate"
      case "SRT" => "Starter rate"
      case "BRT" => "Basic rate"
      case "IRT" => "Intermediate rate"
      case "HRT" => "Higher rate"
      case "ART" => if (scottish) "Top rate" else "Additional rate"
      case "ZRTBR" => "Basic rate band at nil rate"
      case "ZRTHR" => "Higher rate band at nil rate"
      case "ZRTAR" => "Additional rate band at nil rate"
      case _ => taxBandName
    }

  private def rateBandToMessage(taxBand: TaxBand): String =
    s"${taxBandNameToString(taxBand.name)} (£${taxBand.income} at ${taxBand.rate}%)"

  private def rateBandToMessage(nicBand: NicBand): String =
    s"${taxBandNameToString(nicBand.name)} (£${nicBand.income} at ${nicBand.rate}%)"

  private def taxBandRateMessageJson(taxBand: TaxBand): JsObject = Json.obj(
    "rateBand" -> rateBandToMessage(taxBand),
    "amount" -> taxBand.taxAmount
  )

  private def nicBandRateMessageJson(nicBand: NicBand): JsObject = Json.obj(
    "rateBand" -> rateBandToMessage(nicBand),
    "amount" -> nicBand.amount
  )

  private def reductionTypeToString(reductionType: String): Option[String] =
    Some(reductionType) collect {
      case "vctSubscriptions" => "Venture Capital Trust relief"
      case "deficiencyRelief" => "Deficiency Relief"
      case "eisSubscriptions" => "Enterprise Investment Scheme relief"
      case "seedEnterpriseInvestment" => "Seed Enterprise Investment Scheme relief"
      case "communityInvestment" => "Community Investment Tax Relief"
      case "socialEnterpriseInvestment" => "Social Enterprise Investment Tax Relief"
      case "maintenancePayments" => "Maintenance and alimony paid"
      case "qualifyingDistributionRedemptionOfSharesAndSecurities" => "Relief claimed on a qualifying distribution"
      case "nonDeductibleLoanInterest" => "Non deductible loan interest"
      case "marriageAllowanceTransfer" => "Marriage allowance transfer"
      case "topSlicingRelief" => "Top slicing relief"
      case "reliefForFinanceCosts" => "Relief for finance costs"
      case "notionalTax" => "Notional tax from gains on life policies etc."
      case "foreignTaxCreditRelief" => "Foreign Tax Credit Relief"
      case "incomeTaxDueAfterTaxReductions" => "Income Tax due after tax reductions"
    }


  private def taxReductionsJson(taxReductions: ReliefsClaimed): JsObject = Json.obj() ++
    ("reductionDescription", reductionTypeToString(taxReductions.`type`)) ++
    ("amount", taxReductions.amountUsed)

  private def optReliefClaim(description: String, amount: Option[BigDecimal]): Option[ReliefsClaimed] =
    if(amount.isDefined){
      Some(ReliefsClaimed(description, amount))
    } else None

  private val taxReductionsExtrasSeq: Seq[ReliefsClaimed] = Seq() ++
    optReliefClaim("marriageAllowanceTransfer", calculation.reductionsAndCharges.marriageAllowanceTransferredInAmount) ++
    optReliefClaim("topSlicingRelief", calculation.reductionsAndCharges.topSlicingRelief) ++
    optReliefClaim("reliefForFinanceCosts", calculation.reductionsAndCharges.totalResidentialFinanceCostsRelief) ++
    optReliefClaim("notionalTax", calculation.reductionsAndCharges.totalNotionalTax) ++
    optReliefClaim("foreignTaxCreditRelief", calculation.reductionsAndCharges.totalForeignTaxCreditRelief) ++
    optReliefClaim("incomeTaxDueAfterTaxReductions", calculation.reductionsAndCharges.incomeTaxDueAfterTaxReductions)


  private val taxReductionsFullSeq: Seq[ReliefsClaimed] =
    calculation.reductionsAndCharges.reliefsClaimed.getOrElse(Seq()) ++ taxReductionsExtrasSeq

  private case class TaxCalcLineItem(description: String, amount: BigDecimal)

  private def optLineItem(description: String, amount: Option[BigDecimal]): Option[TaxCalcLineItem] = {
    amount.map(TaxCalcLineItem(description, _))
  }

  private def doubleOptLineItem(description: Option[String], amount: Option[BigDecimal]): Option[TaxCalcLineItem] = {
    for { desc <- description; amt <- amount } yield TaxCalcLineItem(desc, amt)
  }

  private val additionalChargesSeq: Seq[TaxCalcLineItem] = Seq() ++
    doubleOptLineItem(class2NicVoluntary(calculation.nic.class2VoluntaryContributions), calculation.nic.class2) ++
    optLineItem("Gift Aid tax charge", calculation.reductionsAndCharges.giftAidTax) ++
    optLineItem("Total pension saving charges", calculation.reductionsAndCharges.totalPensionSavingsTaxCharges) ++
    optLineItem("State pension lump sum", calculation.reductionsAndCharges.statePensionLumpSumCharges)

  private val otherChargesSeq: Seq[TaxCalcLineItem] = Seq() ++
    optLineItem("Student Loan Repayments", calculation.reductionsAndCharges.totalStudentLoansRepaymentAmount) ++
    optLineItem(s"Underpaid tax for earlier years in your tax code for ${taxYear - 1} to $taxYear", calculation.reductionsAndCharges.payeUnderpaymentsCodedOut) ++
    optLineItem(s"Underpaid tax for earlier years in your self assessment for ${taxYear - 1} to $taxYear", calculation.reductionsAndCharges.saUnderpaymentsCodedOut)

  private def lineItemJson(lineItem: TaxCalcLineItem): JsObject = Json.obj(
    "chargeType" -> lineItem.description,
    "amount" -> lineItem.amount
  )

  private def class2NicVoluntary(voluntary: Option[Boolean]): Option[String] = {
    voluntary map {
      case true => "Voluntary Class 2 National Insurance"
      case false => "Class 2 National Insurance"
    }
  }

  private def businessAssetsTaxBandJson(cgtTaxBand: SingleBandCgtDetail): Option[Seq[JsObject]] = {
    for { amt <- cgtTaxBand.taxAmount; taxableGains <- cgtTaxBand.taxableGains; rate <- cgtTaxBand.rate } yield
      Seq(Json.obj(
        "rateBand" -> s"Business Asset Disposal Relief and or Investors' Relief gains (£$taxableGains at $rate %)",
        "amount" -> amt
      ))
  }

  private def taxBandRateString(cgtTaxBand: CgtTaxBand): String = {
    val taxBandName: String = cgtTaxBand.name match {
      case "lowerRate" => "basic rate"
      case "higherRate" => "higher rate"
      case _ => cgtTaxBand.name
    }
    s"$taxBandName (£${cgtTaxBand.income} at ${cgtTaxBand.rate}%)"
  }

  private def propertyInterestTaxBandJson(cgtTaxBand: CgtTaxBand): JsObject = Json.obj(
    "rateBand" -> s"Residential property and carried interest ${taxBandRateString(cgtTaxBand)}",
    "amount" -> cgtTaxBand.taxAmount
  )

  private def otherGainsTaxBandString(cgtTaxBand: CgtTaxBand): JsObject = Json.obj(
    "rateBand" -> s"Other gains ${taxBandRateString(cgtTaxBand)}",
    "amount" -> cgtTaxBand.taxAmount
  )

  private def capitalGainsTaxExtrasJson(cgt: CapitalGainsTax): JsObject = Json.obj() ++
    ("taxableCapitalGains", cgt.totalTaxableGains) ++
      ("capitalGainsTaxAdjustment", cgt.adjustments) ++
      ("foreignTaxCreditReliefOnCapitalGains", cgt.foreignTaxCreditRelief) ++
      ("taxOnGainsAlreadyPaid", cgt.taxOnGainsAlreadyPaid) ++
      ("capitalGainsTaxDue", cgt.capitalGainsTaxDue) ++
      ("capitalGainsTaxCalculatedAsOverpaid", cgt.capitalGainsOverpaid)

  private def capitalGainsTaxRatesJson(cgt: CapitalGainsTax): Option[Seq[JsObject]] = {
    val businessAssets: Option[Seq[JsObject]] = businessAssetsTaxBandJson(cgt.businessAssetsDisposalsAndInvestorsRel)
    val propertyAndInterest: Seq[JsObject] = cgt.propertyAndInterestTaxBands.map(propertyInterestTaxBandJson)
    val otherGains: Seq[JsObject] = cgt.otherGainsTaxBands.map(otherGainsTaxBandString)
    val allJson: Seq[JsObject] = Seq(businessAssets.getOrElse(Seq()), propertyAndInterest, otherGains).flatten

    Some(allJson).filter(_.nonEmpty)
  }

  private def capitalGainsTaxJson(cgt: CapitalGainsTax): JsObject =
    capitalGainsTaxExtrasJson(cgt) ++
      ("rates", capitalGainsTaxRatesJson(cgt))


  private def deductionsString(deduction: String): String =
    deduction match {
      case "inYearAdjustment" => "Outstanding debt collected through PAYE"
      case "payeEmployments" => "All employments"
      case "ukPensions" => "UK pensions"
      case "stateBenefits" => "State benefits"
      case "cis" => "CIS and trading income"
      case "ukLandAndProperty" => "UK land and property"
      case "specialWithholdingTax" => "Special withholding tax"
      case "voidISAs" => "Void ISAs"
      case "savings" => "Interest received from UK banks and building societies"
      case _ => deduction
    }

  private def taxDeductionsJson(taxDeductions: (String, BigDecimal)): JsObject = Json.obj(
    "deductionType" -> deductionsString(taxDeductions._1),
    "amount" -> taxDeductions._2
  )

  private val taxDeductionsFullSeq: Seq[(String, BigDecimal)] = {
    val totalDeductions = calculation.taxDeductedAtSource.total
    totalDeductions.fold(calculation.taxDeductedAtSource.allFields)(total => calculation.taxDeductedAtSource.allFields :+ ("Income Tax due after deductions", total))
  }

  private def optDetail(optDetail: Seq[JsObject]): Option[Seq[JsObject]] =
    if(optDetail.nonEmpty) Some(optDetail) else None

  private def optDetail(optDetail: JsObject): Option[JsObject] = {
    if(optDetail == Json.obj()) None else Some(optDetail)
  }


  private val calculationMessagesDetail: Option[Seq[JsObject]] = optDetail(allowedCalcMessages.map(calcMessagesJson))

  private val payPensionsProfitDetail: Option[Seq[JsObject]] = optDetail(calculation.payPensionsProfit.bands.filter(_.income > 0).map(taxBandRateMessageJson))

  private val savingsDetail: Option[Seq[JsObject]] = optDetail(calculation.savingsAndGains.bands.filter(_.income > 0).map(taxBandRateMessageJson))

  private val dividendsDetail: Option[Seq[JsObject]] = optDetail(calculation.dividends.bands.filter(_.income > 0).map(taxBandRateMessageJson))

  private val employmentLumpSumsDetail: Option[Seq[JsObject]] = optDetail(calculation.lumpSums.bands.filter(_.income > 0).map(taxBandRateMessageJson))

  private val gainsOnLifePoliciesDetail: Option[Seq[JsObject]] = optDetail(calculation.gainsOnLifePolicies.bands.filter(_.income > 0).map(taxBandRateMessageJson))

  private val class4NationInsuranceDetail: Option[Seq[JsObject]] = optDetail(calculation.nic.class4Bands.getOrElse(Seq.empty).filter(_.income > 0).map(nicBandRateMessageJson))

  private val taxReductionsDetails: Option[Seq[JsObject]] = optDetail(taxReductionsFullSeq.map(taxReductionsJson))

  private val additionChargesDetail: Option[Seq[JsObject]] = optDetail(additionalChargesSeq.map(lineItemJson))

  private val capitalGainsTaxDetail: Option[JsObject] = optDetail(capitalGainsTaxJson(calculation.capitalGainsTax))

  private val otherChargesDetail: Option[Seq[JsObject]] = optDetail(otherChargesSeq.map(lineItemJson))

  private val taxDeductionsDetail: Option[Seq[JsObject]] = optDetail(taxDeductionsFullSeq.map(taxDeductionsJson))

  override val detail: JsValue =
    userAuditDetails(mtdItUser) ++
      ("calculationOnTaxableIncome", calculation.totalTaxableIncome) ++
      ("incomeTaxAndNationalInsuranceContributionsDue", calculation.totalIncomeTaxAndNicsDue) ++
      ("taxCalculationMessage", calculationMessagesDetail) ++
      ("payPensionsProfit", payPensionsProfitDetail) ++
      ("savings", savingsDetail) ++
      ("dividends", dividendsDetail) ++
      ("employmentLumpSums", employmentLumpSumsDetail) ++
      ("gainsOnLifePolicies", gainsOnLifePoliciesDetail) ++
      ("class4NationalInsurance", class4NationInsuranceDetail) ++
      ("capitalGainsTax", capitalGainsTaxDetail) ++
      ("taxReductions", taxReductionsDetails) ++
      ("additionalCharges", additionChargesDetail) ++
      ("otherCharges", otherChargesDetail) ++
      ("taxDeductions", taxDeductionsDetail)

}
