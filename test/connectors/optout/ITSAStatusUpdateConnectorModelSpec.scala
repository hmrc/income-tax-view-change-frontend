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

package connectors.optout

import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess, optOutUpdateReason}
import models.itsaStatus.ITSAStatusUpdateRequest
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, Json}
import testUtils.UnitSpec

class ITSAStatusUpdateConnectorModelSpec extends UnitSpec with Matchers {

  val endYear = 2024

  "The request model" should {

    val requestObject = ITSAStatusUpdateRequest(TaxYear.forYearEnd(endYear).toString, optOutUpdateReason)
    val requestJson = Json.parse(
      """
        {
          "taxYear": "2023-2024",
          "updateReason": "10"
        }
        """.stripMargin)

    "verify write to json" in {
      Json.toJson[ITSAStatusUpdateRequest](requestObject) shouldBe requestJson
    }
  }

  "The success model" should {
    val successObject = ITSAStatusUpdateResponseSuccess()
    val successJson = Json.parse(
      """
        {
          "correlationId": "123",
          "statusCode": 204
        }
        """.stripMargin)

    "verify read from json" in {
      successJson.validate[ITSAStatusUpdateResponseSuccess] shouldBe JsSuccess(successObject)
    }
  }

  "The failure model" should {

    val failureObject = ITSAStatusUpdateResponseFailure.defaultFailure()
    val failureJson = Json.parse(
      """
        {
          "correlationId": "123",
          "statusCode": 500,
          "failures": [{
          "code": "INTERNAL_SERVER_ERROR",
          "reason": "Request failed due to unknown reason"
          }]
        }
        """.stripMargin)

    "verify read from json" in {
      failureJson.validate[ITSAStatusUpdateResponseFailure] shouldBe JsSuccess(failureObject)
    }
  }

}
