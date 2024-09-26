/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import connectors.constants.AddressLookupConnectorConstants._
import models.admin.IncomeSources
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupResponse, PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import uk.gov.hmrc.http.HttpReads

import scala.concurrent.Future

class AddressLookupConnectorSpec extends BaseConnectorSpec {

  val connector = new AddressLookupConnector(appConfig, mockHttpClientV2, messagesApi)

  "AddressLookupConnector" should {

    "addressLookupInitializeUrl" should {

      "return the initialising address" in {

        disableAllSwitches()
        enable(IncomeSources)

        val result = connector.addressLookupInitializeUrl
        result shouldBe s"$baseUrl/api/v2/init"
      }
    }

    "getAddressDetailsUrl" should {

      "return the get url" in {

        disableAllSwitches()
        enable(IncomeSources)

        val result = connector.getAddressDetailsUrl("123")
        result shouldBe s"$baseUrl/api/v2/confirmed?id=123"
      }
    }

    "initialiseAddressLookup" should {

      "return the redirect location" when {

        "location returned from the lookup-service (individual)" in {

          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          val response: PostAddressLookupResponse =
            Right(PostAddressLookupSuccessResponse(Some("Sample location")))

          when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[PostAddressLookupResponse]], any()))
            .thenReturn(Future.successful(response))

          val result = connector.initialiseAddressLookup(isAgent = false, isChange = false)

          whenReady(result) { res =>
            res shouldBe Right(PostAddressLookupSuccessResponse(Some("Sample location")))
          }
        }

        "location returned from lookup-service (agent)" in {

          //this is the only specific agent test, just to test that everything works with both possible json payloads

          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          val response: PostAddressLookupResponse =
            Right(PostAddressLookupSuccessResponse(Some("Sample location")))

          when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[PostAddressLookupResponse]], any()))
            .thenReturn(Future.successful(response))

          val result = connector.initialiseAddressLookup(isAgent = true, isChange = false)

          whenReady(result) { res =>
            res shouldBe Right(PostAddressLookupSuccessResponse(Some("Sample location")))
          }
        }
      }

      "return the redirect location when on the change page" when {

        "location returned from the lookup-service (individual) and isChange = true" in {

          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          val response: PostAddressLookupResponse =
            Right(PostAddressLookupSuccessResponse(Some("Sample location")))

          when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[PostAddressLookupResponse]], any()))
            .thenReturn(Future.successful(response))

          val result = connector.initialiseAddressLookup(isAgent = false, isChange = true)

          whenReady(result) { res =>
            res shouldBe Right(PostAddressLookupSuccessResponse(Some("Sample location")))
          }
        }

        "location returned from lookup-service (agent) isAgent = true and when isChange = true" in {

          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()

          val response: PostAddressLookupResponse =
            Right(PostAddressLookupSuccessResponse(Some("Sample location")))

          when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[PostAddressLookupResponse]], any()))
            .thenReturn(Future.successful(response))

          val result = connector.initialiseAddressLookup(isAgent = true, isChange = true)

          whenReady(result) { res =>
            res shouldBe Right(PostAddressLookupSuccessResponse(Some("Sample location")))
          }
        }
      }

      "return an error" when {
        "non-standard status returned from lookup-service" in {
          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()
          val response: PostAddressLookupResponse =
            Left(UnexpectedPostStatusFailure(Status.BAD_REQUEST))

          when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[PostAddressLookupResponse]], any()))
            .thenReturn(Future.successful(response))

          val result = connector.initialiseAddressLookup(isAgent = false, isChange = false)

          whenReady(result) { res =>
            res shouldBe Left(UnexpectedPostStatusFailure(Status.BAD_REQUEST))
          }
        }

        "non-standard status returned from lookup-service on change page" in {
          disableAllSwitches()
          enable(IncomeSources)
          beforeEach()
          val response: PostAddressLookupResponse =
            Left(UnexpectedPostStatusFailure(Status.BAD_REQUEST))

          when(mockHttpClientV2.post(any())(any())).thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.withBody(any())(any(), any(), any()))
            .thenReturn(mockRequestBuilder)

          when(mockRequestBuilder.execute(any[HttpReads[PostAddressLookupResponse]], any()))
            .thenReturn(Future.successful(response))

          val result = connector.initialiseAddressLookup(isAgent = false, isChange = true)

          whenReady(result) { res =>
            res shouldBe Left(UnexpectedPostStatusFailure(Status.BAD_REQUEST))
          }
        }
      }
    }
  }
}