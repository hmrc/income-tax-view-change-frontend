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
import play.api.libs.json.JsValue

object FinancialDetailsStub {
  //FinancialDetails Stubs
  def financialDetailsUrl(nino: String, from: String, to: String): String = s"/income-tax-financial-details/$nino/financial-details/charges/from/$from/to/$to"

  def stubGetFinancialDetailsByDateRange(nino: String, from: String = "2017-04-06", to: String = "2018-04-05")
                                        (status: Int, response: JsValue): StubMapping = {
    WiremockHelper.stubGet(financialDetailsUrl(nino, from, to), status, response.toString())
  }

  def verifyGetFinancialDetailsByDateRange(nino: String, from: String = "2017-04-06", to: String = "2018-04-05", noOffcalls: Int = 1): Unit = {
    WiremockHelper.verifyGet(financialDetailsUrl(nino, from, to), noOffcalls)
  }
}
