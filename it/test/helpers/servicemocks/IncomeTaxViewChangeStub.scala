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
import models.core.{Nino, NinoResponseError, NinoResponseSuccess}
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceResponse}
import models.financialDetails.Payment
import models.incomeSourceDetails.IncomeSourceDetailsResponse
import models.obligations.ObligationsModel
import models.repaymentHistory.RepaymentHistoryModel
import play.api.http.Status
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.libs.json.{JsValue, Json}

import java.time.LocalDate

object IncomeTaxViewChangeStub { // scalastyle:off number.of.methods

  // NINO Lookup Stubs
  // =================
  val ninoLookupUrl: String => String = mtditid => s"/income-tax-view-change/nino-lookup/$mtditid"

  def stubGetNinoResponse(mtditid: String, nino: NinoResponseSuccess): Unit =
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

  def stubGetIncomeSourceDetailsErrorResponse(mtditid: String)(status: Int): Unit =
    WiremockHelper.stubGet(incomeSourceDetailsUrl(mtditid), status, "")

  def verifyGetIncomeSourceDetails(mtditid: String, noOfCalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(incomeSourceDetailsUrl(mtditid), noOfCalls)
  }

  def stubPostFeedback(status: Int): Unit = {
    WiremockHelper.stubPost("http://localhost:9250/contact/beta-feedback/submit?service=ITVC", status, "")
  }

  // Stub CreateBusinessDetails
  def stubCreateBusinessDetailsResponse(mtditid: String)(status: Int, response: List[CreateIncomeSourceResponse]): Unit =
    WiremockHelper.stubPost(s"/income-tax-view-change/create-income-source/business/$mtditid", status, Json.toJson(response).toString)

  def stubCreateBusinessDetailsErrorResponse(mtditid: String): Unit =
    WiremockHelper.stubPost(s"/income-tax-view-change/create-income-source/business/$mtditid", INTERNAL_SERVER_ERROR, "")

  def stubCreateBusinessDetailsErrorResponseNew(mtditid: String)(response: List[CreateIncomeSourceErrorResponse]): Unit =
    WiremockHelper.stubPost(s"/income-tax-view-change/create-income-source/business/$mtditid", INTERNAL_SERVER_ERROR, Json.toJson(response).toString)

  //PreviousObligations Stubs
  def fulfilledObligationsUrl(nino: String): String = {
    s"/income-tax-view-change/$nino/fulfilled-report-deadlines"
  }

  def stubGetFulfilledObligations(nino: String, deadlines: ObligationsModel): Unit =
    WiremockHelper.stubGet(fulfilledObligationsUrl(nino), Status.OK, Json.toJson(deadlines).toString())

  def allObligationsUrl(nino: String, fromDate: LocalDate, toDate: LocalDate): String = {
    s"/income-tax-view-change/$nino/obligations/from/$fromDate/to/$toDate"
  }

  def obligationsUrl(nino: String): String = {
    s"/income-tax-view-change/$nino/open-obligations"
  }

  def stubGetAllObligations(nino: String, fromDate: LocalDate, toDate: LocalDate, deadlines: ObligationsModel): Unit =
    WiremockHelper.stubGet(allObligationsUrl(nino, fromDate, toDate), Status.OK, Json.toJson(deadlines).toString())

  def stubGetAllObligationsNotFound(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.stubGet(allObligationsUrl(nino, fromDate, toDate), Status.NOT_FOUND, Json.obj().toString())

  def stubGetAllObligationsError(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.stubGet(allObligationsUrl(nino, fromDate, toDate), Status.INTERNAL_SERVER_ERROR, "")


  def stubGetFulfilledObligationsNotFound(nino: String): Unit =
    WiremockHelper.stubGet(fulfilledObligationsUrl(nino), Status.NOT_FOUND, "")

  def verifyGetAllObligations(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.verifyGet(allObligationsUrl(nino, fromDate, toDate))

  def verifyGetObligations(nino: String): Unit =
    WiremockHelper.verifyGet(obligationsUrl(nino))

  //NextUpdates Stubs
  //=====================
  def nextUpdatesUrl(nino: String): String = s"/income-tax-view-change/$nino/open-obligations"

  def stubGetNextUpdates(nino: String, deadlines: ObligationsModel): Unit =
    WiremockHelper.stubGet(nextUpdatesUrl(nino), Status.OK, Json.toJson(deadlines).toString())

  def stubGetNextUpdatesError(nino: String): Unit =
    WiremockHelper.stubGet(nextUpdatesUrl(nino), Status.INTERNAL_SERVER_ERROR, "ISE")

  def stubGetNextUpdatesNotFound(nino: String): Unit =
    WiremockHelper.stubGet(nextUpdatesUrl(nino), Status.NO_CONTENT, "")

  def verifyGetNextUpdates(nino: String): Unit =
    WiremockHelper.verifyGet(nextUpdatesUrl(nino))

  //PayApi Stubs
  def stubPayApiResponse(url: String, status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubPost(url, status, response.toString())
  }

  def verifyStubPayApi(url: String, requestBody: JsValue): Unit = {
    WiremockHelper.verifyPost(url, Some(requestBody.toString()))
  }

  //FinancialDetails Stubs
  def financialDetailsUrl(nino: String, from: String, to: String): String = s"/income-tax-view-change/$nino/financial-details/charges/from/$from/to/$to"
  def financialDetailsCreditsUrl(nino: String, from: String, to: String): String = s"/income-tax-view-change/$nino/financial-details/credits/from/$from/to/$to"

  def getFinancialsByDocumentIdUrl(nino: String, documentNumber: String) = s"/income-tax-view-change/$nino/financial-details/charges/documentId/$documentNumber"

  def stubGetFinancialDetailsByDateRange(nino: String, from: String = "2017-04-06", to: String = "2018-04-05")
                                        (status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(financialDetailsUrl(nino, from, to), status, response.toString())
  }

  def stubGetFinancialDetailsCreditsByDateRange(nino: String, from: String = "2017-04-06", to: String = "2018-04-05")
                                        (status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(financialDetailsCreditsUrl(nino, from, to), status, response.toString())
  }

  def verifyGetFinancialDetailsByDateRange(nino: String, from: String = "2017-04-06", to: String = "2018-04-05", noOffcalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(financialDetailsUrl(nino, from, to), noOffcalls)
  }

  def verifyGetFinancialDetailsCreditsByDateRange(nino: String, from: String = "2017-04-06", to: String = "2018-04-05", noOffcalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(financialDetailsCreditsUrl(nino, from, to), noOffcalls)
  }

  def stubGetFinancialsByDocumentId(nino: String, docNumber: String)(status: Int, response: JsValue): Unit =
    WiremockHelper.stubGet(getFinancialsByDocumentIdUrl(nino, docNumber), status, response.toString())

  //Payments Stubs
  def paymentsUrl(nino: String, from: String, to: String): String = s"/income-tax-view-change/$nino/financial-details/payments/from/$from/to/$to"

  def stubGetPaymentsResponse(nino: String, from: String, to: String)
                             (status: Int, response: Seq[Payment]): StubMapping = {
    WiremockHelper.stubGet(paymentsUrl(nino, from, to), status, Json.toJson(response).toString())
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

  //Charge History stubs
  def getChargeHistoryUrl(nino: String, chargeReference: String): String = {
    s"/income-tax-view-change/charge-history/$nino/chargeReference/$chargeReference"
  }

  def stubChargeHistoryResponse(nino: String, chargeReference: String)(status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(getChargeHistoryUrl(nino, chargeReference), status, response.toString())
  }

  //Payment Allocation Charges stubs
  def paymentAllocationChargesUrl(nino: String, paymentLot: String, paymentLotItem: String) = s"/income-tax-view-change/$nino/payment-allocations/$paymentLot/$paymentLotItem"

  def stubGetPaymentAllocationResponse(nino: String, paymentLot: String, paymentLotItem: String)(status: Int, response: JsValue): Unit =
    WiremockHelper.stubGet(paymentAllocationChargesUrl(nino, paymentLot, paymentLotItem), status, response.toString())

  // Repayment History By RepaymentId stubs
  def getRepaymentHistoryByIdUrl(nino: String, repaymentId: String): String = {
    s"/income-tax-view-change/repayments/$nino/repaymentId/$repaymentId"
  }

  def stubGetRepaymentHistoryByRepaymentId(nino: Nino, repaymentId: String)
                                          (status: Int, response: RepaymentHistoryModel): StubMapping = {
    WiremockHelper.stubGet(getRepaymentHistoryByIdUrl(nino.value, repaymentId), status, Json.toJson(response).toString())
  }

  def stubUpdateIncomeSource(status: Int, response: JsValue): StubMapping =
    WiremockHelper.stubPut("/income-tax-view-change/update-income-source", status, response.toString())

  def stubUpdateIncomeSourceError(): StubMapping = {
    stubUpdateIncomeSource(INTERNAL_SERVER_ERROR, Json.obj("failures" -> Json.arr(
      Json.obj("code" -> "500", "reason" -> "ETMP is broken :(")
    )))
  }

  def verifyUpdateIncomeSource(body: Option[String]): Unit = {
    WiremockHelper.verifyPut("/income-tax-view-change/update-income-source", body)
  }

  def stubPostClaimToAdjustPoa(status: Int, response: String): Unit = {
    WiremockHelper.stubPost("/income-tax-view-change/submit-claim-to-adjust-poa", status, response)
  }

  //payment-plan
  val selfServeTimeToPayJourneyStartUrl: String = "/essttp-backend/sa/itsa/journey/start"
  def stubPostStartSelfServeTimeToPayJourney()(status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubPost(selfServeTimeToPayJourneyStartUrl, status, response.toString())
  }
}
