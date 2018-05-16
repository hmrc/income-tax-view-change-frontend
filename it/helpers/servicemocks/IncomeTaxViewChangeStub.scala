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
import models._
import models.calculation.LastTaxCalculation
import models.core.{Nino, NinoResponseError}
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import models.reportDeadlines.ReportDeadlinesModel
import play.api.http.Status
import play.api.libs.json.Json

object IncomeTaxViewChangeStub {

  // Last Tax Calc Stubs
  // ===================
  val lastCalcUrl: (String,String) => String = (nino, year) =>
    s"/income-tax-view-change/estimated-tax-liability/$nino/$year/it"

  def stubGetLastTaxCalc(nino: String, year: String, lastCalc: LastTaxCalculation): Unit = {
    WiremockHelper.stubGet(lastCalcUrl(nino, year), Status.OK, Json.toJson(lastCalc).toString())
  }

  def stubGetLastCalcNoData(nino: String, year: String): Unit = {
    WiremockHelper.stubGet(lastCalcUrl(nino, year), Status.NOT_FOUND, "")
  }

  def stubGetLastCalcError(nino: String, year: String): Unit = {
    WiremockHelper.stubGet(lastCalcUrl(nino, year), Status.INTERNAL_SERVER_ERROR, "Error Message")
  }

  def verifyGetLastTaxCalc(nino: String, year: String): Unit =
    WiremockHelper.verifyGet(lastCalcUrl(nino, year))



  // NINO Lookup Stubs
  // =================
  val ninoLookupUrl: String => String = mtditid => s"/income-tax-view-change/nino-lookup/$mtditid"

  def stubGetNinoResponse(mtditid: String, nino: Nino): Unit =
    WiremockHelper.stubGet(ninoLookupUrl(mtditid), Status.OK, Json.toJson(nino).toString)

  def stubGetNinoError(mtditid: String, error: NinoResponseError): Unit =
    WiremockHelper.stubGet(ninoLookupUrl(mtditid), Status.INTERNAL_SERVER_ERROR, Json.toJson(error).toString)

  def verifyGetNino(mtditid: String): Unit =
    WiremockHelper.verifyGet(ninoLookupUrl(mtditid))



  // Income Source Details Stubs
  // ===========================
  val incomeSourceDetailsUrl: String => String = mtditid => s"/income-tax-view-change/income-sources/$mtditid"

  def stubGetIncomeSourceDetailsResponse(mtditid: String)(status: Int, response: IncomeSourceDetailsResponse): Unit =
    WiremockHelper.stubGet(incomeSourceDetailsUrl(mtditid), status, response.toJson.toString)

  def verifyGetIncomeSourceDetails(mtditid: String): Unit =
    WiremockHelper.verifyGet(incomeSourceDetailsUrl(mtditid))



  //ReportDeadlines Stubs
  //=====================
  def reportDeadlinesUrl(incomeSourceId: String, nino: String): String = s"/income-tax-view-change/$nino/income-source/$incomeSourceId/report-deadlines"

  def stubGetReportDeadlines(incomeSourceId: String, nino: String , deadlines: ReportDeadlinesModel): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(incomeSourceId, nino), Status.OK, Json.toJson(deadlines).toString())

  def stubGetReportDeadlinesError(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(incomeSourceId, nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  def verifyGetReportDeadlines(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.verifyGet(reportDeadlinesUrl(incomeSourceId, nino))

}
