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

import enums.ViewInYearTaxEstimate
import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import play.api.libs.json.{Json, OWrites}

case class ViewInYearTaxEstimateAuditModel(
                                            nino: String,
                                            mtditid: String,
                                            userType: String,
                                            taxYear: Int,
                                            body: ViewInYearTaxEstimateAuditBody
                                          ) extends AuditModel {
  
  override val transactionName: String = "ViewInYearTaxEstimate"
  override val auditType: String = ViewInYearTaxEstimate.name
  
  override val detail: Seq[(String, String)] = Seq(
    "nino" -> nino,
    "mtditid" -> mtditid,
    "userType" -> userType,
    "taxYear" -> taxYear.toString,
    "body" -> Json.toJson(body).toString()
  )
}

case class ViewInYearTaxEstimateAuditBody(
                                           income: Int,
                                           allowancesAndDeductions: BigDecimal,
                                           totalTaxableIncome: Int,
                                           incomeTaxAndNationalInsuranceContributions: BigDecimal
                                         )

object ViewInYearTaxEstimateAuditBody {
  implicit val writes: OWrites[ViewInYearTaxEstimateAuditBody] = Json.writes[ViewInYearTaxEstimateAuditBody]
  
  def apply(taxCalc: TaxYearSummaryViewModel): ViewInYearTaxEstimateAuditBody = {
    new ViewInYearTaxEstimateAuditBody(
      taxCalc.income,
      taxCalc.deductions,
      taxCalc.totalTaxableIncome,
      taxCalc.taxDue
    )
  }
  
}
