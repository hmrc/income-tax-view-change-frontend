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

import helpers.{IntegrationTestConstants, WiremockHelper}
import models.{BusinessDetailsModel, LastTaxCalculation}
import play.api.http.Status
import play.api.i18n.I18nSupport

object IncomeTaxViewChangeStub {

  val url: (String,String) => String = (nino, year) =>
    s"/income-tax-view-change/estimated-tax-liability/$nino/$year/it"

  def stubGetLastTaxCalc(nino: String, year: String, lastCalc: LastTaxCalculation): Unit = {
    val financialDataResponse =
      IntegrationTestConstants
        .GetLastCalculation.successResponse(
        lastCalc.calcID,
        lastCalc.calcTimestamp,
        lastCalc.calcAmount)
        .toString()
    WiremockHelper.stubGet(url(nino, year), Status.OK, financialDataResponse)
  }

  def verifyGetLastTaxCalc(nino: String, year: String): Unit =
    WiremockHelper.verifyGet(url(nino, year))
}
