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

package mocks.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{RequestHeader, Result}
import services.SessionService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockSessionService extends UnitSpec with BeforeAndAfterEach {

  val mockSessionService: SessionService = mock(classOf[SessionService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionService)
  }

  def setupMockGetSession(response: Option[String]): Unit =
    when(
      mockSessionService.get(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(response)))

  def setupMockSetSession(key: String, value: String, result: Result)(implicit header: RequestHeader): Unit =
    when(
      mockSessionService.set(ArgumentMatchers.eq(key), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(result.addingToSession(key -> value))))


  def mockIncomeSourcesRemovedFromSessionFailure(): Unit = {
    when(
      mockSessionService.remove(any(), any())(any(), any()))
      .thenReturn(Future.successful(Left(new Exception))
      )
  }

  def setupMockSetListSession(result: Result, keyValue: Map[String, String])(implicit header: RequestHeader): Unit =
    when(
      mockSessionService.setList(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(result.addingToSession(keyValue.toSeq: _*)))
      )

  def setupMockSetSession(result: Result, keyValue: (String, String)*)(implicit header: RequestHeader): Unit =
    when(
      mockSessionService.set(ArgumentMatchers.eq(result), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(Right(result.addingToSession(keyValue: _*))))

  def setupMockCreateSession(result: Boolean): Unit =
    when(
      mockSessionService.createSession(ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))

  def setupMockGetSessionKeyMongo(result: Either[Throwable, Option[String]]): Unit = {
    when(
      mockSessionService.getMongoKey(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetSessionKeyMongo(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMongoKey(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockDeleteSession(result: Boolean): Unit = {
    when(
      mockSessionService.deleteSession(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }
}
