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

package audit.models

import audit.Utilities.{getChargeType, userAuditDetails}
import auth.MtdItUser
import implicits.ImplicitDateParser
import models.financialDetails.DocumentDetailWithDueDate
import models.liabilitycalculation.viewmodels.TaxYearOverviewViewModel
import models.nextUpdates.{NextUpdateModelWithIncomeType, ObligationsModel}
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._

case class TaxYearOverviewResponseAuditModel(mtdItUser: MtdItUser[_],
                                             payments: List[DocumentDetailWithDueDate],
                                             updates: ObligationsModel,
                                             taxYearOverviewViewModel: Option[TaxYearOverviewViewModel] = None
                                            ) extends ExtendedAuditModel with ImplicitDateParser {

  override val transactionName: String = "tax-year-overview-response"
  override val auditType: String = "TaxYearOverviewResponse"

  private val taxYearOverviewJson = Json.obj() ++
    ("calculationDate", taxYearOverviewViewModel.map(_.timestamp.toZonedDateTime.toLocalDate)) ++
    ("totalDue", taxYearOverviewViewModel.map(_.taxDue))

  private val calculationDetails: JsObject = Json.obj() ++
    ("income", taxYearOverviewViewModel.map(_.income)) ++
    ("allowancesAndDeductions", taxYearOverviewViewModel.map(_.deductions)) ++
    ("taxableIncome", taxYearOverviewViewModel.map(_.totalTaxableIncome)) ++
    ("taxDue", taxYearOverviewViewModel.map(_.taxDue))

  private def paymentsJson(docDateDetail: DocumentDetailWithDueDate): JsObject = {
    Json.obj("paymentType" -> getChargeType(docDateDetail.documentDetail, latePaymentCharge = false),
      "underReview" -> docDateDetail.dunningLock,
      "status" -> docDateDetail.documentDetail.getChargePaidStatus) ++
      ("amount", docDateDetail.documentDetail.originalAmount) ++
      ("dueDate", docDateDetail.dueDate)
  }

  private def paymentsJsonLPI(docDateDetail: DocumentDetailWithDueDate): JsObject = {
    Json.obj("paymentType" -> getChargeType(docDateDetail.documentDetail, latePaymentCharge = true),
      "underReview" -> docDateDetail.dunningLock,
      "status" -> docDateDetail.documentDetail.getInterestPaidStatus) ++
      ("amount", docDateDetail.documentDetail.latePaymentInterestAmount) ++
      ("dueDate", docDateDetail.dueDate)
  }

  private val paymentsDetails: Seq[JsObject] = payments.map(paymentsJson) ++
    payments.filter(_.documentDetail.latePaymentInterestAmount.isDefined).map(paymentsJsonLPI)

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
