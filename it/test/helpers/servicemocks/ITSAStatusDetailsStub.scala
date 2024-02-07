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

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import helpers.{ComponentSpecBase, WiremockHelper}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import testConstants.BaseIntegrationTestConstants.testNino


object ITSAStatusDetailsStub extends ComponentSpecBase {
  def getUrl(taxYearRange: String = "23-24"): String =
    s"/income-tax-view-change/itsa-status/status/$testNino/$taxYearRange?futureYears=false&history=false"

  def stubGetITSAStatusDetails(status: String, taxYearRange: String = "2023-24"): StubMapping = {
    WiremockHelper.stubGet(getUrl(taxYearRange.takeRight(5)), OK,
      s"""|[
          |  {
          |    "taxYear": "$taxYearRange",
          |    "itsaStatusDetails": [
          |      {
          |        "submittedOn": "2023-06-01T10:19:00.303Z",
          |        "status": "$status",
          |        "statusReason": "Sign up - return available",
          |        "businessIncomePriorTo2Years": 99999999999.99
          |      }
          |    ]
          |  }
          |]""".stripMargin
    )
  }

  def stubGetITSAStatusDetailsError: StubMapping = {
    WiremockHelper.stubGet(getUrl(), INTERNAL_SERVER_ERROR, "IF is currently experiencing problems that require live service intervention.")
  }


}
