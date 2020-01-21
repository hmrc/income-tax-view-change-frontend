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
import models.calculation.{Calculation, ListCalculationItems}
import play.api.http.Status
import play.api.libs.json.{JsObject, Json}

object IndividualCalculationStub {

  def getCalculationListUrl(nino: String, taxYear: String): String = s"/$nino/self-assessment?taxYear=$taxYear"

  def getCalculationUrl(nino: String, calculationId: String): String = s"/$nino/self-assessment/$calculationId"

  def stubGetCalculationList(nino: String, taxYear: String)(status: Int, body: ListCalculationItems): Unit = {
    WiremockHelper.stubGet(getCalculationListUrl(nino, taxYear), status, Json.toJson(body).toString())
  }

  def verifyGetCalculationList(nino: String, taxYear: String): Unit = {
    WiremockHelper.verifyGet(getCalculationListUrl(nino, taxYear))
  }

  def stubGetCalculation(nino: String, calculationId: String)(status: Int, body: JsObject): Unit = {
    WiremockHelper.stubGet(getCalculationUrl(nino, calculationId), status, body.toString())
  }

  def verifyGetCalculation(nino: String, calculationId: String): Unit = {
    WiremockHelper.verifyGet(getCalculationUrl(nino, calculationId))
  }

  def stubGetCalculationNotFound(nino: String, calculationId: String): Unit = {
    WiremockHelper.stubGet(getCalculationUrl(nino, calculationId), Status.NOT_FOUND, "")
  }

  def stubGetCalculationError(nino: String, calculationId: String): Unit = {
    WiremockHelper.stubGet(getCalculationUrl(nino, calculationId), Status.INTERNAL_SERVER_ERROR, "")
  }
}
