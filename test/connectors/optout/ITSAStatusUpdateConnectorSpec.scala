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

import connectors.BaseConnectorSpec
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel._
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.BAD_REQUEST
import play.api.libs.json.Json
import play.mvc.Http.Status.NO_CONTENT
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.concurrent.Future

class ITSAStatusUpdateConnectorSpec extends BaseConnectorSpec {

  val taxYear = TaxYear.forYearEnd(2024)
  val taxableEntityId: String = "AB123456A"
  val connector = new ITSAStatusUpdateConnector(mockHttpClientV2, appConfig)

  "ITSAStatusUpdateConnectorSpec" when {

    ".makeITSAStatusUpdate()" when {

      "happy case" should {

        "return successful response" in {

          val apiResponse = ITSAStatusUpdateResponseSuccess()
          val httpResponse = HttpResponse(NO_CONTENT, Json.toJson(apiResponse), Map())

          when(mockHttpClientV2.put(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future.successful(httpResponse))

          val result: Future[ITSAStatusUpdateResponse] = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason)

          result.futureValue shouldBe ITSAStatusUpdateResponseSuccess()
        }
      }

      "unhappy case" should {

        "return failure response" in {

          val errorItems = List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
            "Submission has not passed validation. Invalid parameter taxableEntityId."))

          val apiFailResponse = ITSAStatusUpdateResponseFailure(errorItems)
          val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(apiFailResponse), Map())

          when(mockHttpClientV2.put(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
            .thenReturn(Future.successful(httpResponse))

          val result: Future[ITSAStatusUpdateResponse] = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason)

          result.futureValue shouldBe ITSAStatusUpdateResponseFailure(errorItems)

        }

        "when missing header" should {

          "return failure response" in {

            val errorItems = List(ErrorItem(
              code = "INVALID_TAXABLE_ENTITY_ID",
              reason = "Submission has not passed validation. Invalid parameter taxableEntityId."
            ))

            val apiFailResponse = ITSAStatusUpdateResponseFailure(errorItems)
            val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(apiFailResponse), Map.empty)

            when(mockHttpClientV2.put(any())(any())).thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.withBody(any())(any(), any(), any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
              .thenReturn(Future.successful(httpResponse))

            val result: Future[ITSAStatusUpdateResponse] = connector.makeITSAStatusUpdate(taxYear, taxableEntityId, optOutUpdateReason)

            result.futureValue shouldBe ITSAStatusUpdateResponseFailure(errorItems)

          }
        }
      }
    }
  }
}