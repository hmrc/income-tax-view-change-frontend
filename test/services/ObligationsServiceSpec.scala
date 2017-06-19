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

import assets.TestConstants
import mocks.{MockObligationDataConnector, MockBusinessDetailsConnector, MockPropertyDataConnector}
import models._
import play.api.i18n.Messages
import play.api.libs.json.{JsResultException, Json}
import play.mvc.Http.Status
import uk.gov.hmrc.play.http.{HeaderCarrier, InternalServerException}
import utils.TestSupport
import assets.TestConstants._
import assets.TestConstants.BusinessDetails._

class ObligationsServiceSpec extends TestSupport with MockObligationDataConnector with MockBusinessDetailsConnector with MockPropertyDataConnector {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  object TestObligationsService extends ObligationsService(mockObligationDataConnector, mockBusinessDetailsConnector, mockPropertyDataConnector)

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
        setupMockBusinesslistResult(testNino)(businesses)
        setupMockObligation(testNino, testSelfEmploymentId)(TestConstants.Obligations.obligationsDataResponse)

        val successfulObligationsResponse =
          ObligationsModel(
            List(
                ObligationModel(
                  start = localDate("2017-04-06"),
                  end = localDate("2017-07-05"),
                  due = localDate("2017-08-05"),
                  met = true
              )
            )
          )

        await(TestObligationsService.getObligations(testNino)) shouldBe successfulObligationsResponse
      }
    }

    "no business list is found" should {

      val businessListErrorResponse = BusinessListError(Status.BAD_REQUEST, "Error Message")

      "return an obligations error model" in {
        setupMockBusinesslistResult(testNino)(businessListErrorResponse)
        await(TestObligationsService.getObligations(testNino)) shouldBe ObligationsErrorModel(Status.BAD_REQUEST, "Error Message")
      }
    }
  }

  "The ObligationsService.getPropertyObligations method" when {

    "a single list of obligations is returned from the connector" should {

      "return a valid list of obligations" in {

        setupMockPropertyObligation(testNino)(TestConstants.Obligations.obligationsDataResponse)

        val successfulObligationsResponse =
          ObligationsModel(
            List(
              ObligationModel(
                start = localDate("2017-04-06"),
                end = localDate("2017-07-05"),
                due = localDate("2017-08-05"),
                met = true
              )
            )
          )
        await(TestObligationsService.getPropertyObligations(testNino)) shouldBe successfulObligationsResponse
      }
    }
  }
}
