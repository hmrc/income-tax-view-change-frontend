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

package connectors.constants

import models.incomeSourceDetails.TaxYear

object ITSAStatusUpdateConnectorConstants {

  val optOutUpdateReason = "10"
  val signUpUpdateReason = "11"
  val taxYear2024: TaxYear = TaxYear.forYearEnd(2024)
  val taxYear2023 = TaxYear.forYearEnd(2023)
  val taxableEntityId = "AB123456A"

  val correctOptOutRequestBody =
    """{
      | "taxYear":"2023-24",
      | "updateReason":"10"
      |}""".stripMargin

  val correctsignUptRequestBody =
    """{
      | "taxYear":"2023-24",
      | "updateReason":"11"
      |}""".stripMargin

  val invalidPayLoadFailureResponseBody =
    """{
      |  "failures": [
      |    {
      |      "code": "INVALID_PAYLOAD",
      |      "reason": "Submission has not passed validation. Invalid payload."
      |    }
      |  ]
      |}""".stripMargin

}
