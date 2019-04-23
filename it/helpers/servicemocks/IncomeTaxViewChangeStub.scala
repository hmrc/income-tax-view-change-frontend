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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WiremockHelper
import models.calculation.CalculationModel
import models.core.{Nino, NinoResponseError}
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import models.reportDeadlines.ReportDeadlinesModel
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}

object IncomeTaxViewChangeStub {

  // Get Latest Calculation Stubs
  // ===================================
  val latestCalculationUrl: (String, String) => String = (nino, year) =>
    s"/income-tax-view-change/previous-tax-calculation/$nino/$year"

  def stubGetLatestCalculation(nino: String, year: String, latestCalc: JsValue): Unit = {
    WiremockHelper.stubGet(latestCalculationUrl(nino, year), Status.OK, latestCalc.toString)
  }

  def stubGetLatestCalcError(nino: String, year: String): Unit = {
    WiremockHelper.stubGet(latestCalculationUrl(nino, year), Status.INTERNAL_SERVER_ERROR, "Error Message")
  }

  def stubGetLatestCalcNotFound(nino: String, year: String): Unit = {
    WiremockHelper.stubGet(latestCalculationUrl(nino, year), Status.NOT_FOUND, "Not found")
  }

  def verifyGetLatestCalculation(nino: String, year: String): Unit =
    WiremockHelper.verifyGet(latestCalculationUrl(nino, year))



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


  //PreviousObligations Stubs
  def previousObligationsUrl(incomeSourceId: String, nino: String): String = {
    s"/income-tax-view-change/$nino/income-source/$incomeSourceId/fulfilled-report-deadlines"
  }

  def stubGetPreviousObligations(incomeSourceId: String, nino: String, deadlines: ReportDeadlinesModel): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(incomeSourceId, nino), Status.OK, Json.toJson(deadlines).toString())

  def stubGetPreviousObligationsNotFound(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(incomeSourceId, nino), Status.NOT_FOUND, "")

  def verifyGetPreviousObligations(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.verifyGet(previousObligationsUrl(incomeSourceId, nino))


  //ReportDeadlines Stubs
  //=====================
  def reportDeadlinesUrl(incomeSourceId: String, nino: String): String = s"/income-tax-view-change/$nino/income-source/$incomeSourceId/report-deadlines"

  def stubGetReportDeadlines(incomeSourceId: String, nino: String , deadlines: ReportDeadlinesModel): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(incomeSourceId, nino), Status.OK, Json.toJson(deadlines).toString())

  def stubGetReportDeadlinesError(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(incomeSourceId, nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  def stubGetReportDeadlinesNotFound(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(incomeSourceId, nino), Status.NO_CONTENT, "")

  def verifyGetReportDeadlines(incomeSourceId: String, nino: String): Unit =
    WiremockHelper.verifyGet(reportDeadlinesUrl(incomeSourceId, nino))

  //PayApi Stubs
  def stubPayApiResponse(url: String, status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubPost(url, status, response.toString())
  }

  def verifyStubPayApi(url: String, requestBody: JsValue): Unit = {
    WiremockHelper.verifyPost(url, requestBody.toString())
  }
}
