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

package mocks

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString, matches}
import org.mockito.Mockito.{mock, reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

trait MockHttpV2 extends UnitSpec with BeforeAndAfterEach {

  val mockHttpClientV2: HttpClientV2 = mock(classOf[HttpClientV2])
  val mockRequestBuilder: RequestBuilder = mock(classOf[RequestBuilder])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpClientV2)
    reset(mockRequestBuilder)
  }

  def setupMockHttpVTwoGet[T](url: String)(response: T): OngoingStubbing[Future[T]] = {
    when(mockHttpClientV2
      .get(ArgumentMatchers.eq(url"$url"))(ArgumentMatchers.any()))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder
      .execute[T](ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }


  def setupMockFailedHttpVTwoGet[T](url: String): OngoingStubbing[Future[T]] = {
    when(mockHttpClientV2
      .get(ArgumentMatchers.eq(url"$url"))(ArgumentMatchers.any())).thenReturn(mockRequestBuilder)

    when(mockRequestBuilder
      .execute[T](ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.failed(new Exception("unknown error")))
  }
}
