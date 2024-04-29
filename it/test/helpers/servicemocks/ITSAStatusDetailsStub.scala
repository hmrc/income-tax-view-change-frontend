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
import models.itsaStatus.ITSAStatus
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsArray, Json}
import testConstants.BaseIntegrationTestConstants.testNino


object ITSAStatusDetailsStub extends ComponentSpecBase {
  def getUrl(taxYearRange: String = "23-24", futureYears: Boolean = false): String =
    s"/income-tax-view-change/itsa-status/status/$testNino/$taxYearRange?futureYears=$futureYears&history=false"

  def stubGetITSAStatusDetails(status: String, taxYearRange: String = "2024-25"): StubMapping = {
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

  def stubGetITSAStatusFutureYearsDetails(taxYear: Int): StubMapping = {
    val previousYear = taxYear - 1
    val futureYear = taxYear + 1

    def shortTaxYear(taxYear: Int) = taxYear.toString.takeRight(2).toInt

    val taxYearToStatus: Map[String, String] = Map(
      s"${futureYear - 1}-${shortTaxYear(futureYear)}" -> ITSAStatus.Mandated.toString,
      s"${taxYear - 1}-${shortTaxYear(taxYear)}" -> ITSAStatus.Mandated.toString,
      s"${previousYear - 1}-${shortTaxYear(previousYear)}" -> ITSAStatus.Mandated.toString
    )
    WiremockHelper.stubGet(getUrl(s"${shortTaxYear(previousYear) - 1}-${shortTaxYear(previousYear)}", futureYears = true), OK,

      taxYearToStatus.foldLeft(JsArray()) {
        case (array, (taxYear, status)) =>
          val itsaStatusObject = Json.parse(
            s"""  {
               |    "taxYear": "$taxYear",
               |    "itsaStatusDetails": [
               |      {
               |        "submittedOn": "2023-06-01T10:19:00.303Z",
               |        "status": "$status",
               |        "statusReason": "Sign up - return available",
               |        "businessIncomePriorTo2Years": 99999999999.99
               |      }
               |    ]
               |  }""".stripMargin)
          array.append(itsaStatusObject)
      }.toString()
    )
  }

  def stubGetITSAStatusDetailsError(taxYear: String = "23-24", futureYears: Boolean = false): StubMapping = {
    WiremockHelper.stubGet(getUrl(taxYear, futureYears), INTERNAL_SERVER_ERROR, "IF is currently experiencing problems that require live service intervention.")
  }


}
