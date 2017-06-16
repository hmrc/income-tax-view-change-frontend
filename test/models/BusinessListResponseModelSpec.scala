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

package models

import assets.TestConstants.BusinessDetails._
import org.scalatest.Matchers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec
import assets.TestConstants._

class BusinessListResponseModelSpec extends UnitSpec with Matchers {

  "The BusinessListModel" should {

    "for the 1st Business" should {

      s"have the id set as $testSelfEmploymentId" in {
        businesses.business.head.id shouldBe testSelfEmploymentId
      }
    }

    "for the 2nd Business" should {

      "have the id set as 5678" in {
        businesses.business.last.id shouldBe "5678"
      }
    }

    "be formatted to JSON correctly" in {
      Json.toJson(businesses).toString() shouldBe jsonString
    }

    "be able to parse a JSON to string into the Model" in {
      Json.parse(jsonString).as[BusinessListModel] shouldBe businesses
    }
  }

  "The BusinessListError" should {

    val code = Status.INTERNAL_SERVER_ERROR
    val message = "InternalServerError"
    val jsonString =
      s"""
        {
          "code": $code,
          "message": "$message"
        }
        """.stripMargin.split("\\s+").mkString

    val errorModel = BusinessListError(code, message)

    "have the correct status code in the model" in {
      errorModel.code shouldBe 500
    }

    "have the correct Error Message in the model" in {
      errorModel.message shouldBe message
    }

    "be formatted to JSON correctly" in {
      Json.toJson(errorModel).toString() shouldBe jsonString
    }

    "be able to parse a JSON to string into the Model" in {
      Json.parse(jsonString).as[BusinessListError] shouldBe errorModel
    }
  }
}