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

package mocks

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.matches
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.play.partials.HtmlPartial

import scala.concurrent.{ExecutionContext, Future}


trait MockHttp extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockHttpGet: HttpClient = mock[HttpClient]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockHttpGet)
  }

  def setupMockHttpPost(url: String, body: JsValue)(response: HttpResponse): OngoingStubbing[Future[HttpResponse]] =
    when(mockHttpGet.POST[JsValue, HttpResponse](matches(url), ArgumentMatchers.eq(body), ArgumentMatchers.any())
    (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))

  def setupMockHttpGet(url: String)(response: HttpResponse): OngoingStubbing[Future[HttpResponse]] =
    when(mockHttpGet.GET[HttpResponse](matches(url), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))

  def setupAgentMockHttpGet(url: Option[String] = None)(status: Int, response: JsValue): Unit = {
    lazy val urlMatcher = url.fold(ArgumentMatchers.any[String]())(x => matches(x))
    when(mockHttpGet.GET[HttpResponse](urlMatcher)
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any[ExecutionContext])).thenReturn(Future.successful(HttpResponse(
      status = status, json = response, headers = Map.empty)))
  }

  def setupMockHttpGetWithParams(url: String, params: Seq[(String, String)])(response: HttpResponse): OngoingStubbing[Future[HttpResponse]] =
    when(mockHttpGet.GET[HttpResponse](matches(url),ArgumentMatchers.eq(params), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))

  def setupMockFailedHttpGet(url: String): OngoingStubbing[Future[HttpResponse]] =
    when(mockHttpGet.GET[HttpResponse](matches(url), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new Exception("unknown error")))

  def setupMockFailedHttpGetWithParams(url: String, params: Seq[(String, String)])(response: HttpResponse): OngoingStubbing[Future[HttpResponse]] =
    when(mockHttpGet.GET[HttpResponse](matches(url),ArgumentMatchers.eq(params), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.failed(new Exception("unknown error")))

  def setupMockHttpGetPartial(url:String)(response: HtmlPartial): OngoingStubbing[Future[HtmlPartial]] =
    when(mockHttpGet.GET[HtmlPartial](matches(url))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
}
