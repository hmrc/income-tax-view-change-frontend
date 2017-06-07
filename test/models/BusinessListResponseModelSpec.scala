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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import org.scalatest.Matchers
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class BusinessListResponseModelSpec extends UnitSpec with Matchers {

  val localDate: String => LocalDate = date => LocalDate.parse(date, DateTimeFormatter.ofPattern("uuuu-M-d"))

  "The BusinessListModel" should {

    val jsonString =
      """
        {
          "business":[
            {
              "id":"1234",
              "accountingPeriod":{"start":"now","end":"later"},
              "accountingType":"CASH",
              "commencementDate":"today",
              "cessationDate":"tomorrow",
              "tradingName":"business",
              "businessDescription":"a business",
              "businessAddressLineOne":"64 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            },
            {
              "id":"5678",
              "accountingPeriod":{"start":"later","end":"evenLater"},
              "accountingType":"CASH",
              "commencementDate":"today",
              "cessationDate":"tomorrow",
              "tradingName":"otherBusiness",
              "businessDescription":"some business",
              "businessAddressLineOne":"65 Zoo Lane",
              "businessAddressLineTwo":"Happy Place",
              "businessAddressLineThree":"Magical Land",
              "businessAddressLineFour":"England",
              "businessPostcode":"ZL1 064"
            }
          ]
        }
      """.stripMargin.split("\\s{2,}").mkString

    val business1 = BusinessModel(
      id = "1234",
      accountingPeriod = AccountingPeriod(start = "now", end = "later"),
      accountingType = "CASH",
      commencementDate = Some("today"),
      cessationDate = Some("tomorrow"),
      tradingName = "business",
      businessDescription = Some("a business"),
      businessAddressLineOne = Some("64 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )
    val business2 = BusinessModel(
      id = "5678",
      accountingPeriod = AccountingPeriod(start = "later", end = "evenLater"),
      accountingType = "CASH",
      commencementDate = Some("today"),
      cessationDate = Some("tomorrow"),
      tradingName = "otherBusiness",
      businessDescription = Some("some business"),
      businessAddressLineOne = Some("65 Zoo Lane"),
      businessAddressLineTwo = Some("Happy Place"),
      businessAddressLineThree = Some("Magical Land"),
      businessAddressLineFour = Some("England"),
      businessPostcode = Some("ZL1 064")
    )

    val businesses = BusinessListModel(List(business1, business2))

    "for the 1st Business" should {

      "have the id set as 1234" in {
        businesses.business.head.id shouldBe "1234"
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