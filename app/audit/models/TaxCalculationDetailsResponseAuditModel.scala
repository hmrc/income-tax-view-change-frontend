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
import models.calculation.TaxDeductedAtSource.Messages
import models.calculation.{Calculation, NicBand, ReductionsAndCharges, ReliefsClaimed, TaxBand}
import play.api.libs.json._
import utils.Utilities._


case class TaxCalculationDetailsResponseAuditModel(mtdItUser: MtdItUser[_],
                                              calculation: Calculation) extends ExtendedAuditModel {

  override val transactionName: String = "tax-calculation-response"
  override val auditType: String = "TaxCalculationDetailsResponse"

  private def scottish: Boolean = calculation.nationalRegime.contains("Scotland")

  private def taxBandNameToString(taxBandName: String): String =
    taxBandName match {
      case "ZRT" => "Zero rate"
      case "SSR" => "Starting rate"
      case "SRT" => "Starter rate"
      case "BRT" => "Basic rate"
      case "IRT" => "Intermediate rate"
      case "HRT" => "Higher rate"
      case "ART" => if(scottish) {"Top rate"} else {"Additional rate"}
      case "ZRTBR" => "Basic rate band at nil rate"
      case "ZRTHR" => "Higher rate band at nil rate"
      case "ZRTAR" => "Additional rate band at nil rate"
      case _ => taxBandName
    }

  private def rateBandToMessage(taxBand: TaxBand): String =
    taxBandNameToString(taxBand.name) + " (£" + taxBand.bandLimit + " at " + taxBand.rate + "%)"

  private def rateBandToMessage(nicBand: NicBand): String =
    taxBandNameToString(nicBand.name) + " (£" + nicBand.income + " at " + nicBand.rate + "%)"

  private def taxBandWithRateMessageJson(taxBand: TaxBand): JsObject = Json.obj(
    "rateBand" -> rateBandToMessage(taxBand),
    "amount" -> taxBand.taxAmount,
    "rateMessage" -> (taxBand.rate + "%")
  )

  private def taxBandWithoutRateMessageJson(taxBand: TaxBand): JsObject = Json.obj(
    "rateBand" -> rateBandToMessage(taxBand),
    "amount" -> taxBand.taxAmount
  )

  private def taxBandWithoutRateMessageJson(nicBand: NicBand): JsObject = Json.obj(
    "rateBand" -> rateBandToMessage(nicBand),
    "amount" -> nicBand.amount
  )

  private def taxReductionsJson(taxReductions: ReliefsClaimed): JsObject = Json.obj(
    "reductionDescription" -> taxReductions.`type`) ++
    ("amount", taxReductions.amountUsed)

  private val taxReductionsExtrasJson: Seq[JsObject] = Seq(
    if(calculation.reductionsAndCharges.marriageAllowanceTransferredInAmount.isDefined) {
      Json.obj("reductionDescription" -> "Marriage allowance transfer") ++
        ("amount", calculation.reductionsAndCharges.marriageAllowanceTransferredInAmount)
    } else Json.obj(),
    if(calculation.reductionsAndCharges.topSlicingRelief.isDefined) {
      Json.obj("reductionDescription" -> "Top slicing relief") ++
        ("amount", calculation.reductionsAndCharges.topSlicingRelief)
    } else Json.obj()
  )




//  private val calculationMessagesDetail: Seq[JsObject] = calculation.messages

  private val payPensionsProfitDetail: Seq[JsObject] = calculation.payPensionsProfit.bands.map(taxBandWithRateMessageJson)

  private val savingsDetail: Seq[JsObject] = calculation.savingsAndGains.bands.map(taxBandWithoutRateMessageJson)

  private val dividendsDetail: Seq[JsObject] = calculation.dividends.bands.map(taxBandWithoutRateMessageJson)

  private val employmentLumpSumsDetail: Seq[JsObject] = calculation.lumpSums.bands.map(taxBandWithoutRateMessageJson)

  private val gainsOnLifePoliciesDetail: Seq[JsObject] = calculation.gainsOnLifePolicies.bands.map(taxBandWithoutRateMessageJson)

  private val class4NationInsuranceDetail: Seq[JsObject] = calculation.nic.class4Bands.getOrElse(Seq()).map(taxBandWithoutRateMessageJson) //To Fix

  private val taxReductionsDetails: Seq[JsObject] =
    calculation.reductionsAndCharges.reliefsClaimed.getOrElse(Seq()).map(taxReductionsJson) ++ taxReductionsExtrasJson

  override val detail: JsValue =
    userAuditDetails(mtdItUser) ++
      Json.obj("payPensionsProfit" -> payPensionsProfitDetail,
      "savings" -> savingsDetail,
      "dividends" -> dividendsDetail,
      "employmentLumpSums" -> employmentLumpSumsDetail,
      "gainsOnLifePolicies" -> gainsOnLifePoliciesDetail,
      "class4NationalInsurance" -> class4NationInsuranceDetail,
      "taxReductions" -> taxReductionsDetails) ++
      ("calculationOnTaxableIncome", calculation.totalTaxableIncome)


}
