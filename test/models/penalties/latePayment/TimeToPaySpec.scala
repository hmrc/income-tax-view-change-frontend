/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties.latePayment

import play.api.libs.json.{JsObject, Json}
import testUtils.TestSupport

import java.time.LocalDate

class TimeToPaySpec extends TestSupport {

  val timeToPay: TimeToPay = TimeToPay(
    TTPStartDate = Some(LocalDate.of(2024, 1, 10)),
    TTPEndDate = Some(LocalDate.of(2024, 3, 10))
  )

  val timeToPayJson: JsObject = Json.obj(
    "TTPStartDate" -> Some("2024-01-10"),
    "TTPEndDate" -> Some("2024-03-10")
  )

  "TimeToPay" should {
    "successfully write to JSON" in {
      val result = Json.toJson(timeToPay)(TimeToPay.format)
      result shouldBe timeToPayJson
    }

    "successfully read from JSON" in {
      val result = timeToPayJson.as[TimeToPay](TimeToPay.format)
      result shouldBe timeToPay
    }
  }

}
