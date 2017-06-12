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

package services

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import models._
import play.api.i18n.Messages
import play.api.libs.json.{JsResultException, Json}
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.TestSupport

class ObligationsServiceSpec extends TestSupport with MockObligationDataConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val localDate: String => LocalDate = date => LocalDate.parse(date, DateTimeFormatter.ofPattern("uuuu-M-d"))

  val nino = "AA123456A"
  val selfEmploymentId = "5318008"
  val businessListResponse = SuccessResponse(Json.parse(
    s"""
       |{"business": [
       | {
       |   "id": "5318008",
       |   "accountingPeriod": {
       |     "start": "2017-04-06",
       |     "end": "2018-04-05"
       |   },
       |   "accountingType": "CASH",
       |   "commencementDate": "2015-01-01",
       |   "cessationDate": "2018-04-05",
       |   "tradingName": "Test Ltd",
       |   "businessDescription": "Testing services",
       |   "businessAddressLineOne": "1 Test Road",
       |   "businessAddressLineTwo": "Test City",
       |   "businessAddressLineThree": "Test County",
       |   "businessAddressLineSFour": "Test Country",
       |   "businessPostcode": "A9 9AA"
       | }
       |]
       |}
        """.stripMargin
  ))

  object TestObligationsService extends ObligationsService(mockObligationDataConnector)

  "The ObligationsService.getObligations method" when {

    //Business Details

    "a successful single business and single list of obligations is returned from the connector" should {

      val obligationsDataResponse = SuccessResponse(Json.parse(
        s"""{
           |  "obligations" : [
           |    {
           |      "start": "2017-04-06",
           |      "end": "2017-07-05",
           |      "due": "2017-08-05",
           |      "met": true
           |    }
           |  ]
           |}""".stripMargin
      ))

      "return a valid list of obligations" in {
        setupMockBusinesslistResult(nino)(businessListResponse)
        setupMockObligation(nino, selfEmploymentId)(obligationsDataResponse)

        val successfulObligationsResponse =
          ObligationsStatusModel(
            List(
              ObligationStatusModel(
                ObligationModel(
                  start = localDate("2017-04-06"),
                  end = localDate("2017-07-05"),
                  due = localDate("2017-08-05"),
                  met = true
                ),
                ObligationStatus.RECEIVED
              )
            )
          )

        await(TestObligationsService.getObligations(nino)) shouldBe successfulObligationsResponse
      }
    }

    "no business list is found" should {

      val businessListErrorResponse = ErrorResponse(Status.BAD_REQUEST, "Error Message")

      "throw an appropriate exception" in {

        setupMockBusinesslistResult(nino)(businessListErrorResponse)

        val thrown = intercept[Exception] {
          await(TestObligationsService.getObligations(nino))
        }

        thrown.isInstanceOf[InternalServerException]
      }
    }

    "a business is found but no self employment ID exists" should {

      val noBusinessIdListResponse = SuccessResponse(Json.parse(
        s"""
           |{"business": [
           | {
           |   "accountingPeriod": {
           |     "start": "2017-04-06",
           |     "end": "2018-04-05"
           |   },
           |   "accountingType": "CASH",
           |   "commencementDate": "2015-01-01",
           |   "cessationDate": "2018-04-05",
           |   "tradingName": "Test Ltd",
           |   "businessDescription": "Testing services",
           |   "businessAddressLineOne": "1 Test Road",
           |   "businessAddressLineTwo": "Test City",
           |   "businessAddressLineThree": "Test County",
           |   "businessAddressLineSFour": "Test Country",
           |   "businessPostcode": "A9 9AA"
           | }
           |]
           |}
        """.stripMargin
      ))

      "not parse the json and throw an appropriate exception" in {

        setupMockBusinesslistResult(nino)(noBusinessIdListResponse)

        val thrown = intercept[Exception] {
          await(TestObligationsService.getObligations(nino))
        }

        thrown.isInstanceOf[JsResultException]
      }
    }

    //Obligations Data
    "no obligations are returned" should {

      val noObligationsErrorResponse = ErrorResponse(Status.BAD_REQUEST, "Error Message")

      "throw an appropriate exception" in {
        setupMockBusinesslistResult(nino)(businessListResponse)
        setupMockObligation(nino, selfEmploymentId)(noObligationsErrorResponse)
        val thrown = intercept[Exception] {
          await(TestObligationsService.getObligations(nino))
        }
        thrown.isInstanceOf[InternalServerException]
      }
    }

    "an invalid Obligations model is returned" should {

      val invalidObligationsResponse = SuccessResponse(Json.parse(
        s"""{
           |  "obligations" : [
           |    {
           |      "invalidKey": "Bad things"
           |    }
           |  ]
           |}""".stripMargin
      ))



      "throw an appropriate Exception" in {
        setupMockBusinesslistResult(nino)(businessListResponse)
        setupMockObligation(nino, selfEmploymentId)(invalidObligationsResponse)

        val thrown = intercept[Exception]{
          await(TestObligationsService.getObligations(nino))
        }
        thrown.isInstanceOf[JsResultException]
      }

    }
  }

  "The addStatus method" when {

    "passed an 'overdue' ObligationsModel" should {

      val obligationOver = ObligationModel(
        start = localDate("2017-01-01"),
        end = localDate("2017-4-30"),
        due = localDate("2017-5-31"),
        met = false
      )
      val obligationsOver = ObligationsModel(List(obligationOver))

      val obligationOpen = ObligationModel(
        start = localDate("2017-6-1"),
        end = localDate("2100-9-30"),
        due = localDate("2100-10-31"),
        met = false
      )
      val obligationsOpen = ObligationsModel(List(obligationOpen))

      val obligationReceived = ObligationModel(
        start = localDate("2017-6-1"),
        end = localDate("2017-9-30"),
        due = localDate("2017-10-31"),
        met = true
      )
      val obligationsReceived = ObligationsModel(List(obligationReceived))

      "return an ObligationStatusModel with an 'overdue' status" in {
        TestObligationsService.addStatus(obligationsOver) shouldBe List(ObligationStatusModel(obligationOver, ObligationStatus.OVERDUE))
      }

      "return an ObligationStatusModel with an 'open' status" in {
        TestObligationsService.addStatus(obligationsOpen) shouldBe List(ObligationStatusModel(obligationOpen, ObligationStatus.OPEN))
      }

      "return an ObligationStatusModel with a 'received' status" in {
        TestObligationsService.addStatus(obligationsReceived) shouldBe List(ObligationStatusModel(obligationReceived, ObligationStatus.RECEIVED))
      }

    }

  }

}
