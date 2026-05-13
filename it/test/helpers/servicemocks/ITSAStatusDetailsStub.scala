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
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.ITSAStatus
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.{JsArray, Json}
import testConstants.BaseIntegrationTestConstants.testNino


object ITSAStatusDetailsStub extends ComponentSpecBase {

  def getUrl(taxYearRange: String = "23-24", futureYears: Boolean = false, nino: String): String =
    s"/income-tax-obligations/itsa-status/status/$nino/$taxYearRange?futureYears=$futureYears&history=false"

  def stubGetITSAStatusDetails(status: String, taxYearRange: String = "2024-25"): StubMapping = {
    WiremockHelper.stubGet(getUrl(taxYearRange.takeRight(5), nino = testNino), OK,
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

  def stubNotFoundForGetITSAStatusDetails(taxYearRange: String = "2024-25"): StubMapping = {
    WiremockHelper.stubGet(getUrl(taxYearRange.takeRight(5), nino = testNino), NOT_FOUND,
      s"""{
         |"code": "NOT_FOUND",
         |"reason":"The remote endpoint has indicated that the requested resource could not be found."
         |}
         |""".stripMargin
    )
  }

  def stubGetITSAStatusFutureYearsDetails(
                                           taxYear: TaxYear,
                                           `itsaStatusCY-1`: ITSAStatus = ITSAStatus.Mandated,
                                           itsaStatusCY: ITSAStatus = ITSAStatus.Mandated,
                                           `itsaStatusCY+1`: ITSAStatus = ITSAStatus.Mandated,
                                           nino: String = testNino
                                         ): StubMapping = {
    val previousYear = taxYear.previousYear
    val futureYear = taxYear.nextYear

    val taxYearToStatus: Map[String, String] =
      Map(
        s"${futureYear.shortenTaxYearEnd}" -> `itsaStatusCY+1`.toString,
        s"${taxYear.shortenTaxYearEnd}" -> itsaStatusCY.toString,
        s"${previousYear.shortenTaxYearEnd}" -> `itsaStatusCY-1`.toString
      )

    WiremockHelper.stubGet(
      url = getUrl(s"${previousYear.`taxYearYY-YY`}", futureYears = true, nino),
      status = OK,
      body = taxYearToStatus.foldLeft(JsArray()) {
        case (array, (taxYear, status)) =>
          val itsaStatusObject =
            Json.parse(
              s"""{
                 |  "taxYear": "$taxYear",
                 |  "itsaStatusDetails": [
                 |    {
                 |      "submittedOn": "2023-06-01T10:19:00.303Z",
                 |      "status": "$status",
                 |      "statusReason": "Sign up - return available",
                 |      "businessIncomePriorTo2Years": 99999999999.99
                 |    }
                 |  ]
                 |}""".stripMargin)
          array.append(itsaStatusObject)
      }.toString()
    )
  }

  case class ITSAYearStatus(previousYear: ITSAStatus.ITSAStatus, currentYear: ITSAStatus.ITSAStatus, nextYear: ITSAStatus.ITSAStatus)

  def stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(taxYear: Int, yearStatus: ITSAYearStatus): StubMapping = {
    val previousYear = taxYear - 1
    val futureYear = taxYear + 1

    def shortTaxYear(taxYear: Int) = taxYear.toString.takeRight(2).toInt

    val taxYearToStatus: Map[String, String] = Map(
      s"${futureYear - 1}-${shortTaxYear(futureYear)}" -> yearStatus.nextYear.toString,
      s"${taxYear - 1}-${shortTaxYear(taxYear)}" -> yearStatus.currentYear.toString,
      s"${previousYear - 1}-${shortTaxYear(previousYear)}" -> yearStatus.previousYear.toString
    )

    WiremockHelper.stubGet(getUrl(s"${shortTaxYear(previousYear) - 1}-${shortTaxYear(previousYear)}", futureYears = true, testNino), OK,

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
    WiremockHelper.stubGet(getUrl(taxYear, futureYears, testNino), INTERNAL_SERVER_ERROR, "IF is currently experiencing problems that require live service intervention.")
  }


}
