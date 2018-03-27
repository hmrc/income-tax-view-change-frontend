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

  //ReportDeadlines
  //===============
  val businessReportDeadlinesUrl: (String, String) => String = (nino, selfEmploymentId) => s"/ni/$nino/self-employments/$selfEmploymentId/obligations"
  val propertyReportDeadlinesUrl: String => String = nino => s"/ni/$nino/uk-properties/obligations"

  def stubGetBusinessReportDeadlines(nino: String, selfEmploymentId: String, business: ReportDeadlinesModel): Unit =
    WiremockHelper.stubGet(businessReportDeadlinesUrl(nino, selfEmploymentId), Status.OK, Json.toJson(business).toString())

  def stubGetPropertyReportDeadlines(nino: String, property: ReportDeadlinesModel): Unit =
    WiremockHelper.stubGet(propertyReportDeadlinesUrl(nino), Status.OK, Json.toJson(property).toString())

  def stubPropertyReportDeadlinesError(nino: String): Unit =
    WiremockHelper.stubGet(propertyReportDeadlinesUrl(nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  def stubBusinessReportDeadlinesError(nino: String, selfEmploymentId: String): Unit =
    WiremockHelper.stubGet(businessReportDeadlinesUrl(nino, selfEmploymentId), Status.INTERNAL_SERVER_ERROR, "ISE")

  def verifyGetBusinessReportDeadlines(nino: String, selfEmploymentId: String): Unit =
    WiremockHelper.verifyGetWithHeader(businessReportDeadlinesUrl(nino, selfEmploymentId), "Accept", "application/vnd.hmrc.1.0+json")

  def verifyGetPropertyReportDeadlines(nino: String): Unit =
    WiremockHelper.verifyGetWithHeader(propertyReportDeadlinesUrl(nino), "Accept", "application/vnd.hmrc.1.0+json")


  // Calculation Breakdown Stubs
  // ===========================
  val calcUrl: (String,String) => String = (nino, taxCalculationId) => s"/ni/$nino/calculations/$taxCalculationId"

  def stubGetCalcData(nino: String, year: String, calc: String): Unit = {
    WiremockHelper.stubGet(calcUrl(nino, year), Status.OK, calc)
  }

  def stubGetCalcError(nino: String, year: String, error: CalculationDataErrorModel): Unit = {
    WiremockHelper.stubGet(calcUrl(nino, year), Status.INTERNAL_SERVER_ERROR, Json.toJson(error).toString())
  }

  def verifyGetCalcData(nino: String, taxCalculationId: String): Unit =
    WiremockHelper.verifyGet(calcUrl(nino, taxCalculationId))
}

