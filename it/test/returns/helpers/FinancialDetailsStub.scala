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

package returns.helpers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.helpers.WiremockHelper
import common.models.core.Nino
import financials.models.Payment
import financials.models.repaymentHistory.RepaymentHistoryModel
import play.api.libs.json.{JsValue, Json}

object FinancialDetailsStub {


  //PayApi Stubs
  def stubPayApiResponse(url: String, status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubPost(url, status, response.toString())
  }

  def verifyStubPayApi(url: String, requestBody: JsValue): Unit = {
    WiremockHelper.verifyPost(url, Some(requestBody.toString()))
  }

   //FinancialDetails Stubs
   def financialDetailsUrl(nino: String, from: String, to: String): String = s"/income-tax-financial-details/$nino/financial-details/charges/from/$from/to/$to"
   def financialDetailsCreditsUrl(nino: String, from: String, to: String): String = s"/income-tax-financial-details/$nino/financial-details/credits/from/$from/to/$to"

   def getFinancialsByDocumentIdUrl(nino: String, documentNumber: String) = s"/income-tax-financial-details/$nino/financial-details/charges/documentId/$documentNumber"

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
   def paymentsUrl(nino: String, from: String, to: String): String = s"/income-tax-financial-details/$nino/financial-details/payments/from/$from/to/$to"

  def stubGetPaymentsResponse(nino: String, from: String, to: String)
                             (status: Int, response: Seq[Payment]): StubMapping = {
    WiremockHelper.stubGet(paymentsUrl(nino, from, to), status, Json.toJson(response).toString())
  }

  def verifyGetPayments(nino: String, from: String, to: String): Unit = {
    WiremockHelper.verifyGet(paymentsUrl(nino, from, to))
  }

   //Outstanding charges Stubs
   def getOutstandingChargesUrl(idType: String, idNumber: Long, taxYear: String): String = {
     s"/income-tax-financial-details/out-standing-charges/$idType/$idNumber/$taxYear"
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
     s"/income-tax-financial-details/charge-history/$nino/chargeReference/$chargeReference"
   }

  def stubChargeHistoryResponse(nino: String, chargeReference: String)(status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(getChargeHistoryUrl(nino, chargeReference), status, response.toString())
  }

   //Payment Allocation Charges stubs
   def paymentAllocationChargesUrl(nino: String, paymentLot: String, paymentLotItem: String) = s"/income-tax-financial-details/$nino/payment-allocations/$paymentLot/$paymentLotItem"

  def stubGetPaymentAllocationResponse(nino: String, paymentLot: String, paymentLotItem: String)(status: Int, response: JsValue): Unit =
    WiremockHelper.stubGet(paymentAllocationChargesUrl(nino, paymentLot, paymentLotItem), status, response.toString())

   // Repayment History By RepaymentId stubs
   def getRepaymentHistoryByIdUrl(nino: String, repaymentId: String): String = {
     s"/income-tax-financial-details/repayments/$nino/repaymentId/$repaymentId"
   }

  def stubGetRepaymentHistoryByRepaymentId(nino: Nino, repaymentId: String)
                                          (status: Int, response: RepaymentHistoryModel): StubMapping = {
    WiremockHelper.stubGet(getRepaymentHistoryByIdUrl(nino.value, repaymentId), status, Json.toJson(response).toString())
  }



   def stubPostClaimToAdjustPoa(status: Int, response: String): Unit = {
     WiremockHelper.stubPost("/income-tax-financial-details/submit-claim-to-adjust-poa", status, response)
   }

  //payment-plan
  val selfServeTimeToPayJourneyStartUrl: String = "/essttp-backend/sa/itsa/journey/start"
  def stubPostStartSelfServeTimeToPayJourney()(status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubPost(selfServeTimeToPayJourneyStartUrl, status, response.toString())
  }

  // Triggered Migration - Update Customer Facts
  def stubUpdateCustomerFacts(mtdId: String)(status: Int): StubMapping =
    WiremockHelper.stubPut(
      s"/income-tax-business-details/customer-facts/update/$mtdId",
      status,
      ""
    )
}
