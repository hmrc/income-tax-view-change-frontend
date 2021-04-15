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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.WiremockHelper
import models.core.{Nino, NinoResponseError}
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import models.reportDeadlines.ObligationsModel
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}

object IncomeTaxViewChangeStub {

  // NINO Lookup Stubs
  // =================
  val ninoLookupUrl: String => String = mtditid => s"/income-tax-view-change/nino-lookup/$mtditid"

  def stubGetNinoResponse(mtditid: String, nino: Nino): Unit =
    WiremockHelper.stubGet(ninoLookupUrl(mtditid), Status.OK, Json.toJson(nino).toString)

  def stubGetNinoError(mtditid: String, error: NinoResponseError): Unit =
    WiremockHelper.stubGet(ninoLookupUrl(mtditid), Status.INTERNAL_SERVER_ERROR, Json.toJson(error).toString)

  def verifyGetNino(mtditid: String): Unit =
    WiremockHelper.verifyGet(ninoLookupUrl(mtditid))


  // Get Business Details Stubs
  // ==========================
  def getBusinessDetailsUrl(nino: String): String = s"/income-tax-view-change/get-business-details/nino/$nino"

  def stubGetBusinessDetails(nino: String)(status: Int, response: JsValue): Unit = {
    WiremockHelper.stubGet(getBusinessDetailsUrl(nino), status, response.toString())
  }

  def verifyGetBusinessDetails(nino: String): Unit = {
    WiremockHelper.verifyGet(getBusinessDetailsUrl(nino))
  }

  // Income Source Details Stubs
  // ===========================
  val incomeSourceDetailsUrl: String => String = mtditid => s"/income-tax-view-change/income-sources/$mtditid"

  def stubGetIncomeSourceDetailsResponse(mtditid: String)(status: Int, response: IncomeSourceDetailsResponse): Unit =
    WiremockHelper.stubGet(incomeSourceDetailsUrl(mtditid), status, response.toJson.toString)

  def verifyGetIncomeSourceDetails(mtditid: String): Unit =
    WiremockHelper.verifyGet(incomeSourceDetailsUrl(mtditid))


  //PreviousObligations Stubs
  def previousObligationsUrl(nino: String): String = {
    s"/income-tax-view-change/$nino/fulfilled-report-deadlines"
  }

  def stubGetPreviousObligations(nino: String, deadlines: ObligationsModel): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(nino), Status.OK, Json.toJson(deadlines).toString())

  def previousObligationsUrl(nino: String, fromDate: LocalDate, toDate: LocalDate): String = {
    s"/income-tax-view-change/$nino/fulfilled-report-deadlines/from/$fromDate/to/$toDate"
  }

  def stubGetPreviousObligations(nino: String, fromDate: LocalDate, toDate: LocalDate, deadlines: ObligationsModel): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(nino, fromDate, toDate), Status.OK, Json.toJson(deadlines).toString())

  def stubGetPreviousObligationsNotFound(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(nino, fromDate, toDate), Status.NOT_FOUND, Json.obj().toString())

  def stubGetPreviousObligationsError(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(nino, fromDate, toDate), Status.INTERNAL_SERVER_ERROR, "")


  def stubGetPreviousObligationsNotFound(nino: String): Unit =
    WiremockHelper.stubGet(previousObligationsUrl(nino), Status.NOT_FOUND, "")

  def verifyGetPreviousObligations(nino: String): Unit =
    WiremockHelper.verifyGet(previousObligationsUrl(nino))


  //ReportDeadlines Stubs
  //=====================
  def reportDeadlinesUrl(nino: String): String = s"/income-tax-view-change/$nino/report-deadlines"

  def stubGetReportDeadlines(nino: String, deadlines: ObligationsModel): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(nino), Status.OK, Json.toJson(deadlines).toString())

  def stubGetReportDeadlinesError(nino: String): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  def stubGetReportDeadlinesNotFound(nino: String): Unit =
    WiremockHelper.stubGet(reportDeadlinesUrl(nino), Status.NO_CONTENT, "")

  def verifyGetReportDeadlines(nino: String): Unit =
    WiremockHelper.verifyGet(reportDeadlinesUrl(nino))

  //PayApi Stubs
  def stubPayApiResponse(url: String, status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubPost(url, status, response.toString())
  }

  def verifyStubPayApi(url: String, requestBody: JsValue): Unit = {
    WiremockHelper.verifyPost(url, Some(requestBody.toString()))
  }

  //FinancialDetails Stubs
  def financialDetailsUrl(nino: String, from: String, to: String): String = s"/income-tax-view-change/$nino/financial-details/charges/from/$from/to/$to"

  def stubGetFinancialDetailsResponse(nino: String, from: String = "2017-04-06", to: String = "2018-04-05")
                                     (status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(financialDetailsUrl(nino, from, to), status, response.toString())
  }

  def verifyGetFinancialDetails(nino: String, from: String = "2017-04-06", to: String = "2018-04-05"): Unit = {
    WiremockHelper.verifyGet(financialDetailsUrl(nino, from, to))
  }

  //Payments Stubs
  def paymentsUrl(nino: String, from: String, to: String): String = s"/income-tax-view-change/$nino/financial-details/payments/from/$from/to/$to"

  def stubGetPaymentsResponse(nino: String, from: String, to: String)
                             (status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(paymentsUrl(nino, from, to), status, response.toString())
  }

  def verifyGetPayments(nino: String, from: String, to: String): Unit = {
    WiremockHelper.verifyGet(paymentsUrl(nino, from, to))
  }

  //Outstanding charges Stubs
  def getOutstandingChargesUrl(idType: String, idNumber: Long, taxYear: String): String = {
    s"/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear"
  }
  def stubGetOutstandingChargesResponse(idType: String, idNumber: Long, taxYear: String)
                             (status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(getOutstandingChargesUrl(idType, idNumber, s"${taxYear.toInt}-04-05"), status, response.toString())
  }

  def verifyGetOutstandingChargesResponse(idType: String, idNumber: Long, taxYear: String): Unit = {
    WiremockHelper.verifyGet(getOutstandingChargesUrl(idType, idNumber, s"${taxYear.toInt}-04-05"))
  }
}
