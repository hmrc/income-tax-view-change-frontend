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

package models.penalties

import models.penalties.GetPenaltyDetailsParser.{GetPenaltyDetailsFailureResponse, GetPenaltyDetailsMalformed}
import play.api.http.Status
import play.api.http.Status.{IM_A_TEAPOT, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsValue, Json}
import testConstants.PenaltiesTestConstants.getPenaltyDetails
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

class GetPenaltyDetailsSpec extends TestSupport {

  val getPenaltyDetailsJson: JsValue = Json.parse("""{
                                           |  "totalisations" : {
                                           |    "LSPTotalValue" : 200,
                                           |    "penalisedPrincipalTotal" : 2000,
                                           |    "LPPPostedTotal" : 165.25,
                                           |    "LPPEstimatedTotal" : 15.26
                                           |  },
                                           |  "lateSubmissionPenalty" : {
                                           |    "summary" : {
                                           |      "activePenaltyPoints" : 2,
                                           |      "inactivePenaltyPoints" : 0,
                                           |      "PoCAchievementDate" : "2020-05-07",
                                           |      "regimeThreshold" : 2,
                                           |      "penaltyChargeAmount" : 145.33
                                           |    },
                                           |    "details" : [ {
                                           |      "penaltyNumber" : "12345678901234",
                                           |      "penaltyOrder" : "01",
                                           |      "penaltyCategory" : "P",
                                           |      "penaltyStatus" : "ACTIVE",
                                           |      "FAPIndicator" : "X",
                                           |      "penaltyCreationDate" : "2022-10-30",
                                           |      "triggeringProcess" : "P123",
                                           |      "penaltyExpiryDate" : "2022-10-30",
                                           |      "expiryReason" : "EXP",
                                           |      "communicationsDate" : "2022-10-30",
                                           |      "lateSubmissions" : [ {
                                           |        "lateSubmissionID" : "001",
                                           |        "taxPeriod" : "23AA",
                                           |        "taxReturnStatus" : "Fulfilled",
                                           |        "taxPeriodStartDate" : "2022-01-01",
                                           |        "taxPeriodEndDate" : "2022-12-31",
                                           |        "taxPeriodDueDate" : "2023-02-01",
                                           |        "returnReceiptDate" : "2023-02-01"
                                           |      } ],
                                           |      "appealInformation" : [ {
                                           |        "appealStatus" : "99",
                                           |        "appealLevel" : "01",
                                           |        "appealDescription" : "Late"
                                           |      } ],
                                           |      "chargeReference" : "CHARGEREF1",
                                           |      "chargeAmount" : 200,
                                           |      "chargeOutstandingAmount" : 200,
                                           |      "chargeDueDate" : "2022-10-30"
                                           |    } ]
                                           |  },
                                           |  "latePaymentPenalty" : {
                                           |    "details" : [ {
                                           |      "principalChargeReference" : "12345678901234",
                                           |      "penaltyCategory" : "LPP1",
                                           |      "penaltyStatus" : "P",
                                           |      "penaltyAmountAccruing" : 99.99,
                                           |      "penaltyAmountPosted" : 1001.45,
                                           |      "penaltyAmountPaid" : 1001.45,
                                           |      "penaltyAmountOutstanding" : 99.99,
                                           |      "LPP1LRCalculationAmount" : 99.99,
                                           |      "LPP1LRDays" : "15",
                                           |      "LPP1LRPercentage" : 2.21,
                                           |      "LPP1HRCalculationAmount" : 99.99,
                                           |      "LPP1HRDays" : "31",
                                           |      "LPP1HRPercentage" : 2.21,
                                           |      "LPP2Days" : "31",
                                           |      "LPP2Percentage" : 4.59,
                                           |      "penaltyChargeCreationDate" : "2069-10-30",
                                           |      "communicationsDate" : "2069-10-30",
                                           |      "penaltyChargeReference" : "1234567890",
                                           |      "penaltyChargeDueDate" : "2069-10-30",
                                           |      "appealInformation" : [ {
                                           |        "appealStatus" : "99",
                                           |        "appealLevel" : "01",
                                           |        "appealDescription" : "Late"
                                           |      } ],
                                           |      "principalChargeDocNumber" : "123456789012",
                                           |      "principalChargeMainTransaction" : "4700",
                                           |      "principalChargeSubTransaction" : "1174",
                                           |      "principalChargeBillingFrom" : "2069-10-30",
                                           |      "principalChargeBillingTo" : "2069-10-30",
                                           |      "principalChargeDueDate" : "2069-10-30",
                                           |      "timeToPay" : [ {
                                           |        "TTPStartDate" : "2023-10-12",
                                           |        "TTPEndDate" : "2024-10-12"
                                           |      } ]
                                           |    } ],
                                           |    "manualLPPIndicator" : false
                                           |  },
                                           |  "breathingSpace" : {
                                           |    "BSStartDate" : "2020-04-06",
                                           |    "BSEndDate" : "2020-12-30"
                                           |  }
                                           |}
                                           |""".stripMargin)

  def httpResponse(details: GetPenaltyDetails): HttpResponse = HttpResponse.apply(status = Status.OK, json = Json.toJson(details), headers = Map.empty)

  val mockGetPenaltyDetailsModelv3: GetPenaltyDetails = GetPenaltyDetails(
    totalisations = None,
    lateSubmissionPenalty = None,
    latePaymentPenalty = None,
    breathingSpace = None
  )

  val mockOKHttpResponseWithInvalidBody: HttpResponse =
    HttpResponse.apply(status = Status.OK, json = Json.parse(
      """
        |{
        | "lateSubmissionPenalty": {
        |   "summary": {
        |     "activePenaltyPoints": 1
        |     }
        |   }
        | }
        |""".stripMargin
    ), headers = Map.empty)

  val mockBadRequestHttpResponse: HttpResponse = HttpResponse.apply(status = Status.BAD_REQUEST, body = "Bad Request.")
  val mockNotFoundHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "Not Found.")
  val mockNotFoundHttpResponseNoBody: HttpResponse = HttpResponse.apply(status = Status.NOT_FOUND, body = "")
  val mockNoContentHttpResponse: HttpResponse = HttpResponse.apply(status = Status.NO_CONTENT, body = "")
  val mockConflictHttpResponse: HttpResponse = HttpResponse.apply(status = Status.CONFLICT, body = "Conflict.")
  val mockUnprocessableEntityHttpResponse: HttpResponse = HttpResponse.apply(status = Status.UNPROCESSABLE_ENTITY, body = "Unprocessable Entity.")
  val mockISEHttpResponse: HttpResponse = HttpResponse.apply(status = Status.INTERNAL_SERVER_ERROR, body = "Something went wrong.")
  val mockServiceUnavailableHttpResponse: HttpResponse = HttpResponse.apply(status = Status.SERVICE_UNAVAILABLE, body = "Service Unavailable.")
  val mockImATeapotHttpResponse: HttpResponse = HttpResponse.apply(status = Status.IM_A_TEAPOT, body = "I'm a teapot.")

  "GetPenaltyDetails" should {

    "write to JSON" in {
      val result = Json.toJson(getPenaltyDetails)(GetPenaltyDetails.format)
      result shouldBe getPenaltyDetailsJson
    }

    "read from JSON" in {
      val result = getPenaltyDetailsJson.as[GetPenaltyDetails](GetPenaltyDetails.format)
      result shouldBe getPenaltyDetails
    }

    s"parse an OK (${Status.OK}) response" when {
      s"the body of the response is valid" in {
        val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", httpResponse(mockGetPenaltyDetailsModelv3))
        result.isRight shouldBe true
        result.toOption.get.asInstanceOf[GetPenaltyDetailsParser.GetPenaltyDetailsSuccessResponse].penaltyDetails shouldBe mockGetPenaltyDetailsModelv3
      }

      s"the body is malformed - returning a $Left $GetPenaltyDetailsMalformed" in {
        val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockOKHttpResponseWithInvalidBody)
        result.isLeft shouldBe true
      }
    }

    s"parse a BAD REQUEST (${Status.BAD_REQUEST}) response" in {
      val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockBadRequestHttpResponse)
      result.isLeft shouldBe true
      result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.BAD_REQUEST
    }
  }

  s"parse a NOT FOUND (${Status.NOT_FOUND}) response with no body" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNoContentHttpResponse)
    result.isRight shouldBe true
    result.toOption.get.penaltyDetails shouldBe mockGetPenaltyDetailsModelv3
  }

  s"parse a NO CONTENT (${Status.NO_CONTENT}) response" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockNoContentHttpResponse)
    result.isRight shouldBe true
    result.toOption.get.penaltyDetails shouldBe mockGetPenaltyDetailsModelv3
  }

  s"parse a Conflict (${Status.CONFLICT}) response - logging PagerDuty" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockConflictHttpResponse)
    result.isLeft shouldBe true
    result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.CONFLICT
  }

  s"parse an UNPROCESSABLE ENTITY (${Status.UNPROCESSABLE_ENTITY}) response" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockUnprocessableEntityHttpResponse)
    result.isLeft shouldBe true
    result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.UNPROCESSABLE_ENTITY
  }

  s"parse an INTERNAL SERVER ERROR (${Status.INTERNAL_SERVER_ERROR}) response" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockISEHttpResponse)
    result.isLeft shouldBe true
    result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.INTERNAL_SERVER_ERROR
  }

  s"parse a SERVICE UNAVAILABLE (${Status.SERVICE_UNAVAILABLE}) response" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockServiceUnavailableHttpResponse)
    result.isLeft shouldBe true
    result.left.getOrElse(GetPenaltyDetailsFailureResponse(IM_A_TEAPOT)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.SERVICE_UNAVAILABLE
  }

  s"parse an unknown error (e.g. IM A TEAPOT - ${Status.IM_A_TEAPOT}) - and log a PagerDuty" in {
    val result = GetPenaltyDetailsParser.GetPenaltyDetailsReads.read("GET", "/", mockImATeapotHttpResponse)
    result.isLeft shouldBe true
    result.left.getOrElse(GetPenaltyDetailsFailureResponse(INTERNAL_SERVER_ERROR)).asInstanceOf[GetPenaltyDetailsFailureResponse].status shouldBe Status.IM_A_TEAPOT
  }
}
