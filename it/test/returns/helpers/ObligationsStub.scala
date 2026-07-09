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
import common.models.obligations.ObligationsModel
import play.api.http.Status
import play.api.libs.json.Json

import java.time.LocalDate

object ObligationsStub { // scalastyle:off number.of.methods
  
  def allObligationsUrl(nino: String, fromDate: LocalDate, toDate: LocalDate): String = {
    s"/income-tax-obligations/$nino/obligations/from/$fromDate/to/$toDate"
  }

  def stubGetAllObligations(nino: String, fromDate: LocalDate, toDate: LocalDate, deadlines: ObligationsModel): StubMapping =
    WiremockHelper.stubGet(allObligationsUrl(nino, fromDate, toDate), Status.OK, Json.toJson(deadlines).toString())

  def stubGetAllObligationsError(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.stubGet(allObligationsUrl(nino, fromDate, toDate), Status.INTERNAL_SERVER_ERROR, "")

  def verifyGetAllObligations(nino: String, fromDate: LocalDate, toDate: LocalDate): Unit =
    WiremockHelper.verifyGet(allObligationsUrl(nino, fromDate, toDate))

}
