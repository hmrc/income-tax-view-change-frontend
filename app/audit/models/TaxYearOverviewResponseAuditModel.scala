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

import audit.Utilities._
import auth.MtdItUser
import implicits.ImplicitDateParser
import models.calculation.Calculation
import models.financialDetails.DocumentDetailWithDueDate
import models.nextUpdates.{ObligationsModel, NextUpdateModelWithIncomeType}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._

case class TaxYearOverviewResponseAuditModel(mtdItUser: MtdItUser[_],
                                             calculation: Calculation,
                                             payments: List[DocumentDetailWithDueDate],
                                             updates: ObligationsModel) extends ExtendedAuditModel with ImplicitDateParser {

  override val transactionName: String = "tax-year-overview-response"
  override val auditType: String = "TaxYearOverviewResponse"

  private val taxYearOverviewJson = Json.obj() ++
    ("calculationDate", calculation.timestamp.map(_.toZonedDateTime.toLocalDate)) ++
    ("totalDue", calculation.totalIncomeTaxAndNicsDue)

  private val calculationDetails: JsObject = Json.obj() ++
    ("income", calculation.totalIncomeReceived) ++
    ("allowancesAndDeductions", calculation.allowancesAndDeductions.totalAllowancesDeductionsReliefs) ++
    ("taxableIncome", calculation.totalTaxableIncome) ++
    ("taxDue", calculation.totalIncomeTaxAndNicsDue)

  private def getChargeType(chargeType: Option[String]): String = chargeType match {
    case Some("ITSA- POA 1") => "Payment on account 1 of 2"
    case Some("ITSA - POA 2") => "Payment on account 2 of 2"
    case Some("TRM New Charge") | Some("TRM Amend Charge") => "Remaining balance"
    case error => {
      Logger.error(s"[TaxYearOverview][getChargeType] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  private def paymentsJson(docDateDetail: DocumentDetailWithDueDate): JsObject = {
    Json.obj("paymentType" -> getChargeType(docDateDetail.documentDetail.documentDescription),
      "underReview" -> docDateDetail.dunningLock,
      "status" -> docDateDetail.documentDetail.getChargePaidStatus) ++
      ("amount", docDateDetail.documentDetail.originalAmount) ++
      ("dueDate", docDateDetail.dueDate)
  }

  private val paymentsDetails: Seq[JsObject] = payments.map(paymentsJson)

  private def getObligationsType(obligationType: String) = {
    obligationType match {
      case "Property" => "Property income"
      case "Business" => "Business"
      case "Crystallised" => "All income sources"
      case _ => obligationType
    }
  }

  private def getUpdateType(updateType: String) = {
    updateType match {
      case "Quarterly" => "Quarterly Update"
      case "EOPS" => "Annual Update"
      case "Crystallised" => "Final Declaration"
      case _ => updateType
    }
  }

  private def updatesJson(updates: NextUpdateModelWithIncomeType): JsObject = {
    Json.obj("updateType" -> getUpdateType(updates.obligation.obligationType),
      "incomeSource" -> getObligationsType(updates.incomeType)) ++
      ("dateSubmitted", updates.obligation.dateReceived)
  }

  private val updatesDetail: Seq[JsObject] = updates.allDeadlinesWithSource(true)(mtdItUser).map(updatesJson)


  override val detail: JsValue = {
    userAuditDetails(mtdItUser) ++
    Json.obj("taxYearOverview" -> taxYearOverviewJson,
      "calculation" -> calculationDetails,
      "payments" -> paymentsDetails,
      "updates" -> updatesDetail)
  }

}
