/*
 * Copyright 2021 HM Revenue & Customs
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

package assets

import assets.BaseTestConstants.testErrorStatus
import models.chargeHistory._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._

object ChargeHistoryTestConstants {


  val testValidChargeHistoryModelJson: JsValue = Json.parse(
    """
      |   {
      |  "chargeHistoryDetails": [ {
      |  "taxYear": "2019",
      |  "documentId": "123456789",
      |  "documentDate": "2020-01-29",
      |  "documentDescription": "Balancing Charge",
      |  "totalAmount": 12345678912.12,
      |    "reversalDate": "2020-02-24",
      |    "reversalReason": "amended return"
      |     }
      |    ]
      |  }
      |""".stripMargin)


  val testInValidChargeHistoryModelJson: JsValue = Json.parse(
    """
      |   {
      |  "chargeHistoryDetails": [ {
      |  "taxYear": "2019",
      |  "documentId": "123456789",
      |  "documentDate": "2020-01-29",
      |  "documentDescription": "Balancing Charge",
      |  "totalAmount": 12345678912.12,
      |    "reversalDate": "2020-02-24"
      |     }
      |    ]
      |  }
      |""".stripMargin)


  val testValidChargeHistoryModel: ChargesHistoryModel = ChargesHistoryModel(
		idType = "MTDBSA",
		idValue = "XAIT000000000000",
		regimeType = "ITSA",
		chargeHistoryDetails = Some(List(
    ChargeHistoryModel("2017", "123456789", "2020-01-29", "Balancing Charge", 123456789012345.67, "2020-02-24", "amended return"))))

  val testChargeHistoryErrorModelParsing: ChargesHistoryErrorModel = ChargesHistoryErrorModel(
    testErrorStatus, "Json Validation Error. Parsing ChargeHistory Data Response")

  val chargeHistoryErrorNoDataFoundCode = "NO_DATA_FOUND"
  val chargeHistoryErrorNoDataFoundReason = "The remote endpoint has indicated that no match found for the reference provided."

  val testChargeHistoryErrorNoDataFoundJson: JsValue = Json.obj(
    "code" -> chargeHistoryErrorNoDataFoundCode,
    "reason" -> chargeHistoryErrorNoDataFoundReason
  )

  val testChargesHistoryErrorModelNoDataFound: ChargesHistoryErrorModel =
    ChargesHistoryErrorModel(status = NOT_FOUND, message = testChargeHistoryErrorNoDataFoundJson.toString)

  val chargeHistoryErrorInvalidIdtypeCode = "INVALID_IDTYPE"
  val chargeHistoryErrorInvalidIdtypeReason = "Submission has not passed validation. Invalid parameter idType."

  val chargeHistoryErrorInvalidDocNumberCode = "INVALID_DOC_NUMBER"
  val chargeHistoryErrorInvalidDocNumberReason = "Submission has not passed validation. Invalid parameter docNumber."

  val testChargesHistoryFailuresJson: JsValue = Json.obj(
    "failures" -> Seq(
      Json.obj(
        "code" -> chargeHistoryErrorInvalidIdtypeCode,
        "reason" -> chargeHistoryErrorInvalidIdtypeReason
      ),
      Json.obj(
        "code" -> chargeHistoryErrorInvalidDocNumberCode,
        "reason" -> chargeHistoryErrorInvalidDocNumberReason
      )
    )
  )

  val testChargesHistoryErrorModelBadRequestFailures: ChargesHistoryErrorModel =
    ChargesHistoryErrorModel(status = BAD_REQUEST, message = testChargesHistoryFailuresJson.toString)

  val testValidChargeHistoryDetailsModelJson: JsValue = Json.obj(
		"idType" -> "MTDBSA",
		"idValue" -> "XAIT000000000000",
		"regimeType" -> "ITSA",
    "chargeHistoryDetails" -> Json.arr(
      Json.obj(
				"taxYear" -> "2017",
        "documentId" -> "123456789",
        "documentDate" -> "2020-01-29",
        "documentDescription" -> "Balancing Charge",
        "totalAmount" -> 123456789012345.67,
        "reversalDate" -> "2020-02-24",
        "reversalReason" -> "amended return"
      )))

  val testInvalidChargeHistoryDetailsModelJson: JsValue = Json.obj(
    "chargeHistoryDetails" -> Json.arr(
      Json.obj(
        "taxYear" -> "2019",
        "documentId" -> "123456789",
        "documentDate" -> "2020-01-29",
        "documentDescription" -> "Balancing Charge",
        "totalAmount" -> 10.33,
        "reversalDate" -> "2020-02-24"
      )
    )
  )

}
