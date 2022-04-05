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

import models.liabilitycalculation.viewmodels.TaxYearSummaryViewModel
import play.api.libs.json.{JsObject, Json}
import testUtils.UnitSpec

class ViewInYearTaxEstimateAuditModelSpec extends UnitSpec {

  val taxDue = 1000.00
  val income = 500
  val deductions = 800.00
  val totalTaxable = 300
  
  val taxYear = 2022
  
  val nino = "AA000000A"
  val mtditid = "1234567890"
  val individual = "individual"
  
  val taxCalc: TaxYearSummaryViewModel = TaxYearSummaryViewModel(None, None, false, taxDue, income, deductions, totalTaxable)
  
  val viewInYearBodyNormal: ViewInYearTaxEstimateAuditBody = ViewInYearTaxEstimateAuditBody(income, deductions, totalTaxable, taxDue)
  val viewInYearBodyApplyModel: ViewInYearTaxEstimateAuditBody = ViewInYearTaxEstimateAuditBody(taxCalc)

  val expectedInYearBodyJson: JsObject = Json.obj(
    "income" -> income,
    "allowancesAndDeductions" -> deductions,
    "totalTaxableIncome" -> totalTaxable,
    "incomeTaxAndNationalInsuranceContributions" -> taxDue
  )
  
  val expectedKey = "ViewInYearTaxEstimate"
  
  val viewInYearModel: ViewInYearTaxEstimateAuditModel = ViewInYearTaxEstimateAuditModel(
    nino, mtditid, individual, taxYear, viewInYearBodyNormal
  )
  
  "ViewInYearBody" when {
    
    ".apply is called" should {

      "create the model the same as the normal apply method" in {
        viewInYearBodyNormal shouldBe viewInYearBodyApplyModel
      }
      
    }
    
    "the writes are called" should {
      
      "correctly write to Json" in {
        Json.toJson(viewInYearBodyNormal) shouldBe expectedInYearBodyJson
      }
      
    }
    
  }
  
  "ViewInYearTaxEstimateAuditModel" when {
    
    "transactionName is called" should {
      
      s"return a value of ${expectedKey}" in {
        viewInYearModel.transactionName shouldBe expectedKey
      }
      
    }
    
    "auditType is called" should {
      
      s"return a value of ${expectedKey}" in {
        viewInYearModel.auditType shouldBe expectedKey
      }
      
    }
    
    "details is called" should {
      
      "create a sequence that can be sent for audit" in {
        val expectedSeq: Seq[(String, String)] = Seq(
          "nino" -> nino,
          "mtditid" -> mtditid,
          "userType" -> individual,
          "taxYear" -> s"$taxYear",
          "body" -> expectedInYearBodyJson.toString()
        )
        
        viewInYearModel.detail shouldBe expectedSeq
      }
      
    }
    
  }
  
}
