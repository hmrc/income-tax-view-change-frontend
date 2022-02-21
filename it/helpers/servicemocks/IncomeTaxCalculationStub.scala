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
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.libs.json.Json

object IncomeTaxCalculationStub {

  def getCalculationResponseUrl(nino: String, taxYear: String): String = s"/income-tax-calculation/income-tax/nino/$nino/calculation-details?taxYear=$taxYear"
  def getCalculationResponseByCalcIdUrl(nino: String, calcId: String): String = s"/income-tax-calculation/income-tax/nino/$nino/calc-id/$calcId/calculation-details"

  def stubGetCalculationResponse(nino: String, taxYear: String)(status: Int, body: LiabilityCalculationResponse): Unit = {
    WiremockHelper.stubGet(getCalculationResponseUrl(nino, taxYear), status, Json.toJson(body).toString())
  }

  def stubGetCalculationErrorResponse(nino: String, taxYear: String)(status: Int, body: LiabilityCalculationError): Unit = {
    WiremockHelper.stubGet(getCalculationResponseUrl(nino, taxYear), status, Json.toJson(body).toString())
  }
  def verifyGetCalculationResponse(nino: String, taxYear: String, noOfCalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(getCalculationResponseUrl(nino, taxYear), noOfCalls)
  }

  def stubGetCalculationResponseByCalcId(nino: String, calcId: String)(status: Int, body: LiabilityCalculationResponse): Unit = {
    WiremockHelper.stubGet(getCalculationResponseByCalcIdUrl(nino, calcId), status, Json.toJson(body).toString())
  }

  def stubGetCalculationErrorResponseByCalcId(nino: String, calcId: String)(status: Int, body: LiabilityCalculationError): Unit = {
    WiremockHelper.stubGet(getCalculationResponseByCalcIdUrl(nino, calcId), status, Json.toJson(body).toString())
  }
  def verifyGetCalculationResponseByCalcId(nino: String, calcId: String, noOfCalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(getCalculationResponseByCalcIdUrl(nino, calcId), noOfCalls)
  }

}
