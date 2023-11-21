/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}

object CalculationListStub {

  def legacyUrl(nino: String, taxYearEnd: String): String = s"""/income-tax-view-change/list-of-calculation-results/$nino/$taxYearEnd"""

  def url(nino: String, taxYearRange: String): String = s"""/income-tax-view-change/calculation-list/$nino/$taxYearRange"""


  def stubGetLegacyCalculationList(nino: String, taxYearEnd: String)(jsonResponse: String): Unit = {
    WiremockHelper.stubGet(legacyUrl(nino, taxYearEnd), OK, jsonResponse)
  }

  def stubGetCalculationList(nino: String, taxYearRange: String)(jsonResponse: String): Unit = {
    WiremockHelper.stubGet(url(nino, taxYearRange), OK, jsonResponse)
  }

  def stubGetLegacyCalculationListError(nino: String, taxYear: String): Unit = {
    WiremockHelper.stubGet(legacyUrl(nino, taxYear), INTERNAL_SERVER_ERROR,"DES is currently experiencing problems that require live service intervention.")
  }

  def stubGetCalculationListError(nino: String, taxYearRange: String): Unit = {
    WiremockHelper.stubGet(url(nino, taxYearRange), INTERNAL_SERVER_ERROR, "IF is currently experiencing problems that require live service intervention.")
  }

  def verifyGetLegacyCalculationList(nino: String, taxYear: String): Unit =
    WiremockHelper.verifyGet(legacyUrl(nino, taxYear))

  def verifyGetCalculationList(nino: String, taxYearRange: String): Unit =
    WiremockHelper.verifyGet(url(nino, taxYearRange))
}

