/*
 * Copyright 2017 HM Revenue & Customs
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

package helpers.servicemocks

import helpers.WiremockHelper
import models.calculation.CalculationDataErrorModel
import models.reportDeadlines.ReportDeadlinesModel
import play.api.http.Status
import play.api.libs.json.Json

object SelfAssessmentStub {

  // Calculation Breakdown Stubs
  // ===========================
  val calcUrl: (String,String) => String = (nino, taxCalculationId) => s"/ni/$nino/calculations/$taxCalculationId"

  def stubGetCalcData(nino: String, year: String, calc: String): Unit = {
    WiremockHelper.stubGet(calcUrl(nino, year), Status.OK, calc)
  }

  def stubGetCalcDataError(nino: String, year: String, error: CalculationDataErrorModel): Unit = {
    WiremockHelper.stubGet(calcUrl(nino, year), Status.INTERNAL_SERVER_ERROR, Json.toJson(error).toString())
  }

  def verifyGetCalcData(nino: String, taxCalculationId: String): Unit =
    WiremockHelper.verifyGet(calcUrl(nino, taxCalculationId))
}

