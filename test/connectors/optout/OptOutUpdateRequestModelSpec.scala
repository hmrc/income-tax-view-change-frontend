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
import connectors.optout.OptOutUpdateRequestModelSpec._
import models.incomeSourceDetails.TaxYear
import org.eclipse.jetty.http.HttpStatus.{INTERNAL_SERVER_ERROR_500, NO_CONTENT_204}
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsValue, Json}
import testUtils.UnitSpec

object OptOutUpdateRequestModelSpec {

  val endYear = 2024

  private val requestObj = OptOutUpdateRequest(TaxYear.forYearEnd(endYear).toString, optOutUpdateReason)
  private val requestJson: JsValue = Json.obj(
    "taxYear" -> "2023-2024",
    "updateReason" -> "10",
  )

  private val successObj = OptOutUpdateResponseSuccess("123")
  private val successJson: JsValue = Json.obj(
    "correlationId" -> "123",
    "statusCode" -> NO_CONTENT_204,
  )

  private val failureObj = OptOutUpdateResponseFailure.defaultFailure().copy(correlationId = "123")
  private val failureJson: JsValue = Json.obj(
    "correlationId" -> "123",
    "statusCode" -> INTERNAL_SERVER_ERROR_500,
    "failures" -> Json.arr(
      Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "reason" -> "Request failed due to unknown error",
      )
    )
  )

  private val notFoundFailureObj = OptOutUpdateResponseFailure.notFoundFailure("123", "some url")
  private val notFoundFailureJson: JsValue = Json.obj(
    "correlationId" -> "123",
    "statusCode" -> INTERNAL_SERVER_ERROR_500,
    "failures" -> Json.arr(
      Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "reason" -> "URI not found on target backed-end service, url: some url",
      )
    )
  )
}

class OptOutUpdateRequestModelSpec extends UnitSpec with Matchers {

  "The request model" should {
    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateRequest](requestObj) shouldBe requestJson
    }
    "be able to parse a JSON input as a string into the model" in {
      Json.parse(requestJson.toString).as[OptOutUpdateRequest] shouldBe requestObj
    }
  }

  "The success model" should {
    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateResponseSuccess](successObj) shouldBe successJson
    }
    "be able to parse a JSON input as a string into the model" in {
      Json.parse(successJson.toString).as[OptOutUpdateResponseSuccess] shouldBe successObj
    }
  }

  "The failure model" should {
    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateResponseFailure](failureObj) shouldBe failureJson
    }
    "be able to parse a JSON input as a string into the model" in {
      Json.parse(failureJson.toString).as[OptOutUpdateResponseFailure] shouldBe failureObj
    }
  }

  "The not-found failure model" should {
    "be formatted to JSON correctly" in {
      Json.toJson[OptOutUpdateResponseFailure](notFoundFailureObj) shouldBe notFoundFailureJson
    }
    "be able to parse a JSON input as a string into the model" in {
      Json.parse(notFoundFailureJson.toString).as[OptOutUpdateResponseFailure] shouldBe notFoundFailureObj
    }
  }

}
