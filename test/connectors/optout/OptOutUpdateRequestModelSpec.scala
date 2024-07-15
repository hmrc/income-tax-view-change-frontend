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

import connectors.optout.OptOutUpdateRequestModel.{OptOutUpdateRequest, OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess, optOutUpdateReason}
import models.incomeSourceDetails.TaxYear
import org.eclipse.jetty.http.HttpStatus.{INTERNAL_SERVER_ERROR_500, NO_CONTENT_204}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, JsValue, Json}
import testUtils.UnitSpec

class OptOutUpdateRequestModelSpec extends UnitSpec with Matchers {

  val endYear = 2024

  "The request model" should {

    val requestObject = OptOutUpdateRequest(TaxYear.forYearEnd(endYear).toString, optOutUpdateReason)
    val requestJson = Json.parse(
      """
        {
          "taxYear": "2023-2024",
          "updateReason": "10"
        }
        """.stripMargin)

    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateRequest](requestObject) shouldBe requestJson
    }
    "be able to parse a JSON input as a string into the model" in {
      requestJson.validate[OptOutUpdateRequest] shouldBe JsSuccess(requestObject)
    }
  }

  "The success model" should {
    val successObject = OptOutUpdateResponseSuccess("123")
    val successJson = Json.parse(
      """
        {
          "correlationId": "123",
          "statusCode": 204
        }
        """.stripMargin)

    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateResponseSuccess](successObject) shouldBe successJson
    }
    "be able to parse a JSON input as a string into the model" in {
      successJson.validate[OptOutUpdateResponseSuccess] shouldBe JsSuccess(successObject)
    }
  }

  "The failure model" should {

    val failureObject = OptOutUpdateResponseFailure.defaultFailure().copy(correlationId = "123")
    val failureJson = Json.parse(
      """
        {
          "correlationId": "123",
          "statusCode": 500,
          "failures": [{
          "code": "INTERNAL_SERVER_ERROR",
          "reason": "Request failed due to unknown error"
          }]
        }
        """.stripMargin)

    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateResponseFailure](failureObject) shouldBe failureJson
    }
    "be able to parse a JSON input as a string into the model" in {
      failureJson.validate[OptOutUpdateResponseFailure] shouldBe JsSuccess(failureObject)
    }
  }

  "The not-found failure model" should {

    val notFoundFailureObject = OptOutUpdateResponseFailure.notFoundFailure("123", "some url")
    val notFoundFailureJson = Json.parse(
      """
        {
          "correlationId": "123",
          "statusCode": 500,
          "failures": [{
          "code": "INTERNAL_SERVER_ERROR",
          "reason": "URI not found on target backed-end service, url: some url"
          }]
        }
        """.stripMargin)

    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateResponseFailure](notFoundFailureObject) shouldBe notFoundFailureJson
    }
    "be able to parse a JSON input as a string into the model" in {
      notFoundFailureJson.validate[OptOutUpdateResponseFailure] shouldBe JsSuccess(notFoundFailureObject)
    }
  }

}
