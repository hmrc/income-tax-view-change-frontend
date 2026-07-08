/*
 * Copyright 2024 HM Revenue & Customs
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

package common.helpers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.models.incomeSourceDetails.IncomeSourceDetailsResponse
import play.api.libs.json.JsValue
object GetInsourceDetailsStub {

  // Income Source Details Stubs
  // ===========================
  val incomeSourceDetailsUrl: String => String = mtditid => s"/income-tax-business-details/income-sources/$mtditid"

  def stubGetIncomeSourceDetailsResponse(mtditid: String)(status: Int, response: IncomeSourceDetailsResponse): StubMapping =
    WiremockHelper.stubGet(incomeSourceDetailsUrl(mtditid), status, response.toJson.toString)

  def stubGetIncomeSourceDetailsErrorResponse(mtditid: String)(status: Int): Unit =
    WiremockHelper.stubGet(incomeSourceDetailsUrl(mtditid), status, "")

  def verifyGetIncomeSourceDetails(mtditid: String, noOfCalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(incomeSourceDetailsUrl(mtditid), noOfCalls)
  }

  def stubUpdateIncomeSource(status: Int, response: JsValue): StubMapping =
    WiremockHelper.stubPut("/income-tax-business-details/update-income-source", status, response.toString())

}
