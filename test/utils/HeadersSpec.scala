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

package utils

import testUtils.TestSupport
import utils.Headers.checkAndAddTestHeader

class HeadersSpec extends TestSupport {

  val ukSelectReportingMethod: String = "uk-property-reporting-method"
  val incomeSourceCreatedJourney: String = "afterIncomeSourceCreated"
  val testHeader: String = "Gov-Test-Scenario"

  "Return updated Gov-Test-Scenario headers" when {
    "the action scenario matches the govUKTestHeaderValuesMap" in {
        val updatedHeaders = checkAndAddTestHeader(ukSelectReportingMethod, headerCarrier, appConfig.incomeSourceOverrides())
        val hc = headerCarrier.withExtraHeaders(testHeader -> incomeSourceCreatedJourney)
        updatedHeaders shouldBe hc
    }
  }

  "Return empty Gov-Test-Scenario headers" when {
    "the action scenario does not matches the govUKTestHeaderValuesMap" in {
      val updatedHeaders = checkAndAddTestHeader("otherAction", headerCarrier, appConfig.incomeSourceOverrides())
      val hc = headerCarrier.withExtraHeaders(testHeader -> "")
      updatedHeaders shouldBe hc
    }
  }

}
