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

import auth.MtdItUser
import models.calculation.Calculation
import models.financialDetails.Charge
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModelWithIncomeType}
import play.api.Logger
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._
import audit.Utilities._

case class TaxYearOverviewResponseAuditModel(mtdItUser: MtdItUser[_],
                                             agentReferenceNumber: Option[String],
                                             calculation: Calculation,
                                             payments: List[Charge],
                                             updates: ObligationsModel) extends ExtendedAuditModel {

  override val transactionName: String = "tax-year-overview-response"
  override val auditType: String = "TaxYearOverviewResponse"

  private val calculationDetails: JsObject = Json.obj() ++
    ("income", calculation.totalIncomeReceived) ++
    ("allowancesAndDeductions", calculation.allowancesAndDeductions.totalAllowancesDeductionsReliefs) ++
    ("taxableIncome", calculation.totalTaxableIncome) ++
    ("taxDue", calculation.totalIncomeTaxAndNicsDue)

  private def getChargeType(chargeType: Option[String]): String = chargeType match {
    case Some("SA Payment on Account 1") => "Payment on account 1 of 2"
    case Some("SA Payment on Account 2") => "Payment on account 2 of 2"
    case Some("SA Balancing Charge") => "Remaining balance"
    case error => {
      Logger.error(s"[TaxYearOverview][getChargeType] Missing or non-matching charge type: $error found")
      "unknownCharge"
    }
  }

  private def paymentsJson(charge: Charge): JsObject = {
    Json.obj("paymentType" -> getChargeType(charge.mainType),
      "status" -> charge.getChargePaidStatus) ++
      ("amount", charge.originalAmount) ++
      ("dueDate", charge.due)
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

  private def updatesJson(updates: ReportDeadlineModelWithIncomeType): JsObject = {
    Json.obj("updateType" -> getUpdateType(updates.obligation.obligationType),
      "incomeSource" -> getObligationsType(updates.incomeType)) ++
      ("dateSubmitted", updates.obligation.dateReceived)
  }

  private val updatesDetail: Seq[JsObject] = updates.allDeadlinesWithSource(true)(mtdItUser).map(updatesJson)


  override val detail: JsValue = {
    Json.obj("nationalInsuranceNumber" -> mtdItUser.nino,
      "mtditid" -> mtdItUser.mtditid,
      "calculation" -> calculationDetails,
      "payments" -> paymentsDetails,
      "updates" -> updatesDetail) ++
      ("agentReferenceNumber", agentReferenceNumber) ++
      ("saUtr", mtdItUser.saUtr) ++
      ("credId", mtdItUser.credId) ++
      userType(mtdItUser.userType)
  }

}
